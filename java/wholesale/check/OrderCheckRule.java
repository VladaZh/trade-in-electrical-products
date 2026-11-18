package wholesale.check;

import java.util.List;
import java.util.Map;
import wholesale.model.Client;
import wholesale.model.Order;
import wholesale.model.OrderItem;
import wholesale.model.Product;

public interface OrderCheckRule {
    List<String> check(Order order, Client client, List<OrderItem> items, Map<Integer, Product> products);
}
