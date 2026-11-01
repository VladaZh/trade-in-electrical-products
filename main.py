import sys

from client import run_client_console
from delivery import run_delivery_console
from sales_department import run_sales_console
from storage import init_state, state_path
from warehouse import run_warehouse_console

USAGE = """Использование: python -m system.main <роль>
  client     - консоль клиента
  sales      - консоль отдела продаж
  warehouse  - консоль склада
  delivery   - консоль отдела доставки
  reset      - очистить общие данные (начать заново)"""


def main(argv):
    role = argv[1] if len(argv) > 1 else None
    path = state_path()
    if role == "reset":
        init_state(path, force=True)
        print(f"Данные сброшены ({path}).")
        return 0
    consoles = {
        "client": run_client_console,
        "sales": run_sales_console,
        "warehouse": run_warehouse_console,
        "delivery": run_delivery_console,
    }
    if role not in consoles:
        print(USAGE)
        return 1
    init_state(path)
    try:
        return consoles[role](path)
    except (EOFError, KeyboardInterrupt):
        print("\nВвод прерван, выход.")
        return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
