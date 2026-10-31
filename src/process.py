from src.app import (
    agree_delivery_date,
    check_stock_availability,
    create_order,
    notify_client_ready,
    pass_info_to_delivery,
    pass_info_to_sales,
    reject_order,
    show_max_available,
    update_stock,
)
from src.data import MAX_CLARIFY_ATTEMPTS
from src.delivery import (
    cancel_order,
    deliver_goods,
    load_goods,
    receive_delivery_info,
    record_delivery,
    return_goods_to_warehouse,
)
from src.sales_department import (
    apply_client_correction,
    check_order_correctness,
    clarify_with_client,
    confirm_data,
    send_info_to_warehouse,
)
from src.warehouse import (
    create_shipping_documents,
    form_batch,
    receive_order_info,
    report_order_ready,
    run_pickup,
)


def process_order(data, client_id, lines, delivery_type, io):
    client = data["clients"][client_id]
    lots = data["stock_lots"]

    ok, shortage, max_avail = check_stock_availability(lines, lots)
    if not ok:
        reject_order(client, shortage)
        show_max_available(max_avail, data["products"])
        return "Отменить создание текущего заказа", lots
    order, items = create_order(data, client_id, lines, delivery_type)
    pass_info_to_sales(order)

    errors = check_order_correctness(order, items, data["clients"], data["products"])
    attempt = 0
    while errors:
        if attempt >= MAX_CLARIFY_ATTEMPTS:
            order["status"] = "отменён"
            print("Данные не уточнены - заказ отменён")
            return "Заказ отменён", lots
        clarify_with_client(errors)
        correction = io["correction"](errors, client, attempt)
        if correction is None:
            order["status"] = "отменён"
            print("Данные не уточнены - заказ отменён")
            return "Заказ отменён", lots
        apply_client_correction(client, correction)
        attempt += 1
        errors = check_order_correctness(
            order, items, data["clients"], data["products"]
        )
    confirm_data(order)
    send_info_to_warehouse(order)

    receive_order_info(order)
    picks, _complete = form_batch(items, lots)
    create_shipping_documents(data, order, picks)
    report_order_ready(order)

    if order["delivery_type"] == "самовывоз":
        notify_client_ready(client, order)
        arrival_day = io["arrival_day"]()
        result = run_pickup(order, arrival_day)
        if result == "Заказ получен":
            lots = update_stock(lots, picks)
        return result, lots

    date_ = agree_delivery_date(client, order, io["delivery_date"]())
    pass_info_to_delivery(order)
    delivery = receive_delivery_info(data, order, date_)
    load_goods(order)
    deliver_goods(delivery, client["address"])
    if io["accepts"]():
        record_delivery(delivery, order)
        lots = update_stock(lots, picks)
        order["status"] = "выполнен"
        return "Заказ выполнен", lots
    cancel_order(order, delivery)
    return_goods_to_warehouse(order)
    return "Заказ отменен", lots
