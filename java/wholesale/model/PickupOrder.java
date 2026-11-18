package wholesale.model;

public class PickupOrder extends Order {
    public PickupOrder(int orderId, int clientId, String created) {
        super(orderId, clientId, created);
    }

    @Override
    public String deliveryType() {
        return PICKUP;
    }

    @Override
    public boolean requiresAddress() {
        return false;
    }

    @Override
    public void onShippingDocumentsReady(ShopState state, Client client) {
        notifyClientReady(state, client);
        setReadyDate(java.time.LocalDate.now().toString());
        setStatus(OrderStatus.PICKUP_READY);
    }
}
