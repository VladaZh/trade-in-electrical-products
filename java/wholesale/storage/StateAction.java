package wholesale.storage;

import wholesale.model.ShopState;

@FunctionalInterface
public interface StateAction<R> {
    R apply(ShopState state);
}
