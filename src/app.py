from datetime import date

from src.data import STORAGE_DAYS


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
    order_id = len(data["orders"]) + 1
    order = {
        "order_id": order_id,
        "client_id": client_id,
        "delivery_type": delivery_type,
        "created": date(2026, 11, 1),
        "status": "создан",
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
    print(f"Сформирован заказ №{order_id}, сумма {total:,.2f} руб.")
    return order, items


def pass_info_to_sales(order):
    order["status"] = "передан в отдел продаж"
    print(f"Информация по заказу №{order['order_id']} передана в отдел продаж")


def notify_client_ready(client, order):
    print(
        f"Клиент «{client['name']}» уведомлён ({client['phone']}): "
        f"заказ №{order['order_id']} готов, хранение {STORAGE_DAYS} дн."
    )


def agree_delivery_date(client, order, delivery_date):
    print(f"С клиентом «{client['name']}» согласована дата доставки: {delivery_date}")
    return delivery_date


def pass_info_to_delivery(order):
    order["status"] = "передан в доставку"
    print("Информация передана в отдел доставки")


def update_stock(stock_lots, picks):
    taken = {}
    for p in picks:
        taken[p["lot_id"]] = taken.get(p["lot_id"], 0) + p["qty"]
    updated = [
        dict(lot, qty=lot["qty"] - taken.get(lot["lot_id"], 0)) for lot in stock_lots
    ]
    print("Складские остатки обновлены")
    return updated
