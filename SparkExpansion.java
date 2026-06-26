package studio.spark.duels.ffa;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.kit.Kit;
import studio.spark.duels.model.Mode;
import studio.spark.duels.util.Ctx;
import studio.spark.duels.util.Loc;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FFAManager {

    private final SparkDuels plugin;
    private final File file;
    private final Map<String, FFAArena> arenas = new LinkedHashMap<>();
    private final Map<UUID, String> inFfa = new HashMap<>();      // player -> arena name
    private final Map<UUID, Integer> killstreak = new HashMap<>();

    public FFAManager(SparkDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ffa.yml");
        load();
    }

    public Collection<FFAArena> all() { return arenas.values(); }
    public FFAArena get(String name) { return name == null ? null : arenas.get(name.toLowerCase(Locale.ROOT)); }
    public boolean isInFfa(Player p) { return p != null && inFfa.containsKey(p.getUniqueId()); }
    public int killstreakOf(Player p) { return killstreak.getOrDefault(p.getUniqueId(), 0); }
    public int arenaCount() { return arenas.size(); }

    public FFAArena arenaFor(String modeId) {
        int min = plugin.getConfig().getInt("ffa.min-spawns", 3);
        for (FFAArena a : arenas.values()) {
            if (a.isReady(min) && modeId.equalsIgnoreCase(a.mode())) return a;
        }
        return null;
    }

    public int playersIn(String arenaName) {
        int n = 0;
        for (String v : inFfa.values()) if (v.equalsIgnoreCase(arenaName)) n++;
        return n;
    }

    public String arenaName(Player p) { return inFfa.get(p.getUniqueId()); }

    // ---- setup ----
    public FFAArena create(String name) {
        FFAArena a = new FFAArena(name);
        arenas.put(name.toLowerCase(Locale.ROOT), a);
        save();
        return a;
    }

    public boolean delete(String name) {
        boolean ok = arenas.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (ok) save();
        return ok;
    }

    public void addSpawn(FFAArena a, Location loc) { a.spawns().add(loc); a.world(loc.getWorld().getName()); save(); }
    public void setKit(FFAArena a, String kit) { a.kit(kit); save(); }
    public void setMode(FFAArena a, String mode) { a.mode(mode); save(); }
    public void setEnabled(FFAArena a, boolean v) { a.enabled(v); save(); }

    // ---- play ----
    public void join(Player p, String modeId) {
        if (plugin.matches().isInMatch(p)) { plugin.msg().send(p, "duel.already-in-match"); return; }
        Mode mode = plugin.modes().get(modeId);
        if (mode == null || !mode.ffa()) { plugin.msg().send(p, "ffa.invalid-mode", Ctx.of().put("ffa_mode", modeId)); return; }
        FFAArena arena = arenaFor(mode.id());
        if (arena == null) { plugin.msg().send(p, "ffa.no-arena", Ctx.of().put("ffa_mode", mode.display())); return; }

        plugin.queue().leaveSilent(p);
        inFfa.put(p.getUniqueId(), arena.name().toLowerCase(Locale.ROOT));
        killstreak.put(p.getUniqueId(), 0);
        spawnInto(p, arena);
        plugin.msg().send(p, "ffa.joined", Ctx.of().put("ffa_mode", mode.display()).put("ffa_arena", arena.name()));
        plugin.sounds().play(p, "ffa-join");
    }

    public void leave(Player p) {
        if (!isInFfa(p)) { plugin.msg().send(p, "ffa.not-in"); return; }
        inFfa.remove(p.getUniqueId());
        killstreak.remove(p.getUniqueId());
        plugin.lobby().toLobby(p);
        plugin.msg().send(p, "ffa.left");
    }

    public void leaveSilent(Player p) {
        inFfa.remove(p.getUniqueId());
        killstreak.remove(p.getUniqueId());
    }

    private void spawnInto(Player p, FFAArena arena) {
        Location spawn = randomSpawn(arena);
        if (spawn != null) p.teleport(spawn);
        p.setGameMode(GameMode.SURVIVAL);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        Kit kit = resolveKit(arena);
        if (kit != null) kit.apply(p);
        int prot = plugin.getConfig().getInt("ffa.spawn-protection", 2);
        if (prot > 0) p.setNoDamageTicks(prot * 20);
    }

    private Kit resolveKit(FFAArena arena) {
        Kit kit = arena.kit() != null ? plugin.kits().get(arena.kit()) : null;
        if (kit == null) {
            Mode mode = plugin.modes().get(arena.mode());
            if (mode != null) kit = plugin.kits().get(mode.kit());
        }
        return kit;
    }

    private Location randomSpawn(FFAArena arena) {
        if (arena.spawns().isEmpty()) return null;
        return arena.spawns().get(ThreadLocalRandom.current().nextInt(arena.spawns().size()));
    }

    /** Returns true if handled (victim was in FFA). */
    public boolean handleDeath(Player victim, Player killer) {
        if (!isInFfa(victim)) return false;
        FFAArena arena = get(arenaName(victim));

        plugin.stats().get(victim.getUniqueId()).ffaDeaths++;
        plugin.stats().get(victim.getUniqueId()).ffaStreak = 0;
        killstreak.put(victim.getUniqueId(), 0);
        plugin.sounds().play(victim, "ffa-death");

        if (killer != null && !killer.equals(victim) && isInFfa(killer)) {
            var ks = plugin.stats().get(killer.getUniqueId());
            ks.ffaKills++;
            int streak = killstreak.merge(killer.getUniqueId(), 1, Integer::sum);
            ks.ffaStreak = streak;
            if (streak > ks.ffaBestStreak) ks.ffaBestStreak = streak;
            ks.coins += plugin.getConfig().getInt("ffa.coins-per-kill", 5);
            killer.setHealth(killer.getMaxHealth());
            killer.setFoodLevel(20);
            plugin.msg().send(killer, "ffa.kill", Ctx.of().put("target", victim.getName()).put("ffa_streak", streak));
            plugin.sounds().play(killer, "success");
            if (streak % 5 == 0) {
                plugin.getServer().getOnlinePlayers().forEach(o ->
                        plugin.msg().send(o, "ffa.killstreak", Ctx.of().put("player", killer.getName()).put("ffa_streak", streak)));
            }
        }
        if (arena != null) spawnInto(victim, arena);
        plugin.stats().save();
        return true;
    }

    /** Void/knockoff check for FFA players. */
    public void tick() {
        for (Map.Entry<UUID, String> e : new HashMap<>(inFfa).entrySet()) {
            Player p = plugin.getServer().getPlayer(e.getKey());
            if (p == null) continue;
            FFAArena arena = get(e.getValue());
            if (arena == null) continue;
            double floor = floorY(arena);
            if (p.getLocation().getY() < floor) handleDeath(p, null);
        }
    }

    private double floorY(FFAArena a) {
        if (a.corner1() != null && a.corner2() != null) return Math.min(a.corner1().getY(), a.corner2().getY()) - 1;
        double base = a.spawns().isEmpty() ? 0 : a.spawns().get(0).getY();
        return base - 5;
    }

    // ---- persistence ----
    @SuppressWarnings("unchecked")
    public void load() {
        arenas.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("arenas");
        if (root == null) return;
        for (String name : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(name);
            if (s == null) continue;
            FFAArena a = new FFAArena(name);
            a.world(s.getString("world"));
            a.enabled(s.getBoolean("enabled", true));
            a.mode(s.getString("mode"));
            a.kit(s.getString("kit"));
            World w = Loc.worldOrFirst(a.world());
            a.corner1(Loc.readSpawn(s, "corner1", w));
            a.corner2(Loc.readSpawn(s, "corner2", w));
            for (Map<?, ?> m : s.getMapList("spawns")) {
                double x = num(m.get("x")), y = num(m.get("y")), z = num(m.get("z"));
                float yaw = (float) num(m.get("yaw")), pitch = (float) num(m.get("pitch"));
                if (w != null) a.spawns().add(new Location(w, x, y, z, yaw, pitch));
            }
            arenas.put(name.toLowerCase(Locale.ROOT), a);
        }
    }

    private double num(Object o) { return o instanceof Number n ? n.doubleValue() : 0; }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection root = cfg.createSection("arenas");
        for (FFAArena a : arenas.values()) {
            ConfigurationSection s = root.createSection(a.name());
            if (a.world() != null) s.set("world", a.world());
            s.set("enabled", a.enabled());
            if (a.mode() != null) s.set("mode", a.mode());
            if (a.kit() != null) s.set("kit", a.kit());
            Loc.writeSpawn(s, "corner1", a.corner1());
            Loc.writeSpawn(s, "corner2", a.corner2());
            List<Map<String, Object>> spawns = new ArrayList<>();
            for (Location l : a.spawns()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("x", l.getX()); m.put("y", l.getY()); m.put("z", l.getZ());
                m.put("yaw", (double) l.getYaw()); m.put("pitch", (double) l.getPitch());
                spawns.add(m);
            }
            s.set("spawns", spawns);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save ffa.yml: " + e.getMessage());
        }
    }
}
