from datetime import date, datetime

from data import ST_DELIVERY_NEW, ST_SALES_CHECK, STORAGE_DAYS


def today_iso():
    return date.today().isoformat()


def add_history(order, text):
    order["history"].append(f"{datetime.now():%d.%m %H:%M}  {text}")


def find_label(table, status):
    for st, label in table:
        if st == status:
            return label
    return None


def order_in_status(data, order_id, status):
    order = data["orders"].get(order_id)
    if order is None or order["status"] != status:
        print("Заказ уже обработан другим сотрудником, обновите список.")
        return None
    return order


def available_lots(data):
    reserved = {}
    for order in data["orders"].values():
        if order["reserved"]:
            for p in order["picks"]:
                reserved[p["lot_id"]] = reserved.get(p["lot_id"], 0) + p["qty"]
    return [
        dict(lot, qty=lot["qty"] - reserved.get(lot["lot_id"], 0))
        for lot in data["stock_lots"]
    ]


def check_stock_availability(lines, stock_lots):
    totals = {}
    for lot in stock_lots:
        pid = lot["product_id"]
        totals[pid] = totals.get(pid, 0) + lot["qty"]

    shortage = {}
    max_available = {}
    for pid, qty in lines:
        available = totals.get(pid, 0)
        if qty > available:
            shortage[pid] = qty - available
            max_available[pid] = available
    ok = len(shortage) == 0
    return ok, shortage, max_available


def reject_order(client, shortage):
    print(
        f"Заказ клиента «{client['name']}» отклонён: "
        f"не хватает товара по {len(shortage)} поз."
    )


def show_max_available(max_available, products):
    for pid, qty in max_available.items():
        print(f"Доступно максимум: {products[pid]['name']} - {qty} шт.")


def create_order(data, client_id, lines, delivery_type):
    order_id = max(data["orders"]) + 1 if data["orders"] else 1
    order = {
        "order_id": order_id,
        "client_id": client_id,
        "delivery_type": delivery_type,
        "created": today_iso(),
        "status": "создан",
        "history": [],
        "picks": [],
        "reserved": False,
        "clarification": "",
        "ready_date": None,
        "delivery_date": None,
        "delivery_id": None,
    }
    items = [
        {
            "order_id": order_id,
            "product_id": pid,
            "qty": qty,
            "price": data["products"][pid]["price"],
        }
        for pid, qty in lines
    ]
    data["orders"][order_id] = order
    data["order_items"].extend(items)
    total = sum(i["qty"] * i["price"] for i in items)
    add_history(order, "заказ создан клиентом")
    print(f"Сформирован заказ №{order_id}, сумма {total:,.2f} руб.")
    return order, items


def pass_info_to_sales(order):
    order["status"] = ST_SALES_CHECK
    add_history(order, "информация передана в отдел продаж")
    print(f"Информация по заказу №{order['order_id']} передана в отдел продаж")


def send_message(data, client_id, order_id, text):
    data.setdefault("messages", []).append(
        {
            "client_id": client_id,
            "order_id": order_id,
            "text": text,
            "time": f"{datetime.now():%d.%m %H:%M}",
            "read": False,
        }
    )


def has_unread(data, client_id):
    return any(
        m["client_id"] == client_id and not m["read"] for m in data.get("messages", [])
    )


def pop_messages(data, client_id):
    unread = [
        m
        for m in data.get("messages", [])
        if m["client_id"] == client_id and not m["read"]
    ]
    for m in unread:
        m["read"] = True
    return unread


def notify_client_ready(data, client, order):
    add_history(order, "клиенту отправлено сообщение: заказ готов к выдаче")
    send_message(
        data,
        client["client_id"],
        order["order_id"],
        f"Заказ №{order['order_id']} собран и готов к выдаче на складе. "
        f"Срок хранения - {STORAGE_DAYS} дн. Приходите за заказом.",
    )
    print(
        f"Клиенту «{client['name']}» отправлено сообщение о готовности заказа №{order['order_id']}"
    )


def agree_delivery_date(client, order, delivery_date):
    order["delivery_date"] = delivery_date
    add_history(order, f"с клиентом согласована дата доставки {delivery_date}")
    print(f"С клиентом «{client['name']}» согласована дата доставки: {delivery_date}")


def pass_info_to_delivery(order):
    order["status"] = ST_DELIVERY_NEW
    add_history(order, "информация передана в отдел доставки")
    print("Информация передана в отдел доставки")


def update_stock(stock_lots, picks):
    taken = {}
    for p in picks:
        taken[p["lot_id"]] = taken.get(p["lot_id"], 0) + p["qty"]
    return [
        dict(lot, qty=lot["qty"] - taken.get(lot["lot_id"], 0)) for lot in stock_lots
    ]


def write_off_order(data, order):
    data["stock_lots"] = update_stock(data["stock_lots"], order["picks"])
    order["reserved"] = False
    add_history(order, "складские остатки обновлены")
    print("Складские остатки обновлены")
