def receive_delivery_info(data, order, delivery_date):
    delivery_id = len(data["deliveries"]) + 1
    delivery = {
        "delivery_id": delivery_id,
        "order_id": order["order_id"],
        "planned_date": delivery_date,
        "status": "запланирована",
    }
    data["deliveries"][delivery_id] = delivery
    print(f"Отделом доставки получена информация о доставке №{delivery_id}")
    return delivery


def load_goods(order):
    print("Наступила дата доставки; товар загружен")


def deliver_goods(delivery, address):
    delivery["status"] = "в пути"
    print(f"Товар доставлен по адресу: {address}")


def record_delivery(delivery, order):
    delivery["status"] = "доставлена"
    print("Факт доставки зафиксирован")


def return_goods_to_warehouse(order):
    print("Товар возвращён на склад")


def cancel_order(order, delivery):
    order["status"] = "отменён"
    delivery["status"] = "отклонена клиентом"
    print("Клиент не принял заказ - заказ отменён")
