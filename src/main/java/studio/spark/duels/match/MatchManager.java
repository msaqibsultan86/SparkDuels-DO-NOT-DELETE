package studio.spark.duels.match;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.arena.Arena;
import studio.spark.duels.kit.Kit;
import studio.spark.duels.model.Mode;
import studio.spark.duels.util.Ctx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MatchManager {

    private static final String[] CIRCLED = {"①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨"};

    private final SparkDuels plugin;
    private final Map<UUID, Match> byPlayer = new HashMap<>();

    public MatchManager(SparkDuels plugin) {
        this.plugin = plugin;
    }

    public Match getMatch(Player p) { return p == null ? null : byPlayer.get(p.getUniqueId()); }
    public Match getMatch(UUID id) { return byPlayer.get(id); }
    public boolean isInMatch(Player p) { return p != null && byPlayer.containsKey(p.getUniqueId()); }
    public boolean isLive(Player p) {
        Match m = getMatch(p);
        return m != null && m.state() == Match.State.LIVE;
    }

    public void start(Player a, Player b, Mode mode) {
        Kit kit = plugin.kits().get(mode.kit());
        if (kit == null) {
            plugin.msg().send(a, "kit.not-found", Ctx.of().put("kit", mode.kit()));
            plugin.msg().send(b, "kit.not-found", Ctx.of().put("kit", mode.kit()));
            return;
        }
        Arena arena = plugin.arenas().allocate();
        if (arena == null) {
            plugin.msg().send(a, "duel.no-arena");
            plugin.msg().send(b, "duel.no-arena");
            return;
        }

        Match match = new Match(a.getUniqueId(), b.getUniqueId(), mode, kit, arena);
        byPlayer.put(a.getUniqueId(), match);
        byPlayer.put(b.getUniqueId(), match);

        prepare(a, arena.spawn1(), kit);
        prepare(b, arena.spawn2(), kit);

        plugin.msg().send(a, "match.found", Ctx.of().put("duel_opponent", b.getName()).put("duel_mode", mode.display()));
        plugin.msg().send(b, "match.found", Ctx.of().put("duel_opponent", a.getName()).put("duel_mode", mode.display()));
        plugin.sounds().play(a, "match-found");
        plugin.sounds().play(b, "match-found");

        runCountdown(match);
    }

    private void prepare(Player p, Location spawn, Kit kit) {
        if (spawn != null) p.teleport(spawn);
        p.setGameMode(GameMode.SURVIVAL);
        heal(p);
        kit.apply(p);
    }

    private void runCountdown(Match match) {
        int seconds = Math.max(1, plugin.getConfig().getInt("match.countdown-seconds", 5));
        new BukkitRunnable() {
            int left = seconds;
            @Override
            public void run() {
                Player p1 = plugin.getServer().getPlayer(match.player1());
                Player p2 = plugin.getServer().getPlayer(match.player2());
                if (p1 == null || p2 == null || match.state() == Match.State.ENDED) { cancel(); return; }

                if (left <= 0) {
                    match.state(Match.State.LIVE);
                    plugin.msg().title(p1, "match.fight-title", "match.fight-subtitle", Ctx.of(), 0, 600, 200);
                    plugin.msg().title(p2, "match.fight-title", "match.fight-subtitle", Ctx.of(), 0, 600, 200);
                    plugin.sounds().play(p1, "fight");
                    plugin.sounds().play(p2, "fight");
                    cancel();
                    return;
                }

                Ctx ctx = Ctx.of().put("duel_countdown", circled(left));
                plugin.msg().title(p1, "match.countdown-title", "match.countdown-subtitle", ctx, 0, 900, 100);
                plugin.msg().title(p2, "match.countdown-title", "match.countdown-subtitle", ctx, 0, 900, 100);
                plugin.sounds().play(p1, "countdown");
                plugin.sounds().play(p2, "countdown");
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void end(Match match, UUID winnerId, UUID loserId) {
        if (match.state() == Match.State.ENDED) return;
        match.state(Match.State.ENDED);

        byPlayer.remove(match.player1());
        byPlayer.remove(match.player2());
        plugin.arenas().release(match.arena());

        if (winnerId != null && loserId != null) {
            plugin.stats().recordWin(winnerId, loserId, true);
            Player winner = plugin.getServer().getPlayer(winnerId);
            Player loser = plugin.getServer().getPlayer(loserId);

            if (loser != null && plugin.getConfig().getBoolean("match.lightning-on-loss", true)) {
                loser.getWorld().strikeLightningEffect(loser.getLocation());
            }
            if (winner != null) {
                plugin.msg().title(winner, "match.win-title", "match.win-subtitle", Ctx.of(), 200, 2000, 400);
                plugin.sounds().play(winner, "win");
            }
            if (loser != null) {
                plugin.msg().title(loser, "match.loss-title", "match.loss-subtitle", Ctx.of(), 200, 2000, 400);
                plugin.sounds().play(loser, "loss");
            }
            String wName = winner != null ? winner.getName() : "?";
            String lName = loser != null ? loser.getName() : "?";
            plugin.getServer().getOnlinePlayers().forEach(o ->
                    plugin.msg().sendRaw(o, plugin.msg().prefix() + plugin.msg().raw("match.win-broadcast"),
                            Ctx.of().put("duel_winner", wName).put("duel_loser", lName)
                                    .put("duel_mode", match.mode().display())));
        }

        long delay = Math.max(0, plugin.getConfig().getLong("match.end-delay-seconds", 3)) * 20L;
        Player p1 = plugin.getServer().getPlayer(match.player1());
        Player p2 = plugin.getServer().getPlayer(match.player2());
        if (p1 != null) plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.lobby().toLobby(p1), delay);
        if (p2 != null) plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.lobby().toLobby(p2), delay);
    }

    public void handleLeave(Player quitter) {
        Match m = getMatch(quitter);
        if (m == null) return;
        end(m, m.opponentOf(quitter.getUniqueId()), quitter.getUniqueId());
    }

    /** Called periodically - handles Sumo/Spleef void/fall losses. */
    public void tick() {
        for (Match m : new HashMap<>(byPlayer).values()) {
            if (m.state() != Match.State.LIVE) continue;
            if (m.mode().type() == Mode.Type.NORMAL) continue;
            double floor = floorY(m.arena());
            for (UUID id : new UUID[]{m.player1(), m.player2()}) {
                Player p = plugin.getServer().getPlayer(id);
                if (p != null && p.getLocation().getY() < floor) {
                    end(m, m.opponentOf(id), id);
                    break;
                }
            }
        }
    }

    private double floorY(Arena a) {
        if (a.corner1() != null && a.corner2() != null) {
            return Math.min(a.corner1().getY(), a.corner2().getY()) - 1;
        }
        double base = a.spawn1() != null ? a.spawn1().getY() : 0;
        return base - 3;
    }

    private void heal(Player p) {
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
    }

    public static String circled(int n) {
        return (n >= 1 && n <= 9) ? CIRCLED[n - 1] : String.valueOf(n);
    }

    public void endAll() {
        for (Match m : new HashMap<>(byPlayer).values()) end(m, null, null);
    }
}
