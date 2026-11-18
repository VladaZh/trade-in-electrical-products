package wholesale.console;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import wholesale.model.AvailabilityResult;
import wholesale.model.Client;
import wholesale.model.Message;
import wholesale.model.Order;
import wholesale.model.OrderStatus;
import wholesale.model.Product;
import wholesale.model.ShopState;
import wholesale.storage.StateStore;
import wholesale.util.Formats;
import wholesale.util.Out;

public class ClientConsole extends RoleConsole {
    private static final Map<OrderStatus, String> CLIENT_TASKS = new LinkedHashMap<>();

    static {
        CLIENT_TASKS.put(OrderStatus.CLARIFY, "Скорректировать данные (запрос отдела продаж)");
        CLIENT_TASKS.put(OrderStatus.DELIVERY_DATE, "Согласовать дату доставки");
        CLIENT_TASKS.put(OrderStatus.DELIVERED, "Принять или отклонить доставленный заказ");
    }

    private int clientId;
    private String clientName;

    public ClientConsole(StateStore store, ConsoleIO io) {
        super(store, io);
    }

    @Override
    public int run() {
        clientId = chooseClient();
        clientName = store.read().client(clientId).getName();
        return super.run();
    }

    @Override
    protected String title() {
        return "КЛИЕНТ: " + clientName;
    }

    @Override
    protected List<Task> tasks(ShopState data) {
        List<Task> tasks = new ArrayList<>();
        for (Order order : data.orders().values()) {
            if (order.getClientId() == clientId) {
                String label = CLIENT_TASKS.get(order.getStatus());
                if (label != null) {
                    tasks.add(new Task(order.getOrderId(), label));
                }
            }
        }
        return tasks;
    }

    @Override
    protected List<Extra> extras() {
        return List.of(
                new Extra("n", "новый заказ", this::newOrder),
                new Extra("o", "мои заказы", () -> Views.showAllOrders(store.read(), clientId)));
    }

    @Override
    protected void onRefresh() {
        ShopState data = store.read();
        if (data.hasUnread(clientId)) {
            store.runLockedVoid(this::readMessages);
        }
        showOrderStatuses(data);
    }

    @Override
    protected void perform(int orderId) {
        ShopState data = store.read();
        Order order = data.order(orderId);
        Views.showOrder(data, order);
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.CLARIFY) {
            Out.println("\n  Отдел продаж просит уточнить: " + order.getClarification());
            Map<String, String> correction = new LinkedHashMap<>();
            String phone = io.askText("  Новый телефон (Enter - не менять): ");
            String address = io.askText("  Новый адрес (Enter - не менять): ");
            if (!phone.isEmpty()) {
                correction.put("phone", phone);
            }
            if (!address.isEmpty()) {
                correction.put("address", address);
            }
            store.runLockedVoid(s -> correctData(s, orderId, correction));
        } else if (status == OrderStatus.DELIVERY_DATE) {
            Out.println("\n  Заказ готов к отгрузке. Согласуйте дату доставки.");
            String date = io.askDate("  Желаемая дата доставки (ГГГГ-ММ-ДД): ", LocalDate.now());
            store.runLockedVoid(s -> agreeDate(s, orderId, date));
        } else if (status == OrderStatus.DELIVERED) {
            boolean accepted = io.askYesNo("\n  Товар доставлен. Вы принимаете заказ?");
            store.runLockedVoid(s -> decide(s, orderId, accepted));
        }
    }


    private int chooseClient() {
        ShopState data = store.read();
        Out.println("\n  Кто вы?");
        for (Client c : data.clients().values()) {
            Out.println("    " + c.getClientId() + ". " + c.getName()
                    + "  тел.: " + Views.orDefault(c.getPhone(), "-")
                    + "  адрес: " + Views.orDefault(c.getAddress(), "-"));
        }
        Out.println("    0. Я новый клиент");
        Set<Integer> ids = new HashSet<>(data.clients().keySet());
        ids.add(0);
        int choice = io.askFromIds("Выберите номер: ", ids);
        if (choice != 0) {
            return choice;
        }
        String name = io.askNonEmpty("Название клиента: ");
        String phone = io.askText("Телефон (можно пропустить): ");
        String address = io.askText("Адрес (можно пропустить): ");
        int id = store.runLocked(s -> s.addClient(name, phone, address));
        Out.println("Вы зарегистрированы под номером " + id + ".");
        return id;
    }


    private void newOrder() {
        ShopState data = store.read();
        Out.println("\n--- Новый заказ ---");
        Views.showProducts(data);
        Map<Integer, Long> lines = inputLines(data.products());
        String deliveryType = chooseDeliveryType();
        Out.println("\n  Ваш заказ:");
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Long> line : lines.entrySet()) {
            Product product = data.products().get(line.getKey());
            BigDecimal cost = product.lineCost(line.getValue());
            total = total.add(cost);
            Out.println("    " + product.getName() + " x " + line.getValue() + " = " + Formats.money(cost) + " руб.");
        }
        Out.println("  Получение: " + deliveryType + "; итого: " + Formats.money(total) + " руб.");
        if (!io.askYesNo("Отправить заказ?")) {
            Out.println("Оформление отменено.");
            return;
        }
        Out.println();
        store.runLocked(s -> submitOrder(s, lines, deliveryType));
    }

    private Map<Integer, Long> inputLines(Map<Integer, Product> products) {
        Out.println("\nВведите товары: ID и количество через пробел. Пустая строка - закончить.");
        Map<Integer, Long> lines = new LinkedHashMap<>();
        while (true) {
            String text = io.readLine("Товар> ").strip();
            if (text.isEmpty()) {
                if (!lines.isEmpty()) {
                    return lines;
                }
                Out.println("Добавьте хотя бы одну позицию.");
                continue;
            }
            long[] parsed = parseLine(text, products);
            if (parsed == null) {
                continue;
            }
            int pid = (int) parsed[0];
            lines.merge(pid, parsed[1], Long::sum);
            Out.println("  В заказе: " + products.get(pid).getName() + " - " + lines.get(pid) + " шт.");
        }
    }

    private long[] parseLine(String text, Map<Integer, Product> products) {
        String[] parts = text.split("(?U)\\s+");
        if (parts.length != 2 || !ConsoleIO.isDecimal(parts[0]) || !ConsoleIO.isDecimal(parts[1])) {
            Out.println("Нужно два числа: ID товара и количество, например: 101 25");
            return null;
        }
        long pid;
        long qty;
        try {
            pid = Long.parseLong(parts[0]);
            qty = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            Out.println("Нужно два числа: ID товара и количество, например: 101 25");
            return null;
        }
        if (pid > Integer.MAX_VALUE || !products.containsKey((int) pid)) {
            Out.println("Товара с ID " + pid + " нет в списке.");
            return null;
        }
        if (qty <= 0) {
            Out.println("Количество должно быть больше нуля.");
            return null;
        }
        return new long[] {pid, qty};
    }

    private String chooseDeliveryType() {
        Out.println("\n  Способ получения:\n    1. Самовывоз\n    2. Доставка");
        return io.askInt("Выберите способ (1-2): ", 1, 2) == 1 ? Order.PICKUP : Order.HOME_DELIVERY;
    }


    private Integer submitOrder(ShopState state, Map<Integer, Long> lines, String deliveryType) {
        Client client = state.client(clientId);
        AvailabilityResult result = state.freeStock().checkAvailability(lines);
        if (!result.isOk()) {
            client.rejectOrder(result);
            result.showMaxAvailable(state.products());
            Out.println("Создание заказа отменено.");
            return null;
        }
        Order order = state.createOrder(clientId, lines, deliveryType);
        order.passInfoToSales();
        return order.getOrderId();
    }

    private void correctData(ShopState state, int orderId, Map<String, String> correction) {
        state.orderInStatus(orderId, OrderStatus.CLARIFY).ifPresent(order -> {
            state.client(order.getClientId()).applyCorrection(correction.get("phone"), correction.get("address"));
            order.resubmitAfterCorrection(correction);
        });
    }

    private void agreeDate(ShopState state, int orderId, String date) {
        state.orderInStatus(orderId, OrderStatus.DELIVERY_DATE).ifPresent(order -> {
            order.agreeDeliveryDate(state.client(order.getClientId()), date);
            order.passInfoToDelivery();
            state.receiveDeliveryInfo(order, date);
            order.setStatus(OrderStatus.DELIVERY_RECEIVED);
        });
    }

    private void decide(ShopState state, int orderId, boolean accepted) {
        state.orderInStatus(orderId, OrderStatus.DELIVERED).ifPresent(order -> order.decide(accepted));
    }

    private void readMessages(ShopState state) {
        for (Message m : state.popMessages(clientId)) {
            Out.println("\n(" + m.getTime() + "), заказ №" + m.getOrderId() + ":");
            Out.println("      " + m.getText());
        }
    }

    private void showOrderStatuses(ShopState data) {
        List<Order> mine = new ArrayList<>();
        for (Order order : data.orders().values()) {
            if (order.getClientId() == clientId) {
                mine.add(order);
            }
        }
        if (mine.isEmpty()) {
            return;
        }
        Out.println("\n  Статусы ваших заказов:");
        for (Order o : mine) {
            Out.println("    №" + Formats.padRight(String.valueOf(o.getOrderId()), 3)
                    + " " + Formats.padRight(o.deliveryType(), 10) + " " + o.getStatus().getLabel());
        }
    }
}
