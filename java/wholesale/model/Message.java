package wholesale.model;

import java.util.LinkedHashMap;
import java.util.Map;
import wholesale.storage.Json;

public class Message {
    private final int clientId;
    private final int orderId;
    private final String text;
    private final String time;
    private boolean read;

    public Message(int clientId, int orderId, String text, String time, boolean read) {
        this.clientId = clientId;
        this.orderId = orderId;
        this.text = text;
        this.time = time;
        this.read = read;
    }

    public int getClientId() {
        return clientId;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getText() {
        return text;
    }

    public String getTime() {
        return time;
    }

    public boolean isRead() {
        return read;
    }

    public void markRead() {
        read = true;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("client_id", (long) clientId);
        m.put("order_id", (long) orderId);
        m.put("text", text);
        m.put("time", time);
        m.put("read", read);
        return m;
    }

    public static Message fromJson(Map<String, Object> m) {
        return new Message(Json.integer(m.get("client_id")), Json.integer(m.get("order_id")),
                Json.str(m.get("text")), Json.str(m.get("time")), Json.bool(m.get("read")));
    }
}
