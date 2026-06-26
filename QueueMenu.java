package studio.spark.duels.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.stats.PlayerStats;

import java.util.HashMap;
import java.util.Map;

/** Resolves %placeholder% tokens for messages, scoreboards, lore. */
public class Placeholders {

    private final SparkDuels plugin;

    public Placeholders(SparkDuels plugin) {
        this.plugin = plugin;
    }

    public String apply(String text, Player p, Ctx ctx) {
        if (text == null || text.isEmpty()) return "";
        if (text.indexOf('%') < 0 && (ctx == null || ctx.values.isEmpty())) return text;

        Map<String, String> m = new HashMap<>();
        m.put("prefix", plugin.messages().getString("prefix", ""));
        m.put("server", Bukkit.getServer().getName());
        m.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        m.put("max_players", String.valueOf(Bukkit.getMaxPlayers()));
        m.put("plugin_version", plugin.getPluginMeta().getVersion());
        m.put("discord_link", plugin.getConfig().getString("discord-link", "discord.gg/sparkstudios"));
        m.put("modes_total", String.valueOf(plugin.modes().all().size()));
        m.put("arena_total", String.valueOf(plugin.arenas().all().size()));
        m.put("ffa_arena_total", String.valueOf(plugin.ffa() == null ? 0 : plugin.ffa().arenaCount()));

        if (p != null) {
            m.put("player", p.getName());
            m.put("world", p.getWorld().getName());
            m.put("ping", String.valueOf(p.getPing()));
            PlayerStats s = plugin.stats().get(p.getUniqueId());
            m.put("wins", String.valueOf(s.wins));
            m.put("losses", String.valueOf(s.losses));
            m.put("kills", String.valueOf(s.kills));
            m.put("deaths", String.valueOf(s.deaths));
            m.put("streak", String.valueOf(s.streak));
            m.put("best_streak", String.valueOf(s.bestStreak));
            m.put("elo", String.valueOf(s.elo));
            m.put("coins", String.valueOf(s.coins));
            m.put("kdr", String.format("%.2f", s.kdr()));
            m.put("winrate", String.valueOf(s.winrate()));
            m.put("matches_played", String.valueOf(s.wins + s.losses));
            m.put("ffa_kills", String.valueOf(s.ffaKills));
            m.put("ffa_deaths", String.valueOf(s.ffaDeaths));
            m.put("ffa_best_streak", String.valueOf(s.ffaBestStreak));
            m.put("ffa_kdr", String.format("%.2f", s.ffaKdr()));
            m.put("ffa_killstreak", String.valueOf(plugin.ffa() == null ? 0 : plugin.ffa().killstreakOf(p)));
            int rank = plugin.stats().rankOf(p.getUniqueId());
            m.put("rank", rank > 0 ? "#" + rank : "—");
        }

        if (ctx != null) m.putAll(ctx.values);

        StringBuilder out = new StringBuilder(text.length());
        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '%') {
                int end = text.indexOf('%', i + 1);
                if (end > i) {
                    String key = text.substring(i + 1, end);
                    String val = m.get(key);
                    if (val != null) {
                        out.append(val);
                        i = end + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }
}
