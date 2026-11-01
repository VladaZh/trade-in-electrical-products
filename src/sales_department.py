from app import add_history, order_in_status
from console import (
    ask_nonempty,
    ask_yes_no,
    run_console_loop,
    show_all_orders,
    show_order,
)
from data import ST_CLARIFY, ST_SALES_CHECK, ST_WH_NEW
from storage import read_state, run_locked


def check_order_correctness(order, items, clients, products):
    errors = []
    client = clients.get(order["client_id"])
    if client is None:
        return ["клиент не найден"]
    if not client["phone"]:
        errors.append("не указан телефон клиента")
    if order["delivery_type"] == "доставка" and not client["address"]:
        errors.append("не указан адрес доставки")
    for it in items:
        if it["product_id"] not in products:
            errors.append(f"неизвестный товар {it['product_id']}")
        if it["qty"] <= 0:
            errors.append(f"некорректное количество по товару {it['product_id']}")
    return errors


def confirm_data(order):
    add_history(order, "отдел продаж подтвердил корректность данных")
    print("Корректность данных подтверждена")


def send_info_to_warehouse(order):
    order["status"] = ST_WH_NEW
    add_history(order, "информация отправлена на склад")
    print("Информация отправлена на склад отделом продаж")


def clarify_with_client(order, text):
    order["clarification"] = text
    order["status"] = ST_CLARIFY
    add_history(order, f"отдел продаж запросил у клиента: {text}")
    print("Запрос отправлен клиенту, ожидаем его ответа")


def sales_confirm_action(data, order_id):
    order = order_in_status(data, order_id, ST_SALES_CHECK)
    if order is None:
        return
    confirm_data(order)
    send_info_to_warehouse(order)


def sales_clarify_action(data, order_id, text):
    order = order_in_status(data, order_id, ST_SALES_CHECK)
    if order is None:
        return
    clarify_with_client(order, text)


def sales_tasks(data, ctx):
    return [
        (oid, "Проверка корректности поступившего заказа")
        for oid in sorted(data["orders"])
        if data["orders"][oid]["status"] == ST_SALES_CHECK
    ]


def sales_perform(path, order_id):
    data = read_state(path)
    order = data["orders"][order_id]
    show_order(data, order)
    items = [i for i in data["order_items"] if i["order_id"] == order_id]
    hints = check_order_correctness(order, items, data["clients"], data["products"])
    if hints:
        print("  Подсказка системы: " + "; ".join(hints))
    if ask_yes_no("\n  Данные заказа корректны?"):
        run_locked(path, sales_confirm_action, order_id)
    else:
        text = ask_nonempty("  Что нужно уточнить у клиента: ")
        run_locked(path, sales_clarify_action, order_id, text)


def sales_show_orders(path):
    show_all_orders(read_state(path))


def run_sales_console(path):
    extras = (("o", "все заказы", sales_show_orders),)
    return run_console_loop(
        path, "ОТДЕЛ ПРОДАЖ", None, sales_tasks, sales_perform, extras
    )
