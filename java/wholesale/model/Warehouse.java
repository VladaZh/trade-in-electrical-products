package wholesale.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import wholesale.util.Out;

public class Warehouse {
    private List<StockLot> lots;

    public Warehouse(List<StockLot> lots) {
        this.lots = new ArrayList<>(lots);
    }

    public List<StockLot> getLots() {
        return Collections.unmodifiableList(lots);
    }

    public Warehouse freeStock(Collection<Order> orders) {
        Map<Integer, Long> reserved = new HashMap<>();
        for (Order order : orders) {
            if (order.isReserved()) {
                for (Pick p : order.getPicks()) {
                    reserved.merge(p.getLotId(), p.getQty(), Long::sum);
                }
            }
        }
        List<StockLot> free = new ArrayList<>();
        for (StockLot lot : lots) {
            free.add(lot.withQty(lot.getQty() - reserved.getOrDefault(lot.getLotId(), 0L)));
        }
        return new Warehouse(free);
    }

    public Map<Integer, Long> totalsByProduct() {
        Map<Integer, Long> totals = new HashMap<>();
        for (StockLot lot : lots) {
            totals.merge(lot.getProductId(), lot.getQty(), Long::sum);
        }
        return totals;
    }

    public AvailabilityResult checkAvailability(Map<Integer, Long> lines) {
        Map<Integer, Long> totals = totalsByProduct();
        AvailabilityResult result = new AvailabilityResult();
        for (Map.Entry<Integer, Long> line : lines.entrySet()) {
            long available = totals.getOrDefault(line.getKey(), 0L);
            if (line.getValue() > available) {
                result.addShortage(line.getKey(), line.getValue() - available, available);
            }
        }
        return result;
    }

    public PickBatch formBatch(List<OrderItem> items) {
        List<Pick> picks = new ArrayList<>();
        boolean complete = true;
        for (OrderItem item : items) {
            long need = item.getQty();
            List<StockLot> candidates = new ArrayList<>();
            for (StockLot lot : lots) {
                if (lot.getProductId() == item.getProductId() && lot.getQty() > 0) {
                    candidates.add(lot);
                }
            }
            candidates.sort(Comparator.comparing(StockLot::getReceived)); // сортировка устойчивая
            for (StockLot lot : candidates) {
                if (need == 0) {
                    break;
                }
                long take = Math.min(need, lot.getQty());
                picks.add(new Pick(lot.getLotId(), item.getProductId(), lot.getCell(), take));
                need -= take;
            }
            if (need > 0) {
                complete = false;
            }
        }
        return new PickBatch(picks, complete);
    }

    public void writeOff(Order order) {
        Map<Integer, Long> taken = new HashMap<>();
        for (Pick p : order.getPicks()) {
            taken.merge(p.getLotId(), p.getQty(), Long::sum);
        }
        List<StockLot> updated = new ArrayList<>();
        for (StockLot lot : lots) {
            updated.add(lot.withQty(lot.getQty() - taken.getOrDefault(lot.getLotId(), 0L)));
        }
        lots = updated;
        order.releaseReserve();
        order.addHistory("складские остатки обновлены");
        Out.println("Складские остатки обновлены");
    }
}
