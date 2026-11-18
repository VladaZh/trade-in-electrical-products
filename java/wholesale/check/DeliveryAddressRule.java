package wholesale.check;

import java.util.List;
import java.util.Map;
import wholesale.model.Client;
import wholesale.model.Order;
import wholesale.model.OrderItem;
import wholesale.model.Product;

public class DeliveryAddressRule implements OrderCheckRule {
    @Override
    public List<String> check(Order order, Client client, List<OrderItem> items, Map<Integer, Product> products) {
        if (order.requiresAddress() && !client.hasAddress()) {
            return List.of("не указан адрес доставки");
        }
        return List.of();
    }
}
