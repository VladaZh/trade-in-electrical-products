package wholesale.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import wholesale.storage.Json;
import wholesale.util.Formats;
import wholesale.util.Out;

public class ShopState {
    private final Map<Integer, Client> clients = new TreeMap<>();
    private final Map<Integer, Product> products = new TreeMap<>();
    private final Warehouse warehouse;
    private final Map<Integer, Order> orders = new TreeMap<>();
    private final List<OrderItem> orderItems = new ArrayList<>();
    private final Map<Integer, Shipment> shipments = new TreeMap<>();
    private final Map<Integer, Delivery> deliveries = new TreeMap<>();
    private final List<Message> messages = new ArrayList<>();

    private ShopState(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public static ShopState initial() {
        List<StockLot> lots = new ArrayList<>();
        lots.add(new StockLot(1, 101, 20, "2026-09-01", "A-01"));
        lots.add(new StockLot(2, 101, 15, "2026-10-05", "A-02"));
        lots.add(new StockLot(3, 102, 500, "2026-09-20", "B-01"));
        lots.add(new StockLot(4, 103, 40, "2026-10-01", "C-01"));
        lots.add(new StockLot(5, 103, 60, "2026-08-15", "C-02"));
        ShopState s = new ShopState(new Warehouse(lots));
        s.clients.put(1, new Client(1, "ООО «ЭлектроМонтаж»", "+7-495-111-22-33", "Москва, ул. Складочная, 1"));
        s.clients.put(2, new Client(2, "ИП Соколов А.В.", "+7-916-555-44-33", ""));
        s.products.put(101, new Product(101, "Кабель ВВГнг 3x2.5 (бухта 100 м)", new BigDecimal("9500.0")));
        s.products.put(102, new Product(102, "Автомат C16 1P", new BigDecimal("180.0")));
        s.products.put(103, new Product(103, "Розетка с заземлением", new BigDecimal("95.0")));
        return s;
    }

    public Map<Integer, Client> clients() {
        return Collections.unmodifiableMap(clients);
    }

    public Client client(int clientId) {
        return clients.get(clientId);
    }

    public Map<Integer, Product> products() {
        return Collections.unmodifiableMap(products);
    }

    public Map<Integer, Order> orders() {
        return Collections.unmodifiableMap(orders);
    }

    public Order order(int orderId) {
        return orders.get(orderId);
    }

    public List<OrderItem> itemsOf(int orderId) {
        List<OrderItem> result = new ArrayList<>();
        for (OrderItem item : orderItems) {
            if (item.getOrderId() == orderId) {
                result.add(item);
            }
        }
        return result;
    }

    public Warehouse warehouse() {
        return warehouse;
    }

    public Warehouse freeStock() {
        return warehouse.freeStock(orders.values());
    }

    public Map<Integer, Delivery> deliveries() {
        return Collections.unmodifiableMap(deliveries);
    }

    public Delivery delivery(Integer deliveryId) {
        return deliveryId == null ? null : deliveries.get(deliveryId);
    }

    public Map<Integer, Shipment> shipments() {
        return Collections.unmodifiableMap(shipments);
    }

    private static int nextId(Map<Integer, ?> map) {
        return map.isEmpty() ? 1 : ((TreeMap<Integer, ?>) map).lastKey() + 1;
    }

    public int addClient(String name, String phone, String address) {
        int id = nextId(clients);
        clients.put(id, new Client(id, name, phone, address));
        return id;
    }

    public Optional<Order> orderInStatus(int orderId, OrderStatus status) {
        Order order = orders.get(orderId);
        if (order == null || order.getStatus() != status) {
            Out.println("Заказ уже обработан другим сотрудником, обновите список.");
            return Optional.empty();
        }
        return Optional.of(order);
    }

    public Order createOrder(int clientId, Map<Integer, Long> lines, String deliveryType) {
        int id = nextId(orders);
        Order order = Order.create(id, clientId, deliveryType, LocalDate.now().toString());
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Integer, Long> line : lines.entrySet()) {
            items.add(new OrderItem(id, line.getKey(), line.getValue(), products.get(line.getKey()).getPrice()));
        }
        orders.put(id, order);
        orderItems.addAll(items);
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            total = total.add(item.cost());
        }
        order.addHistory("заказ создан клиентом");
        Out.println("Сформирован заказ №" + id + ", сумма " + Formats.money(total) + " руб.");
        return order;
    }

    public Delivery receiveDeliveryInfo(Order order, String plannedDate) {
        int id = nextId(deliveries);
        Delivery delivery = new Delivery(id, order.getOrderId(), plannedDate, "запланирована");
        deliveries.put(id, delivery);
        order.setDeliveryId(id);
        order.addHistory("отдел доставки получил информацию о доставке №" + id);
        Out.println("Отделом доставки получена информация о доставке №" + id);
        return delivery;
    }

    public Shipment createShippingDocuments(Order order, List<Pick> picks) {
        int id = nextId(shipments);
        Shipment shipment = new Shipment(id, order.getOrderId(), picks, LocalDate.now().toString(), "оформлена");
        shipments.put(id, shipment);
        order.addHistory("оформлены документы на отгрузку №" + id);
        Out.println("Склад оформил документы на отгрузку №" + id);
        return shipment;
    }

    public void sendMessage(int clientId, int orderId, String text) {
        messages.add(new Message(clientId, orderId, text, Formats.nowStamp(), false));
    }

    public boolean hasUnread(int clientId) {
        for (Message m : messages) {
            if (m.getClientId() == clientId && !m.isRead()) {
                return true;
            }
        }
        return false;
    }

    public List<Message> popMessages(int clientId) {
        List<Message> unread = new ArrayList<>();
        for (Message m : messages) {
            if (m.getClientId() == clientId && !m.isRead()) {
                unread.add(m);
            }
        }
        for (Message m : unread) {
            m.markRead();
        }
        return unread;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("clients", jsonById(clients));
        root.put("products", jsonById(products));
        List<Object> lots = new ArrayList<>();
        for (StockLot lot : warehouse.getLots()) {
            lots.add(lot.toJson());
        }
        root.put("stock_lots", lots);
        root.put("orders", jsonById(orders));
        List<Object> items = new ArrayList<>();
        for (OrderItem item : orderItems) {
            items.add(item.toJson());
        }
        root.put("order_items", items);
        root.put("shipments", jsonById(shipments));
        root.put("deliveries", jsonById(deliveries));
        List<Object> msgs = new ArrayList<>();
        for (Message m : messages) {
            msgs.add(m.toJson());
        }
        root.put("messages", msgs);
        return root;
    }

    private static Map<String, Object> jsonById(Map<Integer, ? extends JsonSerializable> byId) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, ? extends JsonSerializable> e : byId.entrySet()) {
            result.put(String.valueOf(e.getKey()), e.getValue().toJson());
        }
        return result;
    }

    public static ShopState fromJson(Map<String, Object> root) {
        List<StockLot> lots = new ArrayList<>();
        for (Object o : Json.arr(root.get("stock_lots"))) {
            lots.add(StockLot.fromJson(Json.obj(o)));
        }
        ShopState s = new ShopState(new Warehouse(lots));
        for (Map.Entry<String, Object> e : Json.obj(root.get("clients")).entrySet()) {
            s.clients.put(Integer.valueOf(e.getKey()), Client.fromJson(Json.obj(e.getValue())));
        }
        for (Map.Entry<String, Object> e : Json.obj(root.get("products")).entrySet()) {
            s.products.put(Integer.valueOf(e.getKey()), Product.fromJson(Json.obj(e.getValue())));
        }
        for (Map.Entry<String, Object> e : Json.obj(root.get("orders")).entrySet()) {
            s.orders.put(Integer.valueOf(e.getKey()), Order.fromJson(Json.obj(e.getValue())));
        }
        for (Object o : Json.arr(root.get("order_items"))) {
            s.orderItems.add(OrderItem.fromJson(Json.obj(o)));
        }
        for (Map.Entry<String, Object> e : Json.obj(root.get("shipments")).entrySet()) {
            s.shipments.put(Integer.valueOf(e.getKey()), Shipment.fromJson(Json.obj(e.getValue())));
        }
        for (Map.Entry<String, Object> e : Json.obj(root.get("deliveries")).entrySet()) {
            s.deliveries.put(Integer.valueOf(e.getKey()), Delivery.fromJson(Json.obj(e.getValue())));
        }
        Object msgs = root.get("messages");
        if (msgs != null) {
            for (Object o : Json.arr(msgs)) {
                s.messages.add(Message.fromJson(Json.obj(o)));
            }
        }
        return s;
    }
}
