package wholesale.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import wholesale.util.Out;

public class AvailabilityResult {
    private final Map<Integer, Long> shortage = new LinkedHashMap<>();
    private final Map<Integer, Long> maxAvailable = new LinkedHashMap<>();

    void addShortage(int productId, long missing, long available) {
        shortage.put(productId, missing);
        maxAvailable.put(productId, available);
    }

    public boolean isOk() {
        return shortage.isEmpty();
    }

    public Map<Integer, Long> getShortage() {
        return Collections.unmodifiableMap(shortage);
    }

    public Map<Integer, Long> getMaxAvailable() {
        return Collections.unmodifiableMap(maxAvailable);
    }

    public void showMaxAvailable(Map<Integer, Product> products) {
        for (Map.Entry<Integer, Long> e : maxAvailable.entrySet()) {
            Out.println("Доступно максимум: " + products.get(e.getKey()).getName() + " - " + e.getValue() + " шт.");
        }
    }
}
