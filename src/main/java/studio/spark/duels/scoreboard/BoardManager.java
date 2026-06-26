package studio.spark.duels.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.match.Match;
import studio.spark.duels.model.Mode;
import studio.spark.duels.util.Ctx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoardManager {

    private final SparkDuels plugin;
    private final Map<UUID, Board> boards = new HashMap<>();

    public BoardManager(SparkDuels plugin) { this.plugin = plugin; }

    public void remove(Player p) { boards.remove(p.getUniqueId()); }

    public void tick() {
        boolean enabled = plugin.getConfig().getBoolean("settings.scoreboards-enabled", true);
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Board board = boards.computeIfAbsent(p.getUniqueId(), k -> new Board(p));
            String key = stateKey(p);
            if (!enabled || !plugin.scoreboards().getBoolean(key + ".enabled", true)) {
                board.clear();
                continue;
            }
            Ctx ctx = contextFor(p, key);
            String titleRaw = plugin.scoreboards().getString(key + ".title", "");
            Component title = plugin.msg().parse(plugin.placeholders().apply(titleRaw, p, ctx));
            List<Component> lines = new ArrayList<>();
            for (String line : plugin.scoreboards().getStringList(key + ".lines")) {
                lines.add(plugin.msg().parse(plugin.placeholders().apply(line, p, ctx)));
            }
            board.show(key, title, lines);
        }
    }

    private String stateKey(Player p) {
        if (plugin.matches().isInMatch(p)) return "duel";
        if (plugin.ffa().isInFfa(p)) return "ffa";
        if (plugin.queue().isQueued(p)) return "queue";
        return "lobby";
    }

    private Ctx contextFor(Player p, String key) {
        Ctx ctx = Ctx.of();
        switch (key) {
            case "duel" -> {
                Match m = plugin.matches().getMatch(p);
                if (m != null) {
                    Player opp = plugin.getServer().getPlayer(m.opponentOf(p.getUniqueId()));
                    ctx.put("duel_mode", m.mode().display())
                       .put("duel_arena", m.arena().name())
                       .put("duel_duration", m.elapsedSeconds() + "s")
                       .put("duel_opponent", opp != null ? opp.getName() : "?")
                       .put("duel_health", String.format("%.1f", p.getHealth()))
                       .put("duel_opponent_health", opp != null ? String.format("%.1f", opp.getHealth()) : "0");
                }
            }
            case "queue" -> {
                Mode mode = plugin.queue().modeOf(p);
                ctx.put("queue_mode", mode != null ? mode.display() : "—")
                   .put("queue_kit", mode != null ? mode.kit() : "—")
                   .put("queue_position", plugin.queue().positionOf(p))
                   .put("queue_wait_time", plugin.queue().waitTime(p))
                   .put("queue_players", plugin.queue().playersIn(p));
            }
            case "ffa" -> {
                var arena = plugin.ffa().get(plugin.ffa().arenaName(p));
                Mode mode = arena != null ? plugin.modes().get(arena.mode()) : null;
                ctx.put("ffa_mode", mode != null ? mode.display() : "—")
                   .put("ffa_arena", arena != null ? arena.name() : "—")
                   .put("ffa_killstreak", plugin.ffa().killstreakOf(p))
                   .put("ffa_players", arena != null ? plugin.ffa().playersIn(arena.name()) : 0);
            }
            default -> { /* lobby uses general placeholders */ }
        }
        return ctx;
    }
}
