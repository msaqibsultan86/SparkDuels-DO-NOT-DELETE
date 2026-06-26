package studio.spark.duels.lobby;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import studio.spark.duels.SparkDuels;

import java.util.List;

/** Gives context-aware hotbar items: lobby / in-queue / in-party. */
public class LobbyItems {

    private final SparkDuels plugin;
    private final NamespacedKey actionKey;

    public LobbyItems(SparkDuels plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "action");
    }

    public String actionOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    public void give(Player p) {
        if (!plugin.getConfig().getBoolean("lobby.spawn-items", true)) return;
        if (plugin.matches().isInMatch(p) || plugin.ffa().isInFfa(p)) return;

        p.getInventory().clear();

        if (plugin.queue().isQueued(p)) {
            boolean ranked = plugin.queue().isRanked(p);
            set(p, 0, Material.CLOCK,
                    (ranked ? "<#FFD23C>" : "<#3CFF7E>") + "\u231b sᴇᴀʀᴄʜɪɴɢ ꜰᴏʀ ᴍᴀᴛᴄʜ",
                    List.of("<gray>ᴍᴏᴅᴇ: <white>%queue_mode%", "<gray>ɪɴ ǫᴜᴇᴜᴇ: <white>%queue_players%"), null);
            set(p, 8, Material.RED_DYE, "<red>\u2716 ʟᴇᴀᴠᴇ ǫᴜᴇᴜᴇ",
                    List.of("<gray>ʀɪɢʜᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴄᴀɴᴄᴇʟ"), "leave-queue");
        } else if (plugin.parties().inParty(p)) {
            set(p, 0, Material.DIAMOND_SWORD, "<#3CFF7E>\u2694 ᴘᴀʀᴛʏ ᴅᴜᴇʟ",
                    List.of("<gray>ᴘᴀɪʀ ʏᴏᴜʀ ᴘᴀʀᴛʏ ɪɴᴛᴏ 1ᴠ1s"), "p:duel");
            set(p, 1, Material.BLAZE_POWDER, "<#FFD23C>\u2620 ᴘᴀʀᴛʏ ꜰꜰᴀ",
                    List.of("<gray>sᴇɴᴅ ᴘᴀʀᴛʏ ɪɴᴛᴏ ꜰꜰᴀ"), "p:ffa");
            set(p, 4, Material.NETHER_STAR, "<#3CFF7E>\u2605 ᴘᴀʀᴛʏ ᴍᴇɴᴜ",
                    List.of("<gray>ᴍᴀɴᴀɢᴇ ʏᴏᴜʀ ᴘᴀʀᴛʏ"), "p:menu");
            set(p, 8, Material.RED_DYE, "<red>\u2716 ʟᴇᴀᴠᴇ ᴘᴀʀᴛʏ",
                    List.of("<gray>ʀɪɢʜᴛ-ᴄʟɪᴄᴋ ᴛᴏ ʟᴇᴀᴠᴇ"), "p:leave");
        } else {
            set(p, 0, Material.IRON_SWORD, "<gradient:#3CFF7E:#11A65B>\u2694 ᴜɴʀᴀɴᴋᴇᴅ</gradient>",
                    List.of("<gray>ᴄᴀsᴜᴀʟ ᴍᴀᴛᴄʜᴍᴀᴋɪɴɢ", "<#3CFF7E>\u25B6 ʀɪɢʜᴛ-ᴄʟɪᴄᴋ"), "q:u");
            set(p, 1, Material.DIAMOND_SWORD, "<gradient:#FFD23C:#FF9E2C>\u2b50 ʀᴀɴᴋᴇᴅ</gradient>",
                    List.of("<gray>ᴄᴏᴍᴘᴇᴛɪᴛɪᴠᴇ ᴇʟᴏ ᴍᴀᴛᴄʜᴇs", "<#FFD23C>\u25B6 ʀɪɢʜᴛ-ᴄʟɪᴄᴋ"), "q:r");
            set(p, 4, Material.BLAZE_POWDER, "<gradient:#FFD23C:#FF9E2C>\u2620 ꜰꜰᴀ</gradient>",
                    List.of("<gray>ꜰʀᴇᴇ-ꜰᴏʀ-ᴀʟʟ ᴀʀᴇɴᴀs", "<#FFD23C>\u25B6 ʀɪɢʜᴛ-ᴄʟɪᴄᴋ"), "ffa");
            set(p, 7, Material.NETHER_STAR, "<#C77DFF>\u2605 ᴘᴀʀᴛʏ",
                    List.of("<gray>ᴄʀᴇᴀᴛᴇ ᴀ ᴘᴀʀᴛʏ", "<#C77DFF>\u25B6 ʀɪɢʜᴛ-ᴄʟɪᴄᴋ"), "party");
            set(p, 8, Material.PAPER, "<gradient:#3CFF7E:#11A65B>\u2691 ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ</gradient>",
                    List.of("<gray>ᴛᴏᴘ ᴘʟᴀʏᴇʀs", "<#3CFF7E>\u25B6 ʀɪɢʜᴛ-ᴄʟɪᴄᴋ"), "top");
        }
        p.updateInventory();
    }

    private void set(Player p, int slot, Material mat, String name, List<String> lore, String action) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(plugin.msg().item(name));
        meta.lore(lore.stream().map(l -> plugin.msg().item(
                plugin.placeholders().apply(l, p, studio.spark.duels.util.Ctx.of()))).toList());
        if (action != null) meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        it.setItemMeta(meta);
        p.getInventory().setItem(slot, it);
    }
}
