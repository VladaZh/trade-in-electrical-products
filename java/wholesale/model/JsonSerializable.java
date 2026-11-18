package wholesale.model;

import java.util.Map;

public interface JsonSerializable {
    Map<String, Object> toJson();
}
