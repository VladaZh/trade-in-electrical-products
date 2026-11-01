from datetime import date

from app import available_lots
from storage import read_state

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
        return value.isoformat()


def show_products(products, lots):
    totals = {}
    for lot in lots:
        totals[lot["product_id"]] = totals.get(lot["product_id"], 0) + lot["qty"]
    print("\n  ID   Товар                                   Цена, руб.   Доступно")
    for pid in sorted(products):
        p = products[pid]
        print(
            f"  {pid:<4} {p['name']:<38} {p['price']:>10,.2f} {totals.get(pid, 0):>10}"
        )


def show_stock_lots(data):
    free = {lot["lot_id"]: lot["qty"] for lot in available_lots(data)}
    print(
        "\n  Партия  Товар                                   Остаток  В резерве  Поступила   Ячейка"
    )
    for lot in data["stock_lots"]:
        name = data["products"][lot["product_id"]]["name"]
        reserved = lot["qty"] - free[lot["lot_id"]]
        print(
            f"  {lot['lot_id']:<7} {name:<38} {lot['qty']:>8} {reserved:>10}  "
            f"{lot['received']}  {lot['cell']}"
        )


def show_order(data, order):
    client = data["clients"][order["client_id"]]
    print(
        f"\n  Заказ №{order['order_id']}   клиент: {client['name']}   получение: {order['delivery_type']}"
    )
    print(f"  Статус: {order['status']}")
    for it in data["order_items"]:
        if it["order_id"] == order["order_id"]:
            name = data["products"][it["product_id"]]["name"]
            print(f"    - {name} x {it['qty']} (по {it['price']:,.2f} руб.)")
    print(
        f"  Контакты клиента: тел.: {client['phone'] or 'НЕ УКАЗАНО'}; "
        f"адрес: {client['address'] or 'НЕ УКАЗАНО'}"
    )
    for line in order["history"][-6:]:
        print(f"    · {line}")


def show_all_orders(data, client_id=None):
    shown = 0
    for oid in sorted(data["orders"]):
        order = data["orders"][oid]
        if client_id is None or order["client_id"] == client_id:
            show_order(data, order)
            shown += 1
    if shown == 0:
        print("\n  Заказов пока нет.")


def ask_task_choice(count, extras):
    keys = ", ".join(f"{k} - {label}" for k, label, _fn in extras)
    hint = "номер задачи" if count else "Enter"
    while True:
        text = (
            input(
                f"\nВыбор ({hint}; {keys + '; ' if keys else ''}Enter - обновить; 0 - выход): "
            )
            .strip()
            .lower()
        )
        if text == "":
            return "refresh", None
        if text == "0":
            return "exit", None
        for k, _label, _fn in extras:
            if text == k:
                return "extra", k
        if text.isdecimal() and 1 <= int(text) <= count:
            return "task", int(text)
        print("Нет такой команды.")


def run_console_loop(path, title, ctx, tasks_fn, perform_fn, extras):
    while True:
        data = read_state(path)
        print(f"\n=========== {title} ===========")
        tasks = tasks_fn(data, ctx)
        if tasks:
            print("Задачи, ожидающие вашего решения:")
            for i, (oid, label) in enumerate(tasks, start=1):
                client = data["clients"][data["orders"][oid]["client_id"]]["name"]
                print(f"  {i}. Заказ №{oid} ({client}): {label}")
        else:
            print("Сейчас задач нет. Нажмите Enter, чтобы обновить список.")
        kind, value = ask_task_choice(len(tasks), extras)
        if kind == "exit":
            print("Выход.")
            return 0
        if kind == "extra":
            for k, _label, fn in extras:
                if k == value:
                    fn(path, ctx)
        elif kind == "task":
            perform_fn(path, tasks[value - 1][0])
