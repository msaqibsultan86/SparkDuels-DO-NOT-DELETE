package studio.spark.duels.kit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import studio.spark.duels.SparkDuels;

import java.util.*;

public class KitManager {

    private final SparkDuels plugin;
    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitManager(SparkDuels plugin) {
        this.plugin = plugin;
        load();
    }

    public Collection<Kit> all() { return kits.values(); }

    public Kit get(String name) {
        if (name == null) return null;
        return kits.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) { return get(name) != null; }

    public void load() {
        kits.clear();
        ConfigurationSection root = plugin.kitsConfig().getConfigurationSection("kits");
        if (root == null) return;
        for (String name : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(name);
            if (s == null) continue;
            Material icon = Material.matchMaterial(s.getString("icon", "IRON_SWORD").toUpperCase(Locale.ROOT));
            Kit kit = new Kit(name, icon);

            kit.helmet(plugin.items().build(s.getConfigurationSection("helmet")));
            kit.chestplate(plugin.items().build(s.getConfigurationSection("chestplate")));
            kit.leggings(plugin.items().build(s.getConfigurationSection("leggings")));
            kit.boots(plugin.items().build(s.getConfigurationSection("boots")));
            kit.offhand(plugin.items().build(s.getConfigurationSection("offhand")));

            // fixed-slot items first, then auto-fill the rest
            List<Map<?, ?>> items = s.getMapList("items");
            List<Map<?, ?>> auto = new ArrayList<>();
            for (Map<?, ?> map : items) {
                int slot = plugin.items().slotOf(map);
                ItemStack item = plugin.items().build(map);
                if (item == null) continue;
                if (slot >= 0) kit.setSlot(slot, item);
                else auto.add(map);
            }
            for (Map<?, ?> map : auto) {
                ItemStack item = plugin.items().build(map);
                if (item != null) kit.add(item);
            }
            kits.put(name.toLowerCase(Locale.ROOT), kit);
        }
        plugin.getLogger().info("Loaded " + kits.size() + " kits.");
    }
}
