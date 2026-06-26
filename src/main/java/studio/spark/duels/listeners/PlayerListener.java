package studio.spark.duels.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import studio.spark.duels.SparkDuels;

public class PlayerListener implements Listener {

    private final SparkDuels plugin;

    public PlayerListener(SparkDuels plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.lobby().toLobby(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var p = e.getPlayer();
        if (plugin.matches().isInMatch(p) && plugin.getConfig().getBoolean("match.combat-log-protection", true)) {
            plugin.matches().handleLeave(p);
        }
        plugin.queue().leaveSilent(p);
        plugin.ffa().leaveSilent(p);
        if (plugin.parties().inParty(p)) plugin.parties().leave(p);
        plugin.boards().remove(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (plugin.lobby().hasSpawn()) e.setRespawnLocation(plugin.lobby().getSpawn());
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.lobby().toLobby(e.getPlayer()));
    }
}
