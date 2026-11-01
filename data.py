STORAGE_DAYS = 5
MAX_CLARIFY_ATTEMPTS = 3
LOCK_TIMEOUT = 5.0

ST_SALES_CHECK = "на проверке в отделе продаж"
ST_CLARIFY = "ожидает уточнения данных от клиента"
ST_WH_NEW = "передан на склад"
ST_WH_RECEIVED = "склад получил информацию о заказе"
ST_WH_BATCH = "партия сформирована"
ST_PICKUP_READY = "готов к выдаче (самовывоз)"
ST_DISASSEMBLY_DOCS = "оформлены документы на расформирование"
ST_DELIVERY_DATE = "ожидает согласования даты доставки"
ST_DELIVERY_NEW = "передан в отдел доставки"
ST_DELIVERY_RECEIVED = "отдел доставки получил информацию"
ST_DELIVERY_LOADED = "товар загружен"
ST_DELIVERED = "доставлен, ожидает решения клиента"
ST_ACCEPTED = "клиент принял заказ"
ST_DECLINED = "клиент отказался от заказа"
ST_RETURNING = "заказ отменён, товар возвращается на склад"
ST_RECEIVED = "заказ получен"
ST_NOT_RECEIVED = "заказ не получен"
ST_DONE = "заказ выполнен"
ST_CANCELLED = "заказ отменён"

WAREHOUSE_TASKS = (
    (ST_WH_RECEIVED, "Сформировать партию"),
    (ST_WH_BATCH, "Оформить документы на отгрузку"),
    (ST_PICKUP_READY, "Самовывоз: срок хранения, выдача заказа"),
    (ST_DISASSEMBLY_DOCS, "Расформировать заказ"),
)

DELIVERY_TASKS = (
    (ST_DELIVERY_RECEIVED, "Загрузить товар (по наступлении даты доставки)"),
    (ST_DELIVERY_LOADED, "Доставить товар"),
    (ST_ACCEPTED, "Зафиксировать факт доставки"),
    (ST_DECLINED, "Отменить заказ"),
    (ST_RETURNING, "Вернуть товар на склад"),
)


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
            "received": "2026-09-01",
            "cell": "A-01",
        },
        {
            "lot_id": 2,
            "product_id": 101,
            "qty": 15,
            "received": "2026-10-05",
            "cell": "A-02",
        },
        {
            "lot_id": 3,
            "product_id": 102,
            "qty": 500,
            "received": "2026-09-20",
            "cell": "B-01",
        },
        {
            "lot_id": 4,
            "product_id": 103,
            "qty": 40,
            "received": "2026-10-01",
            "cell": "C-01",
        },
        {
            "lot_id": 5,
            "product_id": 103,
            "qty": 60,
            "received": "2026-08-15",
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
