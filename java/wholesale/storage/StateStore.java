package wholesale.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import wholesale.Config;
import wholesale.model.ShopState;

public class StateStore {
    private final Path path;

    public StateStore(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public ShopState read() {
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                return parse();
            } catch (IOException | JsonException e) {
                sleep(50);
            }
        }
        try {
            return parse();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать " + path + ": " + e.getMessage(), e);
        }
    }

    private ShopState parse() throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return ShopState.fromJson(Json.obj(Json.parse(text)));
    }

    public void write(ShopState state) {
        Path tmp = Path.of(path + ".tmp");
        try {
            Files.writeString(tmp, Json.write(state.toJson()), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось записать " + path + ": " + e.getMessage(), e);
        }
    }

    public void init(boolean force) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать каталог данных: " + e.getMessage(), e);
        }
        Path lock = acquireLock();
        try {
            if (force || !Files.exists(path)) {
                write(ShopState.initial());
            }
        } finally {
            releaseLock(lock);
        }
    }

    public <R> R runLocked(StateAction<R> action) {
        Path lock = acquireLock();
        try {
            ShopState state = read();
            R result = action.apply(state);
            write(state);
            return result;
        } finally {
            releaseLock(lock);
        }
    }

    public void runLockedVoid(Consumer<ShopState> action) {
        runLocked(state -> {
            action.accept(state);
            return null;
        });
    }

    private Path acquireLock() {
        Path lock = Path.of(path + ".lock");
        long started = System.currentTimeMillis();
        while (true) {
            try {
                Files.createFile(lock);
                return lock;
            } catch (FileAlreadyExistsException e) {
                if (System.currentTimeMillis() - started > Config.LOCK_TIMEOUT_MS) {
                    try {
                        Files.deleteIfExists(lock);
                    } catch (IOException ignored) {

                    }
                    started = System.currentTimeMillis();
                }
                sleep(50);
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось создать блокировку: " + e.getMessage(), e);
            }
        }
    }

    private void releaseLock(Path lock) {
        try {
            Files.deleteIfExists(lock);
        } catch (IOException ignored) {

        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
