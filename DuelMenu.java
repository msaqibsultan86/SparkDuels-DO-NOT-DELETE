package studio.spark.duels.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import studio.spark.duels.SparkDuels;

import java.util.Locale;
import java.util.Map;

/** Builds ItemStacks from kit config sections. */
public class ItemFactory {

    private final SparkDuels plugin;

    public ItemFactory(SparkDuels plugin) {
        this.plugin = plugin;
    }

    public Material material(String name, Material fallback) {
        if (name == null) return fallback;
        Material m = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return m == null ? fallback : m;
    }

    private Enchantment enchant(String name) {
        try {
            return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
        } catch (Exception e) {
            return null;
        }
    }

    /** Build an item from a config section (type, amount, enchants, potion, name). */
    public ItemStack build(ConfigurationSection sec) {
        if (sec == null) return null;
        Material mat = material(sec.getString("type"), null);
        if (mat == null) return null;
        int amount = sec.getInt("amount", 1);
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        ConfigurationSection ench = sec.getConfigurationSection("enchants");
        if (ench != null) {
            for (String key : ench.getKeys(false)) {
                Enchantment e = enchant(key);
                if (e != null) meta.addEnchant(e, ench.getInt(key), true);
            }
        }

        String potion = sec.getString("potion");
        if (potion != null && meta instanceof PotionMeta pm) {
            try {
                pm.setBasePotionType(PotionType.valueOf(potion.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }

        String name = sec.getString("name");
        if (name != null) meta.displayName(plugin.msg().item(name));

        item.setItemMeta(meta);
        return item;
    }

    /** Build from a raw map (kit item lists come back as List<Map>). */
    @SuppressWarnings("unchecked")
    public ItemStack build(Map<?, ?> map) {
        if (map == null || map.get("type") == null) return null;
        Material mat = material(String.valueOf(map.get("type")), null);
        if (mat == null) return null;
        int amount = map.get("amount") instanceof Number num ? num.intValue() : 1;
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Object ench = map.get("enchants");
        if (ench instanceof Map<?, ?> em) {
            for (Map.Entry<?, ?> e : em.entrySet()) {
                Enchantment en = enchant(String.valueOf(e.getKey()));
                int lvl = e.getValue() instanceof Number num ? num.intValue() : 1;
                if (en != null) meta.addEnchant(en, lvl, true);
            }
        }
        Object potion = map.get("potion");
        if (potion != null && meta instanceof PotionMeta pm) {
            try { pm.setBasePotionType(PotionType.valueOf(String.valueOf(potion).toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ignored) {}
        }
        Object name = map.get("name");
        if (name != null) meta.displayName(plugin.msg().item(String.valueOf(name)));

        item.setItemMeta(meta);
        return item;
    }

    public int slotOf(Map<?, ?> map) {
        return map.get("slot") instanceof Number num ? num.intValue() : -1;
    }
}
