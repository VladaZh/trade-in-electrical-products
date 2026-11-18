package wholesale.console;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import wholesale.model.Client;
import wholesale.model.Order;
import wholesale.model.OrderItem;
import wholesale.model.Product;
import wholesale.model.ShopState;
import wholesale.model.StockLot;
import wholesale.util.Formats;
import wholesale.util.Out;

public final class Views {
    private Views() {
    }

    public static void showProducts(ShopState data) {
        Map<Integer, Long> totals = data.freeStock().totalsByProduct();
        Out.println("\n  ID   Товар                                   Цена, руб.   Доступно");
        for (Product p : data.products().values()) {
            Out.println("  " + Formats.padRight(String.valueOf(p.getProductId()), 4)
                    + " " + Formats.padRight(p.getName(), 38)
                    + " " + Formats.padLeft(Formats.money(p.getPrice()), 10)
                    + " " + Formats.padLeft(String.valueOf(totals.getOrDefault(p.getProductId(), 0L)), 10));
        }
    }

    public static void showStockLots(ShopState data) {
        Map<Integer, Long> free = new HashMap<>();
        for (StockLot lot : data.freeStock().getLots()) {
            free.put(lot.getLotId(), lot.getQty());
        }
        Out.println("\n  Партия  Товар                                   Остаток  В резерве  Поступила   Ячейка");
        for (StockLot lot : data.warehouse().getLots()) {
            String name = data.products().get(lot.getProductId()).getName();
            long reserved = lot.getQty() - free.get(lot.getLotId());
            Out.println("  " + Formats.padRight(String.valueOf(lot.getLotId()), 7)
                    + " " + Formats.padRight(name, 38)
                    + " " + Formats.padLeft(String.valueOf(lot.getQty()), 8)
                    + " " + Formats.padLeft(String.valueOf(reserved), 10)
                    + "  " + lot.getReceived() + "  " + lot.getCell());
        }
    }

    public static void showOrder(ShopState data, Order order) {
        Client client = data.client(order.getClientId());
        Out.println("\n  Заказ №" + order.getOrderId() + "   клиент: " + client.getName()
                + "   получение: " + order.deliveryType());
        Out.println("  Статус: " + order.getStatus().getLabel());
        for (OrderItem item : data.itemsOf(order.getOrderId())) {
            String name = data.products().get(item.getProductId()).getName();
            Out.println("    - " + name + " x " + item.getQty() + " (по " + Formats.money(item.getPrice()) + " руб.)");
        }
        Out.println("  Контакты клиента: тел.: " + orDefault(client.getPhone(), "НЕ УКАЗАНО")
                + "; адрес: " + orDefault(client.getAddress(), "НЕ УКАЗАНО"));
        List<String> history = order.getHistory();
        for (String line : history.subList(Math.max(0, history.size() - 6), history.size())) {
            Out.println("    · " + line);
        }
    }

    public static void showAllOrders(ShopState data, Integer clientId) {
        int shown = 0;
        for (Order order : data.orders().values()) {
            if (clientId == null || order.getClientId() == clientId) {
                showOrder(data, order);
                shown++;
            }
        }
        if (shown == 0) {
            Out.println("\n  Заказов пока нет.");
        }
    }

    static String orDefault(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
