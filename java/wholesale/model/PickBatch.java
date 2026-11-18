package wholesale.model;

import java.util.Collections;
import java.util.List;

public class PickBatch {
    private final List<Pick> picks;
    private final boolean complete;

    PickBatch(List<Pick> picks, boolean complete) {
        this.picks = picks;
        this.complete = complete;
    }

    public List<Pick> getPicks() {
        return Collections.unmodifiableList(picks);
    }

    public boolean isComplete() {
        return complete;
    }
}
