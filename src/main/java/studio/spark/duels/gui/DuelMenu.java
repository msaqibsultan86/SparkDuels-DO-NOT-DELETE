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
import java.util.UUID;

/** Mode-selection menu shown to the duel sender. */
public class DuelMenu implements InventoryHolder {

    private final UUID target;
    private final Map<Integer, String> slotMode = new HashMap<>();
    private Inventory inventory;

    private DuelMenu(UUID target) { this.target = target; }

    public UUID target() { return target; }
    public Map<Integer, String> slotMode() { return slotMode; }

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(SparkDuels plugin, Player sender, Player target) {
        DuelMenu menu = new DuelMenu(target.getUniqueId());
        Inventory inv = Bukkit.createInventory(menu, 27,
                plugin.msg().parse(plugin.msg().raw("duel.menu-title")));
        menu.inventory = inv;

        List<Mode> modes = plugin.modes().duelModes();
        int fallback = 0;
        for (Mode m : modes) {
            int slot = m.slot();
            if (slot < 0 || slot >= 27) slot = fallback;
            ItemStack icon = new ItemStack(m.icon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(plugin.msg().item(m.display()));
            meta.lore(List.of(
                    plugin.msg().item("<gray>ᴋɪᴛ: <white>" + m.kit()),
                    plugin.msg().item(""),
                    plugin.msg().item("<#3CFF7E>\u25B6 ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀʟʟᴇɴɢᴇ")));
            icon.setItemMeta(meta);
            inv.setItem(slot, icon);
            menu.slotMode.put(slot, m.id());
            fallback++;
        }
        sender.openInventory(inv);
        plugin.sounds().play(sender, "gui-open");
    }
}
