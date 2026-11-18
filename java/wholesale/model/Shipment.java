package wholesale.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import wholesale.storage.Json;

public class Shipment implements JsonSerializable {
    private final int shipmentId;
    private final int orderId;
    private final List<Pick> lines;
    private final String date;
    private final String status;

    public Shipment(int shipmentId, int orderId, List<Pick> lines, String date, String status) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.lines = new ArrayList<>(lines);
        this.date = date;
        this.status = status;
    }

    public int getShipmentId() {
        return shipmentId;
    }

    public int getOrderId() {
        return orderId;
    }

    @Override
    public Map<String, Object> toJson() {
        List<Object> jsonLines = new ArrayList<>();
        for (Pick p : lines) {
            jsonLines.add(p.toJson());
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shipment_id", (long) shipmentId);
        m.put("order_id", (long) orderId);
        m.put("lines", jsonLines);
        m.put("date", date);
        m.put("status", status);
        return m;
    }

    public static Shipment fromJson(Map<String, Object> m) {
        List<Pick> picks = new ArrayList<>();
        for (Object o : Json.arr(m.get("lines"))) {
            picks.add(Pick.fromJson(Json.obj(o)));
        }
        return new Shipment(Json.integer(m.get("shipment_id")), Json.integer(m.get("order_id")),
                picks, Json.str(m.get("date")), Json.str(m.get("status")));
    }
}
