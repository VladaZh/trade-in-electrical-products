package wholesale.model;

public class HomeDeliveryOrder extends Order {
    public HomeDeliveryOrder(int orderId, int clientId, String created) {
        super(orderId, clientId, created);
    }

    @Override
    public String deliveryType() {
        return HOME_DELIVERY;
    }

    @Override
    public boolean requiresAddress() {
        return true;
    }

    @Override
    public void onShippingDocumentsReady(ShopState state, Client client) {
        addHistory("ожидается согласование даты доставки с клиентом");
        state.sendMessage(getClientId(), getOrderId(),
                "Заказ №" + getOrderId() + " собран. Согласуйте дату доставки ");
        setStatus(OrderStatus.DELIVERY_DATE);
    }
}
