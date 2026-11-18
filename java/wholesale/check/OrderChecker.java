package wholesale.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import wholesale.model.Client;
import wholesale.model.Order;
import wholesale.model.OrderItem;
import wholesale.model.Product;

public class OrderChecker {
    private final List<OrderCheckRule> rules;

    public OrderChecker(List<OrderCheckRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static OrderChecker standard() {
        return new OrderChecker(List.of(new ClientPhoneRule(), new DeliveryAddressRule(), new OrderItemsRule()));
    }

    public List<String> check(Order order, Map<Integer, Client> clients, List<OrderItem> items,
                              Map<Integer, Product> products) {
        Client client = clients.get(order.getClientId());
        if (client == null) {
            return List.of("клиент не найден");
        }
        List<String> errors = new ArrayList<>();
        for (OrderCheckRule rule : rules) {
            errors.addAll(rule.check(order, client, items, products));
        }
        return errors;
    }
}
