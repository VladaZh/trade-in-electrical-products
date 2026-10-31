from datetime import date

STORAGE_DAYS = 5
MAX_CLARIFY_ATTEMPTS = 3


def make_data():
    clients = {
        1: {
            "client_id": 1,
            "name": "ООО «ЭлектроМонтаж»",
            "phone": "+7-495-111-22-33",
            "address": "Москва, ул. Складочная, 1",
        },
        2: {
            "client_id": 2,
            "name": "ИП Соколов А.В.",
            "phone": "+7-916-555-44-33",
            "address": "",
        },
    }
    products = {
        101: {
            "product_id": 101,
            "name": "Кабель ВВГнг 3x2.5 (бухта 100 м)",
            "price": 9500.0,
        },
        102: {"product_id": 102, "name": "Автомат C16 1P", "price": 180.0},
        103: {"product_id": 103, "name": "Розетка с заземлением", "price": 95.0},
    }
    stock_lots = [
        {
            "lot_id": 1,
            "product_id": 101,
            "qty": 20,
            "received": date(2026, 9, 1),
            "cell": "A-01",
        },
        {
            "lot_id": 2,
            "product_id": 101,
            "qty": 15,
            "received": date(2026, 10, 5),
            "cell": "A-02",
        },
        {
            "lot_id": 3,
            "product_id": 102,
            "qty": 500,
            "received": date(2026, 9, 20),
            "cell": "B-01",
        },
        {
            "lot_id": 4,
            "product_id": 103,
            "qty": 40,
            "received": date(2026, 10, 1),
            "cell": "C-01",
        },
        {
            "lot_id": 5,
            "product_id": 103,
            "qty": 60,
            "received": date(2026, 8, 15),
            "cell": "C-02",
        },
    ]
    return {
        "clients": clients,
        "products": products,
        "stock_lots": stock_lots,
        "orders": {},
        "order_items": [],
        "shipments": {},
        "deliveries": {},
    }
