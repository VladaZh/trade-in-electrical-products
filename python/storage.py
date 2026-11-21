import json
import os
import time

from data import make_data

LOCK_TIMEOUT = 5.0


def state_path():
    return os.environ.get("DATA_FILE", os.path.join("data", "state.json"))


def convert_keys(data):
    for name in ("clients", "products", "orders", "shipments", "deliveries"):
        data[name] = {int(k): v for k, v in data[name].items()}
    return data


def read_state(path):
    for _ in range(20):
        try:
            with open(path, encoding="utf-8") as f:
                return convert_keys(json.load(f))
        except (OSError, json.JSONDecodeError):
            time.sleep(0.05)
    with open(path, encoding="utf-8") as f:
        return convert_keys(json.load(f))


def write_state(path, data):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    os.replace(tmp, path)


def acquire_lock(path):
    lock = path + ".lock"
    started = time.time()
    while True:
        try:
            fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            os.close(fd)
            return lock
        except FileExistsError:
            if time.time() - started > LOCK_TIMEOUT:
                try:
                    os.remove(lock)
                except FileNotFoundError:
                    pass
                started = time.time()
            time.sleep(0.05)


def release_lock(lock):
    try:
        os.remove(lock)
    except FileNotFoundError:
        pass


def init_state(path, force=False):
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    lock = acquire_lock(path)
    try:
        if force or not os.path.exists(path):
            write_state(path, make_data())
    finally:
        release_lock(lock)


def run_locked(path, action, *args):
    lock = acquire_lock(path)
    try:
        data = read_state(path)
        result = action(data, *args)
        write_state(path, data)
        return result
    finally:
        release_lock(lock)
