package studio.spark.duels.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.stats.PlayerStats;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** View-only top-players GUI (player heads). */
public class LeaderboardMenu implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(SparkDuels plugin, Player viewer, boolean ffa) {
        LeaderboardMenu menu = new LeaderboardMenu();
        String title = ffa ? "<gradient:#FFD23C:#FF9E2C><bold>\u2620 ꜰꜰᴀ ᴛᴏᴘ</bold></gradient>"
                           : "<gradient:#3CFF7E:#11A65B><bold>\u2694 ᴅᴜᴇʟ ᴛᴏᴘ</bold></gradient>";
        Inventory inv = Bukkit.createInventory(menu, 54, plugin.msg().parse(title));
        menu.inventory = inv;

        int slot = 0, rank = 1;
        for (Map.Entry<UUID, PlayerStats> e : plugin.stats().top(45, ffa)) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            PlayerStats s = e.getValue();
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta sm) {
                sm.setOwningPlayer(op);
                sm.displayName(plugin.msg().item("<#3CFF7E>#" + rank + " <white>" + (op.getName() == null ? "?" : op.getName())));
                if (ffa) {
                    sm.lore(List.of(
                            plugin.msg().item("<gray>ᴋɪʟʟs: <white>" + s.ffaKills),
                            plugin.msg().item("<gray>ʙᴇsᴛ sᴛʀᴇᴀᴋ: <white>" + s.ffaBestStreak)));
                } else {
                    sm.lore(List.of(
                            plugin.msg().item("<gray>ᴡɪɴs: <white>" + s.wins),
                            plugin.msg().item("<gray>ᴇʟᴏ: <white>" + s.elo),
                            plugin.msg().item("<gray>ᴡɪɴʀᴀᴛᴇ: <white>" + s.winrate() + "%")));
                }
                head.setItemMeta(sm);
            }
            inv.setItem(slot++, head);
            rank++;
            if (slot >= 54) break;
        }
        viewer.openInventory(inv);
        plugin.sounds().play(viewer, "gui-open");
    }
}
