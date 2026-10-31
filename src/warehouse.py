from datetime import date

from src.app import notify_client_ready
from src.data import STORAGE_DAYS


def receive_order_info(order):
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
    shipment_id = len(data["shipments"]) + 1
    shipment = {
        "shipment_id": shipment_id,
        "order_id": order["order_id"],
        "lines": picks,
        "date": date(2026, 11, 2),
        "status": "оформлена",
    }
    data["shipments"][shipment_id] = shipment
    print(f"Склад оформил документы на отгрузку №{shipment_id}")
    return shipment


def report_order_ready(order):
    order["status"] = "готов"
    print(f"Заказ №{order['order_id']} готов на складе")


def issue_order(order):
    print("Заказ выдан клиенту со склада")


def create_disassembly_documents(order):
    print("Оформлены документы на расформирование заказа на складе")


def disassemble_order(order):
    order["status"] = "не получен"
    print("Заказ расформирован, товар возвращён на места хранения на складе")


def run_pickup(order, client, client_arrival_day):
    notify_client_ready(client, order)
    day = 1
    while True:
        if storage_period_expired(day, STORAGE_DAYS):
            create_disassembly_documents(order)
            disassemble_order(order)
            return "Заказ не получен"
        if client_arrival_day == day:
            issue_order(order)
            complete_order(order)
            return "Заказ получен"
        day += 1


def storage_period_expired(day, storage_days):
    return day > storage_days


def complete_order(order):
    order["status"] = "выполнен"
    print("Заказ завершён")
