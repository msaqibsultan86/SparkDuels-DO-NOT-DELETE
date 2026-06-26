package studio.spark.duels.lobby;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import studio.spark.duels.SparkDuels;

import java.io.File;
import java.io.IOException;

public class LobbyManager {

    private final SparkDuels plugin;
    private final File file;
    private Location spawn;

    public LobbyManager(SparkDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    public Location getSpawn() { return spawn; }
    public boolean hasSpawn() { return spawn != null; }

    public void setSpawn(Location loc) {
        this.spawn = loc;
        save();
    }

    public int size() { return plugin.getConfig().getInt("lobby.protection-size", 1500); }

    /** Is this location inside the protected lobby square (full height)? */
    public boolean inRegion(Location loc) {
        if (spawn == null || loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().equals(spawn.getWorld())) return false;
        double half = size() / 2.0;
        return Math.abs(loc.getX() - spawn.getX()) <= half
                && Math.abs(loc.getZ() - spawn.getZ()) <= half;
    }

    public void toLobby(Player p) {
        plugin.queue().leaveSilent(p);
        if (plugin.ffa() != null) plugin.ffa().leaveSilent(p);
        if (spawn != null) p.teleport(spawn);
        p.setGameMode(GameMode.ADVENTURE);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        p.getInventory().clear();
        p.updateInventory();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        this.spawn = cfg.getLocation("spawn");
    }

    private void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        if (spawn != null) cfg.set("spawn", spawn);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }
}
