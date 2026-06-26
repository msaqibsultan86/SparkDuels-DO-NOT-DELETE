package studio.spark.duels.util;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import studio.spark.duels.SparkDuels;

import java.util.Locale;

/** Plays configurable sounds from config.yml -> sounds.* */
public class Sounds {

    private final SparkDuels plugin;

    public Sounds(SparkDuels plugin) {
        this.plugin = plugin;
    }

    public void play(Player p, String key) {
        if (p == null) return;
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("sounds");
        if (root == null || !root.getBoolean("enabled", true)) return;
        ConfigurationSection s = root.getConfigurationSection(key);
        if (s == null) return;
        String name = s.getString("sound", "");
        if (name.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(name.toUpperCase(Locale.ROOT));
            float vol = (float) s.getDouble("volume", 1.0);
            float pitch = (float) s.getDouble("pitch", 1.0);
            p.playSound(p.getLocation(), sound, vol, pitch);
        } catch (IllegalArgumentException ignored) {
            // unknown sound name in config - skip silently
        }
    }
}
