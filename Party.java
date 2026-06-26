package studio.spark.duels.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.model.Mode;
import studio.spark.duels.party.Party;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Interactive party menu. Actions routed by MenuListener. */
public class PartyMenu implements InventoryHolder {

    private final Map<Integer, String> actions = new HashMap<>();
    private Inventory inventory;

    @Override
    public Inventory getInventory() { return inventory; }
    public Map<Integer, String> actions() { return actions; }

    public static void open(SparkDuels plugin, Player viewer) {
        Party party = plugin.parties().getParty(viewer);
        if (party == null) { plugin.msg().send(viewer, "party.not-in-party"); return; }

        PartyMenu menu = new PartyMenu();
        Inventory inv = Bukkit.createInventory(menu, 27,
                plugin.msg().parse("<gradient:#3CFF7E:#11A65B><bold>ᴘᴀʀᴛʏ</bold></gradient>"));
        menu.inventory = inv;

        int slot = 0;
        for (UUID id : party.members()) {
            if (slot > 8) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta sm) {
                sm.setOwningPlayer(op);
                sm.displayName(plugin.msg().item((party.isLeader(id) ? "<#3CFF7E>\u2605 " : "<white>")
                        + (op.getName() == null ? "?" : op.getName())));
                head.setItemMeta(sm);
            }
            inv.setItem(slot++, head);
        }

        boolean leader = party.isLeader(viewer.getUniqueId());
        Mode duelMode = plugin.modes().duelModes().stream().findFirst().orElse(null);
        Mode ffaMode = plugin.modes().all().stream().filter(Mode::ffa).findFirst().orElse(null);

        if (leader && duelMode != null) menu.button(inv, 11, Material.DIAMOND_SWORD,
                "<#3CFF7E>sᴛᴀʀᴛ ᴘᴀʀᴛʏ ᴅᴜᴇʟ", "duel:" + duelMode.id(), plugin);
        if (leader && ffaMode != null) menu.button(inv, 13, Material.NETHERITE_AXE,
                "<#FFD23C>sᴛᴀʀᴛ ᴘᴀʀᴛʏ ꜰꜰᴀ", "ffa:" + ffaMode.id(), plugin);
        if (leader) menu.button(inv, 15, Material.BARRIER, "<red>ᴅɪsʙᴀɴᴅ", "disband", plugin);
        menu.button(inv, 22, Material.RED_DYE, "<gray>ʟᴇᴀᴠᴇ ᴘᴀʀᴛʏ", "leave", plugin);

        viewer.openInventory(inv);
        plugin.sounds().play(viewer, "gui-open");
    }

    private void button(Inventory inv, int slot, Material mat, String name, String action, SparkDuels plugin) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(plugin.msg().item(name));
        it.setItemMeta(meta);
        inv.setItem(slot, it);
        actions.put(slot, action);
    }
}
