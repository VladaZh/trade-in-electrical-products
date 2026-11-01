from datetime import date

from app import (
    add_history,
    agree_delivery_date,
    available_lots,
    check_stock_availability,
    create_order,
    find_label,
    order_in_status,
    pass_info_to_delivery,
    pass_info_to_sales,
    reject_order,
    show_max_available,
)
from console import (
    ask_date,
    ask_from_ids,
    ask_int,
    ask_nonempty,
    ask_text,
    ask_yes_no,
    run_console_loop,
    show_all_orders,
    show_order,
    show_products,
)
from data import (
    ST_ACCEPTED,
    ST_CLARIFY,
    ST_DECLINED,
    ST_DELIVERED,
    ST_DELIVERY_DATE,
    ST_SALES_CHECK,
)
from storage import read_state, run_locked

CLIENT_TASKS = (
    (ST_CLARIFY, "Скорректировать данные (запрос отдела продаж)"),
    (ST_DELIVERY_DATE, "Согласовать дату доставки"),
    (ST_DELIVERED, "Принять или отклонить доставленный заказ"),
)


def add_client_action(data, name, phone, address):
    client_id = max(data["clients"]) + 1 if data["clients"] else 1
    data["clients"][client_id] = {
        "client_id": client_id,
        "name": name,
        "phone": phone,
        "address": address,
    }
    return client_id


def submit_order_action(data, client_id, lines, delivery_type):
    client = data["clients"][client_id]
    ok, shortage, max_avail = check_stock_availability(lines, available_lots(data))
    if not ok:
        reject_order(client, shortage)
        show_max_available(max_avail, data["products"])
        print("Создание заказа отменено.")
        return None
    order, _items = create_order(data, client_id, lines, delivery_type)
    pass_info_to_sales(order)
    return order["order_id"]


def correct_data_action(data, order_id, correction):
    order = order_in_status(data, order_id, ST_CLARIFY)
    if order is None:
        return
    data["clients"][order["client_id"]].update(correction)
    order["status"] = ST_SALES_CHECK
    add_history(order, f"клиент скорректировал данные: {correction}")
    print("Данные отправлены в отдел продаж на повторную проверку")


def agree_date_action(data, order_id, delivery_date):
    order = order_in_status(data, order_id, ST_DELIVERY_DATE)
    if order is None:
        return
    agree_delivery_date(data["clients"][order["client_id"]], order, delivery_date)
    pass_info_to_delivery(order)


def decide_action(data, order_id, accepted):
    order = order_in_status(data, order_id, ST_DELIVERED)
    if order is None:
        return
    order["status"] = ST_ACCEPTED if accepted else ST_DECLINED
    add_history(
        order, "клиент принял заказ" if accepted else "клиент отказался от заказа"
    )
    print("Решение передано в отдел доставки")


def parse_line(text, products):
    parts = text.split()
    if len(parts) != 2 or not (parts[0].isdecimal() and parts[1].isdecimal()):
        print("Нужно два числа: ID товара и количество, например: 101 25")
        return None
    pid, qty = int(parts[0]), int(parts[1])
    if pid not in products:
        print(f"Товара с ID {pid} нет в списке.")
        return None
    if qty <= 0:
        print("Количество должно быть больше нуля.")
        return None
    return pid, qty


def input_lines(products):
    print("\nВведите товары: ID и количество через пробел. Пустая строка - закончить.")
    lines = {}
    while True:
        text = input("Товар> ").strip()
        if text == "":
            if lines:
                return list(lines.items())
            print("Добавьте хотя бы одну позицию.")
            continue
        parsed = parse_line(text, products)
        if parsed is None:
            continue
        pid, qty = parsed
        lines[pid] = lines.get(pid, 0) + qty
        print(f"  В заказе: {products[pid]['name']} - {lines[pid]} шт.")


def choose_delivery_type():
    print("\n  Способ получения:\n    1. Самовывоз\n    2. Доставка")
    return "самовывоз" if ask_int("Выберите способ (1-2): ", 1, 2) == 1 else "доставка"


def choose_client(path):
    data = read_state(path)
    print("\n  Кто вы?")
    for cid in sorted(data["clients"]):
        c = data["clients"][cid]
        print(
            f"    {cid}. {c['name']}  тел.: {c['phone'] or '-'}  адрес: {c['address'] or '-'}"
        )
    print("    0. Я новый клиент")
    choice = ask_from_ids("Выберите номер: ", set(data["clients"]) | {0})
    if choice != 0:
        return choice
    name = ask_nonempty("Название клиента: ")
    phone = ask_text("Телефон (можно пропустить): ")
    address = ask_text("Адрес (можно пропустить): ")
    client_id = run_locked(path, add_client_action, name, phone, address)
    print(f"Вы зарегистрированы под номером {client_id}.")
    return client_id


def client_tasks(data, client_id):
    tasks = []
    for oid in sorted(data["orders"]):
        order = data["orders"][oid]
        if order["client_id"] == client_id:
            label = find_label(CLIENT_TASKS, order["status"])
            if label:
                tasks.append((oid, label))
    return tasks


def client_new_order(path, client_id):
    data = read_state(path)
    print("\n--- Новый заказ ---")
    show_products(data["products"], available_lots(data))
    lines = input_lines(data["products"])
    delivery_type = choose_delivery_type()
    print("\n  Ваш заказ:")
    total = 0
    for pid, qty in lines:
        cost = data["products"][pid]["price"] * qty
        total += cost
        print(f"    {data['products'][pid]['name']} x {qty} = {cost:,.2f} руб.")
    print(f"  Получение: {delivery_type}; итого: {total:,.2f} руб.")
    if not ask_yes_no("Отправить заказ?"):
        print("Оформление отменено.")
        return
    print()
    run_locked(path, submit_order_action, client_id, lines, delivery_type)


def client_show_orders(path, client_id):
    show_all_orders(read_state(path), client_id)


def client_perform(path, order_id):
    data = read_state(path)
    order = data["orders"][order_id]
    show_order(data, order)
    status = order["status"]
    if status == ST_CLARIFY:
        print(f"\n  Отдел продаж просит уточнить: {order['clarification']}")
        correction = {}
        phone = ask_text("  Новый телефон (Enter - не менять): ")
        address = ask_text("  Новый адрес (Enter - не менять): ")
        if phone:
            correction["phone"] = phone
        if address:
            correction["address"] = address
        run_locked(path, correct_data_action, order_id, correction)
    elif status == ST_DELIVERY_DATE:
        print("\n  Заказ готов к отгрузке. Согласуйте дату доставки.")
        delivery_date = ask_date(
            "  Желаемая дата доставки (ГГГГ-ММ-ДД): ", date.today()
        )
        run_locked(path, agree_date_action, order_id, delivery_date)
    elif status == ST_DELIVERED:
        accepted = ask_yes_no("\n  Товар доставлен. Вы принимаете заказ?")
        run_locked(path, decide_action, order_id, accepted)


def run_client_console(path):
    client_id = choose_client(path)
    name = read_state(path)["clients"][client_id]["name"]
    extras = (
        ("n", "новый заказ", client_new_order),
        ("o", "мои заказы", client_show_orders),
    )
    return run_console_loop(
        path, f"КЛИЕНТ: {name}", client_id, client_tasks, client_perform, extras
    )
