package studio.spark.duels.util;

import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;

/** A small bag of extra placeholder values for a single render. */
public class Ctx {
    public final Map<String, String> values = new HashMap<>();

    public static Ctx of() { return new Ctx(); }

    public Ctx put(String key, Object value) {
        values.put(key, value == null ? "" : String.valueOf(value));
        return this;
    }

    public Ctx target(OfflinePlayer t) {
        if (t != null && t.getName() != null) values.put("target", t.getName());
        return this;
    }
}
