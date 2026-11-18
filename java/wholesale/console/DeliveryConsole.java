package wholesale.console;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import wholesale.model.Client;
import wholesale.model.Delivery;
import wholesale.model.Order;
import wholesale.model.OrderStatus;
import wholesale.model.ShopState;
import wholesale.storage.StateStore;
import wholesale.util.Out;

public class DeliveryConsole extends RoleConsole {
    private static final Map<OrderStatus, String> DELIVERY_TASKS = new LinkedHashMap<>();

    static {
        DELIVERY_TASKS.put(OrderStatus.DELIVERY_RECEIVED, "Загрузить товар (по наступлении даты доставки)");
        DELIVERY_TASKS.put(OrderStatus.DELIVERY_LOADED, "Доставить товар");
        DELIVERY_TASKS.put(OrderStatus.ACCEPTED, "Зафиксировать факт доставки");
        DELIVERY_TASKS.put(OrderStatus.DECLINED, "Отменить заказ");
        DELIVERY_TASKS.put(OrderStatus.RETURNING, "Вернуть товар на склад");
    }

    public DeliveryConsole(StateStore store, ConsoleIO io) {
        super(store, io);
    }

    @Override
    protected String title() {
        return "ОТДЕЛ ДОСТАВКИ";
    }

    @Override
    protected List<Task> tasks(ShopState data) {
        List<Task> tasks = new ArrayList<>();
        for (Order order : data.orders().values()) {
            String label = DELIVERY_TASKS.get(order.getStatus());
            if (label != null) {
                tasks.add(new Task(order.getOrderId(), label));
            }
        }
        return tasks;
    }

    @Override
    protected List<Extra> extras() {
        return List.of(new Extra("o", "все заказы", () -> Views.showAllOrders(store.read(), null)));
    }

    @Override
    protected void perform(int orderId) {
        ShopState data = store.read();
        Order order = data.order(orderId);
        Views.showOrder(data, order);
        OrderStatus status = order.getStatus();
        String label = DELIVERY_TASKS.get(status);
        if (status == OrderStatus.DELIVERY_RECEIVED) {
            Out.println("\n  Плановая дата доставки: " + order.getDeliveryDate() + "; сегодня: " + LocalDate.now());
            if (!io.askYesNo("  Дата доставки наступила, загружаем товар?")) {
                Out.println("  Загрузка отложена до даты доставки.");
                return;
            }
        } else if (!io.askYesNo("\n  Выполнить задачу «" + label + "»?")) {
            return;
        }
        store.runLockedVoid(s -> step(s, orderId, status));
    }

    private void step(ShopState state, int orderId, OrderStatus status) {
        Optional<Order> found = state.orderInStatus(orderId, status);
        if (found.isEmpty()) {
            return;
        }
        Order order = found.get();
        Delivery delivery = state.delivery(order.getDeliveryId());
        Client client = state.client(order.getClientId());
        switch (status) {
            case DELIVERY_RECEIVED:
                order.loadGoods();
                break;
            case DELIVERY_LOADED:
                delivery.deliverGoods(order, client.getAddress());
                break;
            case ACCEPTED:
                delivery.recordDelivery(order);
                state.warehouse().writeOff(order);
                order.setStatus(OrderStatus.DONE);
                break;
            case DECLINED:
                delivery.cancel(order);
                break;
            case RETURNING:
                order.returnGoodsToWarehouse();
                break;
            default:
                break;
        }
    }
}
