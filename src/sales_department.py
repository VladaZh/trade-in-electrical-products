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


def clarify_with_client(errors):
    print("Отдел продаж уточняет у клиента: " + "; ".join(errors))


def apply_client_correction(client, correction):
    client.update(correction)
    print(f"Клиент скорректировал данные: {correction}")


def confirm_data(order):
    order["status"] = "данные подтверждены"
    print("Корректность данных подтверждена")


def send_info_to_warehouse(order):
    order["status"] = "передан на склад"
    print("Информация отправлена на склад отделом продаж")
