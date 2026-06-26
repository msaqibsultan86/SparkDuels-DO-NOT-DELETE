package studio.spark.duels.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.match.Match;
import studio.spark.duels.model.Mode;
import studio.spark.duels.stats.PlayerStats;

/** PlaceholderAPI expansion: %sparkduels_<key>% */
public class SparkExpansion extends PlaceholderExpansion {

    private final SparkDuels plugin;

    public SparkExpansion(SparkDuels plugin) { this.plugin = plugin; }

    @Override public String getIdentifier() { return "sparkduels"; }
    @Override public String getAuthor() { return "Spark Studios"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer op, String params) {
        if (op == null || params == null) return null;
        String key = params.toLowerCase();
        PlayerStats s = plugin.stats().get(op.getUniqueId());

        switch (key) {
            case "wins": return String.valueOf(s.wins);
            case "losses": return String.valueOf(s.losses);
            case "kills": return String.valueOf(s.kills);
            case "deaths": return String.valueOf(s.deaths);
            case "streak": return String.valueOf(s.streak);
            case "best_streak": return String.valueOf(s.bestStreak);
            case "elo": return String.valueOf(s.elo);
            case "coins": return String.valueOf(s.coins);
            case "kdr": return String.format("%.2f", s.kdr());
            case "winrate": return String.valueOf(s.winrate());
            case "matches_played": return String.valueOf(s.wins + s.losses);
            case "rank": { int r = plugin.stats().rankOf(op.getUniqueId()); return r > 0 ? "#" + r : "—"; }
            case "ffa_kills": return String.valueOf(s.ffaKills);
            case "ffa_deaths": return String.valueOf(s.ffaDeaths);
            case "ffa_streak": return String.valueOf(s.ffaStreak);
            case "ffa_best_streak": return String.valueOf(s.ffaBestStreak);
            case "ffa_kdr": return String.format("%.2f", s.ffaKdr());
            case "online": return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "max_players": return String.valueOf(Bukkit.getMaxPlayers());
            case "modes_total": return String.valueOf(plugin.modes().all().size());
            case "arena_total": return String.valueOf(plugin.arenas().all().size());
            case "ffa_arena_total": return String.valueOf(plugin.ffa().arenaCount());
            case "version": return plugin.getPluginMeta().getVersion();
            case "discord_link": return plugin.getConfig().getString("discord-link", "discord.gg/sparkstudios");
            default: break;
        }

        Player p = op.isOnline() ? op.getPlayer() : null;
        if (p == null) return null;

        switch (key) {
            case "ping": return String.valueOf(p.getPing());
            case "world": return p.getWorld().getName();
            case "ffa_killstreak": return String.valueOf(plugin.ffa().killstreakOf(p));
            case "state": return plugin.matches().isInMatch(p) ? "duel"
                    : plugin.ffa().isInFfa(p) ? "ffa"
                    : plugin.queue().isQueued(p) ? "queue" : "lobby";
            default: break;
        }

        if (plugin.queue().isQueued(p)) {
            Mode m = plugin.queue().modeOf(p);
            switch (key) {
                case "queue_mode": return m != null ? m.display() : "—";
                case "queue_position": return String.valueOf(plugin.queue().positionOf(p));
                case "queue_players": return String.valueOf(plugin.queue().playersIn(p));
                case "queue_wait_time": return plugin.queue().waitTime(p);
                default: break;
            }
        }

        Match match = plugin.matches().getMatch(p);
        if (match != null) {
            Player opp = plugin.getServer().getPlayer(match.opponentOf(p.getUniqueId()));
            switch (key) {
                case "duel_mode": return match.mode().display();
                case "duel_arena": return match.arena().name();
                case "duel_duration": return match.elapsedSeconds() + "s";
                case "duel_opponent": return opp != null ? opp.getName() : "?";
                case "duel_health": return String.format("%.1f", p.getHealth());
                case "duel_opponent_health": return opp != null ? String.format("%.1f", opp.getHealth()) : "0";
                default: break;
            }
        }
        return null;
    }
}
