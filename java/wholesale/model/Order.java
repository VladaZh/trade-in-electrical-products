package wholesale.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import wholesale.Config;
import wholesale.storage.Json;
import wholesale.util.Formats;
import wholesale.util.Out;

public abstract class Order implements JsonSerializable {
    public static final String PICKUP = "самовывоз";
    public static final String HOME_DELIVERY = "доставка";

    private final int orderId;
    private final int clientId;
    private final String created;
    private OrderStatus status = OrderStatus.CREATED;
    private final List<String> history = new ArrayList<>();
    private List<Pick> picks = new ArrayList<>();
    private boolean reserved;
    private String clarification = "";
    private String readyDate;
    private String deliveryDate;
    private Integer deliveryId;

    protected Order(int orderId, int clientId, String created) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.created = created;
    }

    public static Order create(int orderId, int clientId, String deliveryType, String created) {
        if (PICKUP.equals(deliveryType)) {
            return new PickupOrder(orderId, clientId, created);
        }
        if (HOME_DELIVERY.equals(deliveryType)) {
            return new HomeDeliveryOrder(orderId, clientId, created);
        }
        throw new IllegalArgumentException("Неизвестный способ получения: " + deliveryType);
    }

    public abstract String deliveryType();

    public abstract boolean requiresAddress();

    public abstract void onShippingDocumentsReady(ShopState state, Client client);


    public int getOrderId() {
        return orderId;
    }

    public int getClientId() {
        return clientId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<Pick> getPicks() {
        return Collections.unmodifiableList(picks);
    }

    public boolean isReserved() {
        return reserved;
    }

    public String getClarification() {
        return clarification;
    }

    public String getReadyDate() {
        return readyDate;
    }

    protected void setReadyDate(String readyDate) {
        this.readyDate = readyDate;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public Integer getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Integer deliveryId) {
        this.deliveryId = deliveryId;
    }

    public void addHistory(String text) {
        history.add(Formats.nowStamp() + "  " + text);
    }

    public void passInfoToSales() {
        status = OrderStatus.SALES_CHECK;
        addHistory("информация передана в отдел продаж");
        Out.println("Информация по заказу №" + orderId + " передана в отдел продаж");
    }

    public void confirmData() {
        addHistory("отдел продаж подтвердил корректность данных");
        Out.println("Корректность данных подтверждена");
    }

    public void sendInfoToWarehouse() {
        status = OrderStatus.WH_RECEIVED;
        addHistory("информация отправлена на склад");
        Out.println("Информация отправлена на склад отделом продаж");
    }

    public void clarifyWithClient(String text) {
        clarification = text;
        status = OrderStatus.CLARIFY;
        addHistory("отдел продаж запросил у клиента: " + text);
        Out.println("Запрос отправлен клиенту, ожидаем его ответа");
    }

    public void resubmitAfterCorrection(Map<String, String> correction) {
        status = OrderStatus.SALES_CHECK;
        addHistory("клиент скорректировал данные: " + Formats.pyDict(correction));
        Out.println("Данные отправлены в отдел продаж на повторную проверку");
    }

    public void receiveOrderInfo() {
        addHistory("склад получил информацию о заказе");
        Out.println("Склад получил информацию о заказе №" + orderId);
    }

    public void reserve(List<Pick> batchPicks) {
        picks = new ArrayList<>(batchPicks);
        reserved = true;
    }

    public void markBatchFormed() {
        addHistory("партия сформирована (" + picks.size() + " строк отбора)");
        status = OrderStatus.WH_BATCH;
    }

    public void reportReady() {
        addHistory("склад сообщил о готовности заказа");
        Out.println("Заказ №" + orderId + " готов на складе");
    }

    protected void notifyClientReady(ShopState state, Client client) {
        addHistory("клиенту отправлено сообщение: заказ готов к выдаче");
        state.sendMessage(client.getClientId(), orderId,
                "Заказ №" + orderId + " собран и готов к выдаче на складе. "
                        + "Срок хранения - " + Config.STORAGE_DAYS + " дн. Приходите за заказом.");
        Out.println("Клиенту «" + client.getName() + "» отправлено сообщение о готовности заказа №" + orderId);
    }

    public void issue() {
        addHistory("заказ выдан клиенту");
        Out.println("Заказ выдан клиенту со склада");
    }

    public void complete() {
        status = OrderStatus.RECEIVED;
        addHistory("заказ завершён");
        Out.println("Заказ завершён");
    }

    public void createDisassemblyDocuments() {
        status = OrderStatus.DISASSEMBLY_DOCS;
        addHistory("оформлены документы на расформирование");
        Out.println("Оформлены документы на расформирование заказа на складе");
    }

    public void disassemble() {
        status = OrderStatus.NOT_RECEIVED;
        reserved = false;
        addHistory("заказ расформирован, товар возвращён на места хранения");
        Out.println("Заказ расформирован, товар возвращён на места хранения на складе");
    }

    public void releaseReserve() {
        reserved = false;
    }

    public void agreeDeliveryDate(Client client, String date) {
        deliveryDate = date;
        addHistory("с клиентом согласована дата доставки " + date);
        Out.println("С клиентом «" + client.getName() + "» согласована дата доставки: " + date);
    }

    public void passInfoToDelivery() {
        status = OrderStatus.DELIVERY_NEW;
        addHistory("информация передана в отдел доставки");
        Out.println("Информация передана в отдел доставки");
    }

    public void loadGoods() {
        addHistory("товар загружен");
        Out.println("Наступила дата доставки; товар загружен");
        status = OrderStatus.DELIVERY_LOADED;
    }

    public void decide(boolean accepted) {
        status = accepted ? OrderStatus.ACCEPTED : OrderStatus.DECLINED;
        addHistory(accepted ? "клиент принял заказ" : "клиент отказался от заказа");
        Out.println("Решение передано в отдел доставки");
    }

    public void returnGoodsToWarehouse() {
        reserved = false;
        addHistory("товар возвращён на склад");
        Out.println("Товар возвращён на склад");
        status = OrderStatus.CANCELLED;
    }

    @Override
    public Map<String, Object> toJson() {
        List<Object> jsonPicks = new ArrayList<>();
        for (Pick p : picks) {
            jsonPicks.add(p.toJson());
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order_id", (long) orderId);
        m.put("client_id", (long) clientId);
        m.put("delivery_type", deliveryType());
        m.put("created", created);
        m.put("status", status.getLabel());
        m.put("history", new ArrayList<Object>(history));
        m.put("picks", jsonPicks);
        m.put("reserved", reserved);
        m.put("clarification", clarification);
        m.put("ready_date", readyDate);
        m.put("delivery_date", deliveryDate);
        m.put("delivery_id", deliveryId == null ? null : (Object) (long) deliveryId);
        return m;
    }

    public static Order fromJson(Map<String, Object> m) {
        Order order = create(Json.integer(m.get("order_id")), Json.integer(m.get("client_id")),
                Json.str(m.get("delivery_type")), Json.str(m.get("created")));
        order.status = OrderStatus.fromLabel(Json.str(m.get("status")));
        for (Object h : Json.arr(m.get("history"))) {
            order.history.add((String) h);
        }
        for (Object p : Json.arr(m.get("picks"))) {
            order.picks.add(Pick.fromJson(Json.obj(p)));
        }
        order.reserved = Json.bool(m.get("reserved"));
        order.clarification = Json.str(m.get("clarification"));
        order.readyDate = Json.str(m.get("ready_date"));
        order.deliveryDate = Json.str(m.get("delivery_date"));
        order.deliveryId = Json.integerOrNull(m.get("delivery_id"));
        return order;
    }
}
