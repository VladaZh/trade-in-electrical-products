package wholesale.model;

public enum OrderStatus {
    CREATED("создан"),
    SALES_CHECK("на проверке в отделе продаж"),
    CLARIFY("ожидает уточнения данных от клиента"),
    WH_NEW("передан на склад"),
    WH_RECEIVED("склад получил информацию о заказе"),
    WH_BATCH("партия сформирована"),
    PICKUP_READY("готов к выдаче (самовывоз)"),
    DISASSEMBLY_DOCS("оформлены документы на расформирование"),
    DELIVERY_DATE("ожидает согласования даты доставки"),
    DELIVERY_NEW("передан в отдел доставки"),
    DELIVERY_RECEIVED("отдел доставки получил информацию"),
    DELIVERY_LOADED("товар загружен"),
    DELIVERED("доставлен, ожидает решения клиента"),
    ACCEPTED("клиент принял заказ"),
    DECLINED("клиент отказался от заказа"),
    RETURNING("заказ отменён, товар возвращается на склад"),
    RECEIVED("заказ получен"),
    NOT_RECEIVED("заказ не получен"),
    DONE("заказ выполнен"),
    CANCELLED("заказ отменён");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static OrderStatus fromLabel(String label) {
        for (OrderStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Неизвестный статус заказа: " + label);
    }
}
