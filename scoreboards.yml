package studio.spark.duels.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.util.Loc;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final SparkDuels plugin;
    private final File file;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<UUID, String> setupSession = new HashMap<>(); // admin -> arena being built

    public ArenaManager(SparkDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        load();
    }

    public Collection<Arena> all() { return arenas.values(); }
    public Arena get(String name) { return name == null ? null : arenas.get(name.toLowerCase(Locale.ROOT)); }

    public Arena create(String name) {
        Arena a = new Arena(name);
        arenas.put(name.toLowerCase(Locale.ROOT), a);
        return a;
    }

    public void beginSetup(UUID admin, String arena) { setupSession.put(admin, arena.toLowerCase(Locale.ROOT)); }
    public Arena currentSetup(UUID admin) {
        String n = setupSession.get(admin);
        return n == null ? null : arenas.get(n);
    }

    public boolean delete(String name) {
        boolean removed = arenas.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) save();
        return removed;
    }

    /** First complete, enabled, free arena - marked in use. */
    public Arena allocate() {
        for (Arena a : arenas.values()) {
            if (a.enabled() && a.isComplete() && !a.inUse()) {
                a.inUse(true);
                return a;
            }
        }
        return null;
    }

    public void release(Arena a) { if (a != null) a.inUse(false); }

    public void load() {
        arenas.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("arenas");
        if (root == null) return;
        for (String name : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(name);
            if (s == null) continue;
            Arena a = new Arena(name);
            a.world(s.getString("world"));
            a.enabled(s.getBoolean("enabled", true));
            World w = Loc.worldOrFirst(a.world());
            a.spawn1(Loc.readSpawn(s, "spawn1", w));
            a.spawn2(Loc.readSpawn(s, "spawn2", w));
            a.corner1(Loc.readSpawn(s, "corner1", w));
            a.corner2(Loc.readSpawn(s, "corner2", w));
            arenas.put(name.toLowerCase(Locale.ROOT), a);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection root = cfg.createSection("arenas");
        for (Arena a : arenas.values()) {
            ConfigurationSection s = root.createSection(a.name());
            if (a.world() != null) s.set("world", a.world());
            s.set("enabled", a.enabled());
            Loc.writeSpawn(s, "spawn1", a.spawn1());
            Loc.writeSpawn(s, "spawn2", a.spawn2());
            Loc.writeSpawn(s, "corner1", a.corner1());
            Loc.writeSpawn(s, "corner2", a.corner2());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save arenas.yml: " + e.getMessage());
        }
    }
}
