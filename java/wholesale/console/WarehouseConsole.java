package wholesale.console;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import wholesale.Config;
import wholesale.model.Order;
import wholesale.model.OrderStatus;
import wholesale.model.PickBatch;
import wholesale.model.Pick;
import wholesale.model.ShopState;
import wholesale.storage.StateStore;
import wholesale.util.Out;

public class WarehouseConsole extends RoleConsole {
    private static final Map<OrderStatus, String> WAREHOUSE_TASKS = new LinkedHashMap<>();

    static {
        WAREHOUSE_TASKS.put(OrderStatus.WH_RECEIVED, "Сформировать партию");
        WAREHOUSE_TASKS.put(OrderStatus.WH_BATCH, "Оформить документы на отгрузку");
        WAREHOUSE_TASKS.put(OrderStatus.PICKUP_READY, "Самовывоз: срок хранения, выдача заказа");
        WAREHOUSE_TASKS.put(OrderStatus.DISASSEMBLY_DOCS, "Расформировать заказ");
    }

    public WarehouseConsole(StateStore store, ConsoleIO io) {
        super(store, io);
    }

    @Override
    protected String title() {
        return "СКЛАД";
    }

    @Override
    protected List<Task> tasks(ShopState data) {
        List<Task> tasks = new ArrayList<>();
        for (Order order : data.orders().values()) {
            String label = WAREHOUSE_TASKS.get(order.getStatus());
            if (label != null) {
                tasks.add(new Task(order.getOrderId(), label));
            }
        }
        return tasks;
    }

    @Override
    protected List<Extra> extras() {
        return List.of(
                new Extra("s", "остатки по партиям", () -> Views.showStockLots(store.read())),
                new Extra("o", "все заказы", () -> Views.showAllOrders(store.read(), null)));
    }

    @Override
    protected void onRefresh() {
        ShopState data = store.read();
        boolean any = data.orders().values().stream().anyMatch(o -> o.getStatus() == OrderStatus.WH_NEW);
        if (any) {
            store.runLockedVoid(s -> {
                for (Order order : s.orders().values()) {
                    if (order.getStatus() == OrderStatus.WH_NEW) {
                        order.receiveOrderInfo();
                        order.setStatus(OrderStatus.WH_RECEIVED);
                    }
                }
            });
        }
    }

    @Override
    protected void perform(int orderId) {
        ShopState data = store.read();
        Order order = data.order(orderId);
        Views.showOrder(data, order);
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.PICKUP_READY) {
            long days = ChronoUnit.DAYS.between(LocalDate.parse(order.getReadyDate()), LocalDate.now());
            Out.println("\n  Заказ готов с " + order.getReadyDate() + ", прошло дней: " + days
                    + ", срок хранения: " + Config.STORAGE_DAYS + " дн.");
            if (io.askYesNo("  Срок хранения заказа вышел?")) {
                store.runLockedVoid(s -> s.orderInStatus(orderId, OrderStatus.PICKUP_READY)
                        .ifPresent(Order::createDisassemblyDocuments));
            } else if (io.askYesNo("  Клиент пришёл?")) {
                store.runLockedVoid(s -> issueOrder(s, orderId));
            } else {
                Out.println("  Клиент не пришёл, заказ остаётся на хранении.");
            }
            return;
        }
        String label = WAREHOUSE_TASKS.get(status);
        if (io.askYesNo("\n  Выполнить задачу «" + label + "»?")) {
            store.runLockedVoid(s -> step(s, orderId, status));
        }
    }

    private void issueOrder(ShopState state, int orderId) {
        state.orderInStatus(orderId, OrderStatus.PICKUP_READY).ifPresent(order -> {
            order.issue();
            order.complete();
            state.warehouse().writeOff(order);
        });
    }

    private void step(ShopState state, int orderId, OrderStatus status) {
        Optional<Order> found = state.orderInStatus(orderId, status);
        if (found.isEmpty()) {
            return;
        }
        Order order = found.get();
        switch (status) {
            case WH_RECEIVED:
                formBatch(state, order);
                break;
            case WH_BATCH:
                state.createShippingDocuments(order, order.getPicks());
                order.reportReady();
                order.onShippingDocumentsReady(state, state.client(order.getClientId()));
                break;
            case DISASSEMBLY_DOCS:
                order.disassemble();
                break;
            default:
                break;
        }
    }

    private void formBatch(ShopState state, Order order) {
        PickBatch batch = state.freeStock().formBatch(state.itemsOf(order.getOrderId()));
        Out.println("Сформирована партия: " + batch.getPicks().size() + " строк отбора");
        if (!batch.isComplete()) {
            Out.println("Не удалось собрать заказ: товар зарезервирован под другие заказы.");
            return;
        }
        order.reserve(batch.getPicks());
        for (Pick p : batch.getPicks()) {
            String name = state.products().get(p.getProductId()).getName();
            Out.println("  партия " + p.getLotId() + ", ячейка " + p.getCell() + ": " + name + " - " + p.getQty() + " шт.");
        }
        order.markBatchFormed();
    }
}
