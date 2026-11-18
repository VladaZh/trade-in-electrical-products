package wholesale.model;

import java.util.LinkedHashMap;
import java.util.Map;
import wholesale.storage.Json;

public class StockLot {
    private final int lotId;
    private final int productId;
    private final long qty;
    private final String received;
    private final String cell;

    public StockLot(int lotId, int productId, long qty, String received, String cell) {
        this.lotId = lotId;
        this.productId = productId;
        this.qty = qty;
        this.received = received;
        this.cell = cell;
    }

    public int getLotId() {
        return lotId;
    }

    public int getProductId() {
        return productId;
    }

    public long getQty() {
        return qty;
    }

    public String getReceived() {
        return received;
    }

    public String getCell() {
        return cell;
    }

    public StockLot withQty(long newQty) {
        return new StockLot(lotId, productId, newQty, received, cell);
    }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lot_id", (long) lotId);
        m.put("product_id", (long) productId);
        m.put("qty", qty);
        m.put("received", received);
        m.put("cell", cell);
        return m;
    }

    public static StockLot fromJson(Map<String, Object> m) {
        return new StockLot(Json.integer(m.get("lot_id")), Json.integer(m.get("product_id")),
                Json.lng(m.get("qty")), Json.str(m.get("received")), Json.str(m.get("cell")));
    }
}
