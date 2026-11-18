package wholesale.model;

import java.util.LinkedHashMap;
import java.util.Map;
import wholesale.storage.Json;
import wholesale.util.Out;

public class Delivery implements JsonSerializable {
    private final int deliveryId;
    private final int orderId;
    private final String plannedDate;
    private String status;

    public Delivery(int deliveryId, int orderId, String plannedDate, String status) {
        this.deliveryId = deliveryId;
        this.orderId = orderId;
        this.plannedDate = plannedDate;
        this.status = status;
    }

    public int getDeliveryId() {
        return deliveryId;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getPlannedDate() {
        return plannedDate;
    }

    public String getStatus() {
        return status;
    }

    public void deliverGoods(Order order, String address) {
        status = "в пути";
        order.addHistory("товар доставлен по адресу: " + address);
        Out.println("Товар доставлен по адресу: " + address);
        order.setStatus(OrderStatus.DELIVERED);
    }

    public void recordDelivery(Order order) {
        status = "доставлена";
        order.addHistory("факт доставки зафиксирован");
        Out.println("Факт доставки зафиксирован");
    }

    public void cancel(Order order) {
        status = "отклонена клиентом";
        order.addHistory("заказ отменён");
        Out.println("Клиент не принял заказ - заказ отменён");
        order.setStatus(OrderStatus.RETURNING);
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("delivery_id", (long) deliveryId);
        m.put("order_id", (long) orderId);
        m.put("planned_date", plannedDate);
        m.put("status", status);
        return m;
    }

    public static Delivery fromJson(Map<String, Object> m) {
        return new Delivery(Json.integer(m.get("delivery_id")), Json.integer(m.get("order_id")),
                Json.str(m.get("planned_date")), Json.str(m.get("status")));
    }
}
