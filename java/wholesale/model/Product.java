package wholesale.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import wholesale.storage.Json;

public class Product implements JsonSerializable {
    private final int productId;
    private final String name;
    private final BigDecimal price;

    public Product(int productId, String name, BigDecimal price) {
        this.productId = productId;
        this.name = name;
        this.price = price;
    }

    public int getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal lineCost(long qty) {
        return price.multiply(BigDecimal.valueOf(qty));
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product_id", (long) productId);
        m.put("name", name);
        m.put("price", price);
        return m;
    }

    public static Product fromJson(Map<String, Object> m) {
        return new Product(Json.integer(m.get("product_id")), Json.str(m.get("name")),
                Json.decimal(m.get("price")));
    }
}
