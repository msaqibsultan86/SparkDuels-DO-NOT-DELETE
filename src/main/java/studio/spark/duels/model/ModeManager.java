package studio.spark.duels.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import studio.spark.duels.SparkDuels;

import java.util.*;

public class ModeManager {

    private final SparkDuels plugin;
    private final Map<String, Mode> modes = new LinkedHashMap<>();

    public ModeManager(SparkDuels plugin) {
        this.plugin = plugin;
        load();
    }

    public Collection<Mode> all() { return modes.values(); }

    public Mode get(String id) {
        if (id == null) return null;
        return modes.get(id.toLowerCase(Locale.ROOT));
    }

    public List<Mode> duelModes() {
        List<Mode> list = new ArrayList<>();
        for (Mode m : modes.values()) if (m.duel()) list.add(m);
        list.sort(Comparator.comparingInt(Mode::slot));
        return list;
    }

    public void load() {
        modes.clear();
        ConfigurationSection root = plugin.modesConfig().getConfigurationSection("modes");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            String display = s.getString("display", id);
            Material icon = Material.matchMaterial(s.getString("icon", "IRON_SWORD").toUpperCase(Locale.ROOT));
            if (icon == null) icon = Material.IRON_SWORD;
            String kit = s.getString("kit", id + "Kit");
            Mode.Type type;
            try { type = Mode.Type.valueOf(s.getString("type", "NORMAL").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { type = Mode.Type.NORMAL; }
            int slot = s.getInt("slot", 0);
            boolean duel = s.getBoolean("duel", true);
            boolean ffa = s.getBoolean("ffa", false);
            modes.put(id.toLowerCase(Locale.ROOT), new Mode(id, display, icon, kit, type, slot, duel, ffa));
        }
    }
}
