package studio.spark.duels.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.model.Mode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mode picker that joins an FFA arena. */
public class FFAMenu implements InventoryHolder {

    private final Map<Integer, String> slotMode = new HashMap<>();
    private Inventory inventory;

    public Map<Integer, String> slotMode() { return slotMode; }

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(SparkDuels plugin, Player player) {
        FFAMenu menu = new FFAMenu();
        Inventory inv = Bukkit.createInventory(menu, 27,
                plugin.msg().parse("<gradient:#FFD23C:#FF9E2C><bold>\u2620 ꜰꜰᴀ ᴍᴏᴅᴇs</bold></gradient>"));
        menu.inventory = inv;

        int fallback = 0;
        for (Mode m : plugin.modes().all()) {
            if (!m.ffa()) continue;
            int slot = m.slot();
            if (slot < 0 || slot >= 27) slot = fallback;
            if (inv.getItem(slot) != null) slot = fallback;
            ItemStack icon = new ItemStack(m.icon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(plugin.msg().item(m.display()));
            int live = plugin.ffa().arenaFor(m.id()) != null
                    ? plugin.ffa().playersIn(plugin.ffa().arenaFor(m.id()).name()) : 0;
            meta.lore(List.of(
                    plugin.msg().item("<gray>ɪɴ ɢᴀᴍᴇ: <white>" + live),
                    plugin.msg().item(""),
                    plugin.msg().item("<#FFD23C>\u25B6 ᴄʟɪᴄᴋ ᴛᴏ ᴊᴏɪɴ")));
            icon.setItemMeta(meta);
            inv.setItem(slot, icon);
            menu.slotMode.put(slot, m.id());
            fallback++;
        }
        player.openInventory(inv);
        plugin.sounds().play(player, "gui-open");
    }
}
