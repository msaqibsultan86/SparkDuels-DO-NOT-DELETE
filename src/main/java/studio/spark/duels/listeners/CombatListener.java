package studio.spark.duels.listeners;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.match.Match;
import studio.spark.duels.model.Mode;

import java.util.UUID;

public class CombatListener implements Listener {

    private final SparkDuels plugin;

    public CombatListener(SparkDuels plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        // ---- FFA ----
        if (plugin.ffa().isInFfa(p)) {
            double finalHealth = p.getHealth() - e.getFinalDamage();
            if (finalHealth <= 0.5) {
                e.setCancelled(true);
                plugin.ffa().handleDeath(p, resolveKiller(e));
            }
            return;
        }

        // ---- Duels ----
        Match m = plugin.matches().getMatch(p);
        boolean live = m != null && m.state() == Match.State.LIVE;
        if (!live) { e.setCancelled(true); return; }

        if (m.mode().type() == Mode.Type.SUMO) { e.setCancelled(true); return; }

        double finalHealth = p.getHealth() - e.getFinalDamage();
        if (finalHealth <= 0.5) {
            e.setCancelled(true);
            UUID winner = m.opponentOf(p.getUniqueId());
            plugin.matches().end(m, winner, p.getUniqueId());
        }
    }

    private Player resolveKiller(EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent ede) {
            if (ede.getDamager() instanceof Player k) return k;
            if (ede.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player k) return k;
        }
        return null;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().clear();
        e.setDroppedExp(0);
        Player p = e.getEntity();
        if (plugin.ffa().isInFfa(p)) { plugin.ffa().handleDeath(p, p.getKiller()); return; }
        Match m = plugin.matches().getMatch(p);
        if (m != null) plugin.matches().end(m, m.opponentOf(p.getUniqueId()), p.getUniqueId());
    }
}
