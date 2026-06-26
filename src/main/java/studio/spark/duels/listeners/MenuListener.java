package studio.spark.duels.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.gui.DuelMenu;
import studio.spark.duels.gui.PartyMenu;
import studio.spark.duels.util.Ctx;

public class MenuListener implements Listener {

    private final SparkDuels plugin;

    public MenuListener(SparkDuels plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (holder instanceof DuelMenu menu) {
            e.setCancelled(true);
            String modeId = menu.slotMode().get(e.getRawSlot());
            if (modeId == null) return;
            plugin.sounds().play(p, "gui-click");
            p.closeInventory();
            Player target = plugin.getServer().getPlayer(menu.target());
            if (target == null) {
                plugin.msg().send(p, "general.player-not-found", Ctx.of().put("target", "?"));
                return;
            }
            plugin.duels().send(p, target, modeId);
            return;
        }

        if (holder instanceof PartyMenu menu) {
            e.setCancelled(true);
            String action = menu.actions().get(e.getRawSlot());
            if (action == null) return;
            plugin.sounds().play(p, "gui-click");
            p.closeInventory();
            if (action.equals("leave")) plugin.parties().leave(p);
            else if (action.equals("disband")) plugin.parties().disband(p);
            else if (action.startsWith("duel:")) plugin.parties().startDuel(p, action.substring(5));
            else if (action.startsWith("ffa:")) plugin.parties().startFfa(p, action.substring(4));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p && plugin.kitEditor().isEditing(p)) {
            plugin.kitEditor().save(p);
        }
    }
}
