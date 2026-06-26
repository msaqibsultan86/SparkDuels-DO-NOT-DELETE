package studio.spark.duels.stats;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import studio.spark.duels.SparkDuels;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StatsManager {

    private final SparkDuels plugin;
    private final File file;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();
    private final int startElo;
    private final int kFactor;

    public StatsManager(SparkDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        this.startElo = plugin.getConfig().getInt("stats.starting-elo", 1000);
        this.kFactor = plugin.getConfig().getInt("stats.k-factor", 32);
        load();
    }

    public PlayerStats get(UUID id) {
        return cache.computeIfAbsent(id, k -> new PlayerStats(startElo));
    }

    public void recordWin(UUID winner, UUID loser, boolean ranked) {
        PlayerStats w = get(winner), l = get(loser);
        w.wins++; w.kills++; w.streak++; if (w.streak > w.bestStreak) w.bestStreak = w.streak;
        l.losses++; l.deaths++; l.streak = 0;
        w.coins += plugin.getConfig().getInt("stats.coins-per-win", 10);
        if (ranked) applyElo(winner, loser);
        save();
    }

    public void applyElo(UUID winner, UUID loser) {
        PlayerStats w = get(winner), l = get(loser);
        double expW = 1.0 / (1.0 + Math.pow(10, (l.elo - w.elo) / 400.0));
        double expL = 1.0 / (1.0 + Math.pow(10, (w.elo - l.elo) / 400.0));
        w.elo += (int) Math.round(kFactor * (1 - expW));
        l.elo += (int) Math.round(kFactor * (0 - expL));
        if (l.elo < 0) l.elo = 0;
    }

    /** 1-based rank by ELO; 0 if unknown. */
    public int rankOf(UUID id) {
        if (!cache.containsKey(id)) return 0;
        int target = get(id).elo;
        int rank = 1;
        for (PlayerStats s : cache.values()) if (s.elo > target) rank++;
        return rank;
    }

    public List<Map.Entry<UUID, PlayerStats>> top(int n, boolean byFfa) {
        List<Map.Entry<UUID, PlayerStats>> list = new ArrayList<>(cache.entrySet());
        list.sort((a, b) -> byFfa
                ? Integer.compare(b.getValue().ffaKills, a.getValue().ffaKills)
                : Integer.compare(b.getValue().wins, a.getValue().wins));
        return list.subList(0, Math.min(n, list.size()));
    }

    public void load() {
        cache.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("players");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s == null) continue;
                PlayerStats st = new PlayerStats(s.getInt("elo", startElo));
                st.wins = s.getInt("wins"); st.losses = s.getInt("losses");
                st.kills = s.getInt("kills"); st.deaths = s.getInt("deaths");
                st.streak = s.getInt("streak"); st.bestStreak = s.getInt("best_streak");
                st.coins = s.getInt("coins");
                st.ffaKills = s.getInt("ffa_kills"); st.ffaDeaths = s.getInt("ffa_deaths");
                st.ffaStreak = s.getInt("ffa_streak"); st.ffaBestStreak = s.getInt("ffa_best_streak");
                cache.put(id, st);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection root = cfg.createSection("players");
        for (Map.Entry<UUID, PlayerStats> e : cache.entrySet()) {
            ConfigurationSection s = root.createSection(e.getKey().toString());
            PlayerStats st = e.getValue();
            s.set("wins", st.wins); s.set("losses", st.losses);
            s.set("kills", st.kills); s.set("deaths", st.deaths);
            s.set("streak", st.streak); s.set("best_streak", st.bestStreak);
            s.set("elo", st.elo); s.set("coins", st.coins);
            s.set("ffa_kills", st.ffaKills); s.set("ffa_deaths", st.ffaDeaths);
            s.set("ffa_streak", st.ffaStreak); s.set("ffa_best_streak", st.ffaBestStreak);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats.yml: " + e.getMessage());
        }
    }
}
