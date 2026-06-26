package studio.spark.duels.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.InventoryHolder;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.gui.DuelMenu;

public class ProtectionListener implements Listener {

    private final SparkDuels plugin;

    public ProtectionListener(SparkDuels plugin) { this.plugin = plugin; }

    private boolean lobby(Player p) { return !plugin.matches().isLive(p); }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (lobby(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (lobby(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && lobby(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (lobby(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.lobby().inRegion(b.getLocation()));
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.lobby().inRegion(b.getLocation()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (plugin.matches().isLive(p)) return;
        InventoryHolder holder = e.getView().getTopInventory().getHolder();
        if (holder instanceof DuelMenu) return; // handled by MenuListener
        e.setCancelled(true);
    }
}
