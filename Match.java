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

/** Mode picker that joins the matchmaking queue (ranked or unranked). */
public class QueueMenu implements InventoryHolder {

    private final boolean ranked;
    private final Map<Integer, String> slotMode = new HashMap<>();
    private Inventory inventory;

    private QueueMenu(boolean ranked) { this.ranked = ranked; }

    public boolean ranked() { return ranked; }
    public Map<Integer, String> slotMode() { return slotMode; }

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(SparkDuels plugin, Player player, boolean ranked) {
        QueueMenu menu = new QueueMenu(ranked);
        String title = ranked
                ? "<gradient:#FFD23C:#FF9E2C><bold>\u2b50 ʀᴀɴᴋᴇᴅ ǫᴜᴇᴜᴇ</bold></gradient>"
                : "<gradient:#3CFF7E:#11A65B><bold>\u2694 ᴜɴʀᴀɴᴋᴇᴅ ǫᴜᴇᴜᴇ</bold></gradient>";
        Inventory inv = Bukkit.createInventory(menu, 27, plugin.msg().parse(title));
        menu.inventory = inv;

        int fallback = 0;
        for (Mode m : plugin.modes().duelModes()) {
            int slot = m.slot();
            if (slot < 0 || slot >= 27) slot = fallback;
            ItemStack icon = new ItemStack(m.icon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(plugin.msg().item(m.display()));
            meta.lore(List.of(
                    plugin.msg().item("<gray>ᴋɪᴛ: <white>" + m.kit()),
                    plugin.msg().item(""),
                    plugin.msg().item((ranked ? "<#FFD23C>" : "<#3CFF7E>") + "\u25B6 ᴄʟɪᴄᴋ ᴛᴏ ǫᴜᴇᴜᴇ")));
            icon.setItemMeta(meta);
            inv.setItem(slot, icon);
            menu.slotMode.put(slot, m.id());
            fallback++;
        }
        player.openInventory(inv);
        plugin.sounds().play(player, "gui-open");
    }
}
