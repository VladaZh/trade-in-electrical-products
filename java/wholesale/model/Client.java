package wholesale.model;

import java.util.LinkedHashMap;
import java.util.Map;
import wholesale.storage.Json;
import wholesale.util.Out;

public class Client implements JsonSerializable {
    private final int clientId;
    private final String name;
    private String phone;
    private String address;

    public Client(int clientId, String name, String phone, String address) {
        this.clientId = clientId;
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public int getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public boolean hasPhone() {
        return phone != null && !phone.isEmpty();
    }

    public boolean hasAddress() {
        return address != null && !address.isEmpty();
    }

    public void applyCorrection(String newPhone, String newAddress) {
        if (newPhone != null) {
            phone = newPhone;
        }
        if (newAddress != null) {
            address = newAddress;
        }
    }

    public void rejectOrder(AvailabilityResult result) {
        Out.println("Заказ клиента «" + name + "» отклонён: не хватает товара по "
                + result.getShortage().size() + " поз.");
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("client_id", (long) clientId);
        m.put("name", name);
        m.put("phone", phone);
        m.put("address", address);
        return m;
    }

    public static Client fromJson(Map<String, Object> m) {
        return new Client(Json.integer(m.get("client_id")), Json.str(m.get("name")),
                Json.str(m.get("phone")), Json.str(m.get("address")));
    }
}
