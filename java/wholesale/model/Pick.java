package wholesale.model;

import java.util.LinkedHashMap;
import java.util.Map;
import wholesale.storage.Json;

public class Pick {
    private final int lotId;
    private final int productId;
    private final String cell;
    private final long qty;

    public Pick(int lotId, int productId, String cell, long qty) {
        this.lotId = lotId;
        this.productId = productId;
        this.cell = cell;
        this.qty = qty;
    }

    public int getLotId() {
        return lotId;
    }

    public int getProductId() {
        return productId;
    }

    public String getCell() {
        return cell;
    }

    public long getQty() {
        return qty;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lot_id", (long) lotId);
        m.put("product_id", (long) productId);
        m.put("cell", cell);
        m.put("qty", qty);
        return m;
    }

    public static Pick fromJson(Map<String, Object> m) {
        return new Pick(Json.integer(m.get("lot_id")), Json.integer(m.get("product_id")),
                Json.str(m.get("cell")), Json.lng(m.get("qty")));
    }
}
