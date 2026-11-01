from datetime import date

from app import (
    add_history,
    available_lots,
    notify_client_ready,
    order_in_status,
    today_iso,
    write_off_order,
)
from console import (
    ask_yes_no,
    run_console_loop,
    show_all_orders,
    show_order,
    show_stock_lots,
)
from data import (
    ST_DELIVERY_DATE,
    ST_DISASSEMBLY_DOCS,
    ST_NOT_RECEIVED,
    ST_PICKUP_READY,
    ST_RECEIVED,
    ST_WH_BATCH,
    ST_WH_DOCS,
    ST_WH_NEW,
    ST_WH_RECEIVED,
    STORAGE_DAYS,
    WAREHOUSE_TASKS,
)
from storage import read_state, run_locked


def receive_order_info(order):
    add_history(order, "склад получил информацию о заказе")
    print(f"Склад получил информацию о заказе №{order['order_id']}")


def form_batch(items, stock_lots):
    picks = []
    complete = True
    for it in items:
        need = it["qty"]
        candidates = [
            lot
            for lot in stock_lots
            if lot["product_id"] == it["product_id"] and lot["qty"] > 0
        ]
        candidates = sorted(candidates, key=lambda lot: lot["received"])
        for lot in candidates:
            if need == 0:
                break
            take = min(need, lot["qty"])
            picks.append(
                {
                    "lot_id": lot["lot_id"],
                    "product_id": it["product_id"],
                    "cell": lot["cell"],
                    "qty": take,
                }
            )
            need -= take
        if need > 0:
            complete = False
    print(f"Сформирована партия: {len(picks)} строк отбора")
    return picks, complete


def create_shipping_documents(data, order, picks):
    shipment_id = max(data["shipments"]) + 1 if data["shipments"] else 1
    data["shipments"][shipment_id] = {
        "shipment_id": shipment_id,
        "order_id": order["order_id"],
        "lines": picks,
        "date": today_iso(),
        "status": "оформлена",
    }
    add_history(order, f"оформлены документы на отгрузку №{shipment_id}")
    print(f"Склад оформил документы на отгрузку №{shipment_id}")


def report_order_ready(order):
    add_history(order, "склад сообщил о готовности заказа")
    print(f"Заказ №{order['order_id']} готов на складе")


def issue_order(order):
    add_history(order, "заказ выдан клиенту")
    print("Заказ выдан клиенту со склада")


def complete_order(order):
    order["status"] = ST_RECEIVED
    add_history(order, "заказ завершён")
    print("Заказ завершён")


def create_disassembly_documents(order):
    order["status"] = ST_DISASSEMBLY_DOCS
    add_history(order, "оформлены документы на расформирование")
    print("Оформлены документы на расформирование заказа на складе")


def disassemble_order(order):
    order["status"] = ST_NOT_RECEIVED
    order["reserved"] = False
    add_history(order, "заказ расформирован, товар возвращён на места хранения")
    print("Заказ расформирован, товар возвращён на места хранения на складе")


def warehouse_step_action(data, order_id, status):
    order = order_in_status(data, order_id, status)
    if order is None:
        return
    if status == ST_WH_NEW:
        receive_order_info(order)
        order["status"] = ST_WH_RECEIVED
    elif status == ST_WH_RECEIVED:
        items = [i for i in data["order_items"] if i["order_id"] == order_id]
        picks, complete = form_batch(items, available_lots(data))
        if not complete:
            print("Не удалось собрать заказ: товар зарезервирован под другие заказы.")
            return
        order["picks"] = picks
        order["reserved"] = True
        for p in picks:
            name = data["products"][p["product_id"]]["name"]
            print(
                f"  партия {p['lot_id']}, ячейка {p['cell']}: {name} - {p['qty']} шт."
            )
        add_history(order, f"партия сформирована ({len(picks)} строк отбора)")
        order["status"] = ST_WH_BATCH
    elif status == ST_WH_BATCH:
        create_shipping_documents(data, order, order["picks"])
        order["status"] = ST_WH_DOCS
    elif status == ST_WH_DOCS:
        report_order_ready(order)
        if order["delivery_type"] == "самовывоз":
            notify_client_ready(data["clients"][order["client_id"]], order)
            order["ready_date"] = today_iso()
            order["status"] = ST_PICKUP_READY
        else:
            add_history(order, "ожидается согласование даты доставки с клиентом")
            order["status"] = ST_DELIVERY_DATE
    elif status == ST_DISASSEMBLY_DOCS:
        disassemble_order(order)


def expire_action(data, order_id):
    order = order_in_status(data, order_id, ST_PICKUP_READY)
    if order is not None:
        create_disassembly_documents(order)


def issue_action(data, order_id):
    order = order_in_status(data, order_id, ST_PICKUP_READY)
    if order is None:
        return
    issue_order(order)
    complete_order(order)
    write_off_order(data, order)


def warehouse_tasks(data, ctx):
    tasks = []
    for oid in sorted(data["orders"]):
        status = data["orders"][oid]["status"]
        for st, label in WAREHOUSE_TASKS:
            if st == status:
                tasks.append((oid, label))
    return tasks


def warehouse_perform(path, order_id):
    data = read_state(path)
    order = data["orders"][order_id]
    show_order(data, order)
    status = order["status"]
    if status == ST_PICKUP_READY:
        days = (date.today() - date.fromisoformat(order["ready_date"])).days
        print(
            f"\n  Заказ готов с {order['ready_date']}, прошло дней: {days}, "
            f"срок хранения: {STORAGE_DAYS} дн."
        )
        if ask_yes_no("  Срок хранения заказа вышел?"):
            run_locked(path, expire_action, order_id)
        elif ask_yes_no("  Клиент пришёл?"):
            run_locked(path, issue_action, order_id)
        else:
            print("  Клиент не пришёл, заказ остаётся на хранении.")
        return
    label = next(lb for st, lb in WAREHOUSE_TASKS if st == status)
    if ask_yes_no(f"\n  Выполнить задачу «{label}»?"):
        run_locked(path, warehouse_step_action, order_id, status)


def warehouse_show_stock(path):
    show_stock_lots(read_state(path))


def warehouse_show_orders(path):
    show_all_orders(read_state(path))


def run_warehouse_console(path):
    extras = (
        ("s", "остатки по партиям", warehouse_show_stock),
        ("o", "все заказы", warehouse_show_orders),
    )
    return run_console_loop(
        path, "СКЛАД", None, warehouse_tasks, warehouse_perform, extras
    )
