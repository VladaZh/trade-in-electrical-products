package wholesale.console;

import java.util.ArrayList;
import java.util.List;
import wholesale.check.OrderChecker;
import wholesale.model.Order;
import wholesale.model.OrderStatus;
import wholesale.model.ShopState;
import wholesale.storage.StateStore;
import wholesale.util.Out;

public class SalesConsole extends RoleConsole {
    private final OrderChecker checker = OrderChecker.standard();

    public SalesConsole(StateStore store, ConsoleIO io) {
        super(store, io);
    }

    @Override
    protected String title() {
        return "ОТДЕЛ ПРОДАЖ";
    }

    @Override
    protected List<Task> tasks(ShopState data) {
        List<Task> tasks = new ArrayList<>();
        for (Order order : data.orders().values()) {
            if (order.getStatus() == OrderStatus.SALES_CHECK) {
                tasks.add(new Task(order.getOrderId(), "Проверка корректности поступившего заказа"));
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
        List<String> hints = checker.check(order, data.clients(), data.itemsOf(orderId), data.products());
        if (!hints.isEmpty()) {
            Out.println("  Подсказка системы: " + String.join("; ", hints));
        }
        if (io.askYesNo("\n  Данные заказа корректны?")) {
            store.runLockedVoid(s -> s.orderInStatus(orderId, OrderStatus.SALES_CHECK).ifPresent(o -> {
                o.confirmData();
                o.sendInfoToWarehouse();
            }));
        } else {
            String text = io.askNonEmpty("  Что нужно уточнить у клиента: ");
            store.runLockedVoid(s -> s.orderInStatus(orderId, OrderStatus.SALES_CHECK)
                    .ifPresent(o -> o.clarifyWithClient(text)));
        }
    }
}
