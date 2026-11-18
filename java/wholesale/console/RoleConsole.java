package wholesale.console;

import java.util.List;
import java.util.Locale;
import wholesale.model.Order;
import wholesale.model.ShopState;
import wholesale.storage.StateStore;
import wholesale.util.Out;

public abstract class RoleConsole {
    protected final StateStore store;
    protected final ConsoleIO io;

    protected RoleConsole(StateStore store, ConsoleIO io) {
        this.store = store;
        this.io = io;
    }

    protected abstract String title();

    protected abstract List<Task> tasks(ShopState data);

    protected abstract void perform(int orderId);

    protected List<Extra> extras() {
        return List.of();
    }

    protected void onRefresh() {
    }

    public int run() {
        while (true) {
            onRefresh();
            ShopState data = store.read();
            Out.println("\n=========== " + title() + " ===========");
            List<Task> tasks = tasks(data);
            if (!tasks.isEmpty()) {
                Out.println("Задачи, ожидающие вашего решения:");
                int number = 1;
                for (Task task : tasks) {
                    Order order = data.order(task.orderId());
                    String client = data.client(order.getClientId()).getName();
                    Out.println("  " + number + ". Заказ №" + task.orderId() + " (" + client + "): " + task.label());
                    number++;
                }
            } else {
                Out.println("Сейчас задач нет. Нажмите Enter, чтобы обновить список.");
            }
            String text = askChoice(tasks.size());
            if (text.equals("0")) {
                Out.println("Выход.");
                return 0;
            }
            if (text.isEmpty()) {
                continue;
            }
            Extra extra = findExtra(text);
            if (extra != null) {
                extra.action().run();
            } else {
                perform(tasks.get(Integer.parseInt(text) - 1).orderId());
            }
        }
    }

    private String askChoice(int count) {
        StringBuilder keys = new StringBuilder();
        for (Extra e : extras()) {
            if (keys.length() > 0) {
                keys.append(", ");
            }
            keys.append(e.key()).append(" - ").append(e.label());
        }
        String hint = count > 0 ? "номер задачи" : "Enter";
        String keysPart = keys.length() > 0 ? keys + "; " : "";
        while (true) {
            String text = io.readLine("\nВыбор (" + hint + "; " + keysPart + "Enter - обновить; 0 - выход): ")
                    .strip().toLowerCase(Locale.ROOT);
            if (text.isEmpty() || text.equals("0") || findExtra(text) != null) {
                return text;
            }
            if (ConsoleIO.isDecimal(text) && text.length() < 10) {
                int n = Integer.parseInt(text);
                if (n >= 1 && n <= count) {
                    return text;
                }
            }
            Out.println("Нет такой команды.");
        }
    }

    private Extra findExtra(String key) {
        for (Extra e : extras()) {
            if (e.key().equals(key)) {
                return e;
            }
        }
        return null;
    }
}
