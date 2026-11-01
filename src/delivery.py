from datetime import date

from app import add_history, find_label, order_in_status, write_off_order
from console import (
    ask_yes_no,
    run_console_loop,
    show_all_orders,
    show_order,
)
from data import (
    DELIVERY_TASKS,
    ST_ACCEPTED,
    ST_CANCELLED,
    ST_DECLINED,
    ST_DELIVERED,
    ST_DELIVERY_LOADED,
    ST_DELIVERY_NEW,
    ST_DELIVERY_RECEIVED,
    ST_DONE,
    ST_RETURNING,
)
from storage import read_state, run_locked


def receive_delivery_info(data, order, delivery_date):
    delivery_id = max(data["deliveries"]) + 1 if data["deliveries"] else 1
    data["deliveries"][delivery_id] = {
        "delivery_id": delivery_id,
        "order_id": order["order_id"],
        "planned_date": delivery_date,
        "status": "запланирована",
    }
    order["delivery_id"] = delivery_id
    add_history(order, f"отдел доставки получил информацию о доставке №{delivery_id}")
    print(f"Отделом доставки получена информация о доставке №{delivery_id}")
    return data["deliveries"][delivery_id]


def load_goods(order):
    add_history(order, "товар загружен")
    print("Наступила дата доставки; товар загружен")


def deliver_goods(delivery, order, address):
    delivery["status"] = "в пути"
    add_history(order, f"товар доставлен по адресу: {address}")
    print(f"Товар доставлен по адресу: {address}")


def record_delivery(delivery, order):
    delivery["status"] = "доставлена"
    add_history(order, "факт доставки зафиксирован")
    print("Факт доставки зафиксирован")


def cancel_order(order, delivery):
    delivery["status"] = "отклонена клиентом"
    add_history(order, "заказ отменён")
    print("Клиент не принял заказ - заказ отменён")


def return_goods_to_warehouse(order):
    order["reserved"] = False
    add_history(order, "товар возвращён на склад")
    print("Товар возвращён на склад")


def delivery_step_action(data, order_id, status):
    order = order_in_status(data, order_id, status)
    if order is None:
        return
    delivery = data["deliveries"].get(order["delivery_id"])
    client = data["clients"][order["client_id"]]
    if status == ST_DELIVERY_NEW:
        receive_delivery_info(data, order, order["delivery_date"])
        order["status"] = ST_DELIVERY_RECEIVED
    elif status == ST_DELIVERY_RECEIVED:
        load_goods(order)
        order["status"] = ST_DELIVERY_LOADED
    elif status == ST_DELIVERY_LOADED:
        deliver_goods(delivery, order, client["address"])
        order["status"] = ST_DELIVERED
    elif status == ST_ACCEPTED:
        record_delivery(delivery, order)
        write_off_order(data, order)
        order["status"] = ST_DONE
    elif status == ST_DECLINED:
        cancel_order(order, delivery)
        order["status"] = ST_RETURNING
    elif status == ST_RETURNING:
        return_goods_to_warehouse(order)
        order["status"] = ST_CANCELLED


def delivery_tasks(data, ctx):
    tasks = []
    for oid in sorted(data["orders"]):
        label = find_label(DELIVERY_TASKS, data["orders"][oid]["status"])
        if label:
            tasks.append((oid, label))
    return tasks


def delivery_perform(path, order_id):
    data = read_state(path)
    order = data["orders"][order_id]
    show_order(data, order)
    status = order["status"]
    label = find_label(DELIVERY_TASKS, status)
    if status == ST_DELIVERY_RECEIVED:
        print(
            f"\n  Плановая дата доставки: {order['delivery_date']}; сегодня: {date.today()}"
        )
        if not ask_yes_no("  Дата доставки наступила, загружаем товар?"):
            print("  Загрузка отложена до даты доставки.")
            return
    elif not ask_yes_no(f"\n  Выполнить задачу «{label}»?"):
        return
    run_locked(path, delivery_step_action, order_id, status)


def delivery_show_orders(path):
    show_all_orders(read_state(path))


def run_delivery_console(path):
    extras = (("o", "все заказы", delivery_show_orders),)
    return run_console_loop(
        path, "ОТДЕЛ ДОСТАВКИ", None, delivery_tasks, delivery_perform, extras
    )
