import sys

from src.console import (
    ask_int,
    make_console_io,
    read_order_request,
    show_menu,
    show_orders,
    show_stock_lots,
)
from src.data import make_data
from src.process import process_order


def print_result(result, lots):
    print(f"\n  >>> Конечное событие: {result}")
    print("  Остатки: " + ", ".join(f"партия {l['lot_id']}={l['qty']}" for l in lots))


def run_interactive():
    data = make_data()
    while True:
        show_menu()
        choice = ask_int("Ваш выбор: ", 0, 3)
        if choice == 0:
            print("До свидания!")
            return 0
        if choice == 1:
            request = read_order_request(data)
            if request is not None:
                client_id, lines, delivery_type = request
                result, lots = process_order(
                    data, client_id, lines, delivery_type, make_console_io()
                )
                data["stock_lots"] = lots
                print_result(result, lots)
        elif choice == 2:
            show_stock_lots(data["stock_lots"], data["products"])
        else:
            show_orders(data)


def main(argv):
    try:
        return run_interactive()
    except (EOFError, KeyboardInterrupt):
        print("\nВвод прерван, выход.")
        return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
