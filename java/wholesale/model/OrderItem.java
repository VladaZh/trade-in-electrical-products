package wholesale.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import wholesale.storage.Json;

public class OrderItem {
    private final int orderId;
    private final int productId;
    private final long qty;
    private final BigDecimal price;

    public OrderItem(int orderId, int productId, long qty, BigDecimal price) {
        this.orderId = orderId;
        this.productId = productId;
        this.qty = qty;
        this.price = price;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getProductId() {
        return productId;
    }

    public long getQty() {
        return qty;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal cost() {
        return price.multiply(BigDecimal.valueOf(qty));
    }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order_id", (long) orderId);
        m.put("product_id", (long) productId);
        m.put("qty", qty);
        m.put("price", price);
        return m;
    }

    public static OrderItem fromJson(Map<String, Object> m) {
        return new OrderItem(Json.integer(m.get("order_id")), Json.integer(m.get("product_id")),
                Json.lng(m.get("qty")), Json.decimal(m.get("price")));
    }
}
