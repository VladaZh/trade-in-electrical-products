from datetime import date

from src.data import STORAGE_DAYS

YES_WORDS = ("да", "д", "y", "yes")
NO_WORDS = ("нет", "н", "n", "no")


def ask_text(prompt):
    return input(prompt).strip()


def ask_nonempty(prompt):
    while True:
        text = input(prompt).strip()
        if text:
            return text
        print("Значение не может быть пустым.")


def ask_yes_no(prompt):
    while True:
        answer = input(f"{prompt} (да/нет): ").strip().lower()
        if answer in YES_WORDS:
            return True
        if answer in NO_WORDS:
            return False
        print("Введите «да» или «нет».")


def ask_int(prompt, low, high):
    while True:
        text = input(prompt).strip()
        if text.isdecimal() and low <= int(text) <= high:
            return int(text)
        print(f"Введите число от {low} до {high}.")


def ask_from_ids(prompt, valid_ids):
    while True:
        text = input(prompt).strip()
        if text.isdecimal() and int(text) in valid_ids:
            return int(text)
        print("Нет такого номера, попробуйте ещё раз.")


def ask_date(prompt, min_date):
    while True:
        text = input(prompt).strip()
        try:
            value = date.fromisoformat(text)
        except ValueError:
            print("Формат даты: ГГГГ-ММ-ДД, например 2026-11-20.")
            continue
        if value < min_date:
            print(f"Дата не может быть раньше {min_date}.")
            continue
        return value


def stock_totals(stock_lots):
    totals = {}
    for lot in stock_lots:
        pid = lot["product_id"]
        totals[pid] = totals.get(pid, 0) + lot["qty"]
    return totals


def show_products(products, stock_lots):
    totals = stock_totals(stock_lots)
    print("\n  ID   Товар                                   Цена, руб.   На складе")
    for pid in sorted(products):
        p = products[pid]
        print(f"{pid:<4} {p['name']:<38} {p['price']:>10,.2f} {totals.get(pid, 0):>10}")


def show_stock_lots(stock_lots, products):
    print(
        "\n  Партия  Товар                                   Остаток  Поступила   Ячейка"
    )
    for lot in stock_lots:
        name = products[lot["product_id"]]["name"]
        print(
            f"{lot['lot_id']:<7} {name:<38} {lot['qty']:>8}  {lot['received']}  {lot['cell']}"
        )


def show_orders(data):
    if not data["orders"]:
        print("\n  Заказов пока нет.")
        return
    print("\n  №   Клиент                      Получение    Статус")
    for oid, order in data["orders"].items():
        client = data["clients"][order["client_id"]]
        print(
            f"{oid:<3} {client['name']:<27} {order['delivery_type']:<12} {order['status']}"
        )


def show_order_summary(client, lines, products, delivery_type):
    print("\n  Ваш заказ:")
    total = 0
    for pid, qty in lines:
        cost = products[pid]["price"] * qty
        total += cost
        print(f"{products[pid]['name']} x {qty} = {cost:,.2f} руб.")
    print(
        f"Клиент: {client['name']}; получение: {delivery_type}; итого: {total:,.2f} руб."
    )


def show_menu():
    print("\n=== Оптовая поставка электротехнической продукции ===")
    print("1. Создать заказ")
    print("2. Остатки на складе (по партиям)")
    print("3. Список заказов")
    print("0. Выход")


def add_client(data):
    client_id = max(data["clients"]) + 1 if data["clients"] else 1
    client = {
        "client_id": client_id,
        "name": ask_nonempty("Название клиента: "),
        "phone": ask_text("Телефон (можно пропустить): "),
        "address": ask_text("Адрес (можно пропустить): "),
    }
    data["clients"][client_id] = client
    print(f"Клиент «{client['name']}» добавлен (№{client_id}).")
    return client_id


def choose_client(data):
    print("\n  Клиенты:")
    for cid in sorted(data["clients"]):
        c = data["clients"][cid]
        print(
            f"{cid}. {c['name']}  тел.: {c['phone'] or '-'}  адрес: {c['address'] or '-'}"
        )
    print("0. Новый клиент")
    choice = ask_from_ids("Выберите клиента (номер): ", set(data["clients"]) | {0})
    if choice == 0:
        return add_client(data)
    return choice


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
        print(f"В заказе: {products[pid]['name']} - {lines[pid]} шт.")


def choose_delivery_type():
    print("\nСпособ получения:")
    print("1. Самовывоз")
    print("2. Доставка")
    return "самовывоз" if ask_int("Выберите способ (1-2): ", 1, 2) == 1 else "доставка"


def read_order_request(data):
    print("\n--- Новый заказ ---")
    client_id = choose_client(data)
    show_products(data["products"], data["stock_lots"])
    lines = input_lines(data["products"])
    delivery_type = choose_delivery_type()
    show_order_summary(
        data["clients"][client_id], lines, data["products"], delivery_type
    )
    if not ask_yes_no("Оформить заказ?"):
        print("Оформление отменено.")
        return None
    print()
    return client_id, lines, delivery_type


def console_correction(errors, client, attempt):
    correction = {}
    for err in errors:
        if "телефон" in err:
            correction["phone"] = ask_text("Телефон клиента: ")
        elif "адрес" in err:
            correction["address"] = ask_text("Адрес доставки: ")
    return correction if correction else None


def console_arrival_day():
    print(f"Срок хранения заказа - {STORAGE_DAYS} дн.")
    if not ask_yes_no("Клиент придёт за заказом?"):
        return None
    return ask_int(
        f"На какой день хранения придёт клиент (1-{STORAGE_DAYS})? ", 1, STORAGE_DAYS
    )


def console_delivery_date():
    return ask_date(
        "Дата доставки, согласованная с клиентом (ГГГГ-ММ-ДД): ", date.today()
    )


def console_accepts():
    return ask_yes_no("Клиент принял заказ?")


def make_console_io():
    return {
        "correction": console_correction,
        "arrival_day": console_arrival_day,
        "delivery_date": console_delivery_date,
        "accepts": console_accepts,
    }
