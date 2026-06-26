package studio.spark.duels.kit;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import studio.spark.duels.SparkDuels;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reverse of the kit loader: writes ItemStacks back into kits.yml. */
public class KitSerializer {

    private final SparkDuels plugin;

    public KitSerializer(SparkDuels plugin) { this.plugin = plugin; }

    private Map<String, Object> item(ItemStack is) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", is.getType().name());
        if (is.getAmount() != 1) m.put("amount", is.getAmount());
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            if (!meta.getEnchants().isEmpty()) {
                Map<String, Object> ench = new LinkedHashMap<>();
                for (Map.Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()) {
                    ench.put(e.getKey().getKey().getKey(), e.getValue());
                }
                m.put("enchants", ench);
            }
            if (meta instanceof PotionMeta pm && pm.getBasePotionType() != null) {
                m.put("potion", pm.getBasePotionType().name());
            }
            if (meta.hasDisplayName()) {
                m.put("name", PlainTextComponentSerializer.plainText().serialize(meta.displayName()));
            }
        }
        return m;
    }

    /** Write a kit definition from inventory contents and save kits.yml. */
    public void writeKit(String name, Material icon, ItemStack helmet, ItemStack chest,
                         ItemStack legs, ItemStack boots, ItemStack offhand, ItemStack[] storage) {
        ConfigurationSection root = plugin.kitsConfig().getConfigurationSection("kits");
        if (root == null) root = plugin.kitsConfig().createSection("kits");
        root.set(name, null); // clear any previous definition
        ConfigurationSection k = root.createSection(name);
        k.set("icon", icon == null ? "IRON_SWORD" : icon.name());

        if (helmet != null) k.set("helmet", item(helmet));
        if (chest != null) k.set("chestplate", item(chest));
        if (legs != null) k.set("leggings", item(legs));
        if (boots != null) k.set("boots", item(boots));
        if (offhand != null) k.set("offhand", item(offhand));

        List<Map<String, Object>> items = new ArrayList<>();
        if (storage != null) {
            for (int slot = 0; slot < storage.length && slot < 36; slot++) {
                ItemStack is = storage[slot];
                if (is == null || is.getType() == Material.AIR) continue;
                Map<String, Object> entry = item(is);
                entry.put("slot", slot);
                items.add(entry);
            }
        }
        k.set("items", items);
        save();
        plugin.kits().load();
    }

    public boolean deleteKit(String name) {
        ConfigurationSection root = plugin.kitsConfig().getConfigurationSection("kits");
        if (root == null || !root.contains(name)) return false;
        root.set(name, null);
        save();
        plugin.kits().load();
        return true;
    }

    private void save() {
        try {
            ((org.bukkit.configuration.file.YamlConfiguration) plugin.kitsConfig())
                    .save(new File(plugin.getDataFolder(), "kits.yml"));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save kits.yml: " + e.getMessage());
        }
    }
}
