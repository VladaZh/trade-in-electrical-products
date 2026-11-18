package wholesale;

import java.nio.file.Path;
import wholesale.console.ClientConsole;
import wholesale.console.ConsoleIO;
import wholesale.console.DeliveryConsole;
import wholesale.console.EndOfInputException;
import wholesale.console.RoleConsole;
import wholesale.console.SalesConsole;
import wholesale.console.WarehouseConsole;
import wholesale.storage.StateStore;
import wholesale.util.Out;

public final class Main {
    private static final String USAGE = "Использование: java -cp out wholesale.Main <роль>\n"
            + "  client     - консоль клиента\n"
            + "  sales      - консоль отдела продаж\n"
            + "  warehouse  - консоль склада\n"
            + "  delivery   - консоль отдела доставки\n"
            + "  reset      - очистить общие данные (начать заново)";

    private Main() {
    }

    public static void main(String[] args) {
        int code;
        try {
            code = run(args);
        } finally {
            Out.flush();
        }
        System.exit(code);
    }

    static int run(String[] args) {
        String role = args.length > 0 ? args[0] : null;
        String pathText = System.getenv("DATA_FILE");
        if (pathText == null) {
            pathText = Path.of("data", "state.json").toString();
        }
        StateStore store = new StateStore(Path.of(pathText));
        if ("reset".equals(role)) {
            store.init(true);
            Out.println("Данные сброшены (" + pathText + ").");
            return 0;
        }
        ConsoleIO io = new ConsoleIO();
        RoleConsole console;
        if ("client".equals(role)) {
            console = new ClientConsole(store, io);
        } else if ("sales".equals(role)) {
            console = new SalesConsole(store, io);
        } else if ("warehouse".equals(role)) {
            console = new WarehouseConsole(store, io);
        } else if ("delivery".equals(role)) {
            console = new DeliveryConsole(store, io);
        } else {
            Out.println(USAGE);
            return 1;
        }
        store.init(false);
        try {
            return console.run();
        } catch (EndOfInputException e) {
            Out.println("\nВвод прерван, выход.");
            return 0;
        }
    }
}
