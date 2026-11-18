package wholesale.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import wholesale.model.Client;
import wholesale.model.Order;
import wholesale.model.OrderItem;
import wholesale.model.Product;

public class OrderItemsRule implements OrderCheckRule {
    @Override
    public List<String> check(Order order, Client client, List<OrderItem> items, Map<Integer, Product> products) {
        List<String> errors = new ArrayList<>();
        for (OrderItem item : items) {
            if (!products.containsKey(item.getProductId())) {
                errors.add("неизвестный товар " + item.getProductId());
            }
            if (item.getQty() <= 0) {
                errors.add("некорректное количество по товару " + item.getProductId());
            }
        }
        return errors;
    }
}
