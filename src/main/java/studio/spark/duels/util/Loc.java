package studio.spark.duels.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/** Compact location <-> YAML helpers (world stored separately). */
public final class Loc {

    private Loc() {}

    public static void writeSpawn(ConfigurationSection sec, String path, Location l) {
        if (l == null) return;
        ConfigurationSection s = sec.createSection(path);
        s.set("x", l.getX());
        s.set("y", l.getY());
        s.set("z", l.getZ());
        s.set("yaw", (double) l.getYaw());
        s.set("pitch", (double) l.getPitch());
    }

    public static Location readSpawn(ConfigurationSection sec, String path, World world) {
        ConfigurationSection s = sec.getConfigurationSection(path);
        if (s == null || world == null) return null;
        return new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    public static World worldOrFirst(String name) {
        World w = name == null ? null : Bukkit.getWorld(name);
        if (w != null) return w;
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }
}
