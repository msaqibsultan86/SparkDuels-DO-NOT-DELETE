package studio.spark.duels.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.util.Ctx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Handles /spduplicate (copy to clipboard) and /sparena paste. */
public class Duplicator {

    private final SparkDuels plugin;
    private final Map<UUID, Clipboard> clipboards = new HashMap<>();

    public Duplicator(SparkDuels plugin) { this.plugin = plugin; }

    private static final class Clipboard {
        int sizeX, sizeY, sizeZ;
        BlockData[] grid;        // index = ((dx*sizeY)+dy)*sizeZ+dz
        Vector relSpawn1, relSpawn2;
        float yaw1, pitch1, yaw2, pitch2;
    }

    public void copy(Player admin, String arenaName) {
        Arena a = plugin.arenas().get(arenaName);
        if (a == null) { plugin.msg().send(admin, "arena.not-found", Ctx.of().put("arena", arenaName)); return; }
        if (a.corner1() == null || a.corner2() == null) { plugin.msg().send(admin, "arena.need-corners"); return; }

        Location c1 = a.corner1(), c2 = a.corner2();
        World w = c1.getWorld();
        int minX = Math.min(c1.getBlockX(), c2.getBlockX());
        int minY = Math.min(c1.getBlockY(), c2.getBlockY());
        int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
        int maxX = Math.max(c1.getBlockX(), c2.getBlockX());
        int maxY = Math.max(c1.getBlockY(), c2.getBlockY());
        int maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ());

        Clipboard cb = new Clipboard();
        cb.sizeX = maxX - minX + 1;
        cb.sizeY = maxY - minY + 1;
        cb.sizeZ = maxZ - minZ + 1;
        long volume = (long) cb.sizeX * cb.sizeY * cb.sizeZ;
        long cap = plugin.getConfig().getLong("arena.max-clipboard-volume", 250000);
        if (volume > cap) {
            plugin.msg().send(admin, "arena.too-large", Ctx.of().put("count", volume).put("size", cap));
            return;
        }

        cb.grid = new BlockData[(int) volume];
        for (int dx = 0; dx < cb.sizeX; dx++) {
            for (int dy = 0; dy < cb.sizeY; dy++) {
                for (int dz = 0; dz < cb.sizeZ; dz++) {
                    cb.grid[idx(cb, dx, dy, dz)] = w.getBlockAt(minX + dx, minY + dy, minZ + dz).getBlockData().clone();
                }
            }
        }
        Vector min = new Vector(minX, minY, minZ);
        if (a.spawn1() != null) { cb.relSpawn1 = a.spawn1().toVector().subtract(min); cb.yaw1 = a.spawn1().getYaw(); cb.pitch1 = a.spawn1().getPitch(); }
        if (a.spawn2() != null) { cb.relSpawn2 = a.spawn2().toVector().subtract(min); cb.yaw2 = a.spawn2().getYaw(); cb.pitch2 = a.spawn2().getPitch(); }

        clipboards.put(admin.getUniqueId(), cb);
        plugin.msg().send(admin, "arena.copied", Ctx.of().put("arena", arenaName).put("count", volume));
    }

    public void paste(Player admin, String newName) {
        Clipboard cb = clipboards.get(admin.getUniqueId());
        if (cb == null) { plugin.msg().send(admin, "arena.no-clipboard"); return; }
        if (plugin.arenas().get(newName) != null) { plugin.msg().send(admin, "arena.exists", Ctx.of().put("arena", newName)); return; }

        Location origin = admin.getLocation().getBlock().getLocation();
        World w = origin.getWorld();
        int ox = origin.getBlockX(), oy = origin.getBlockY(), oz = origin.getBlockZ();

        for (int dx = 0; dx < cb.sizeX; dx++) {
            for (int dy = 0; dy < cb.sizeY; dy++) {
                for (int dz = 0; dz < cb.sizeZ; dz++) {
                    BlockData data = cb.grid[idx(cb, dx, dy, dz)];
                    if (data != null) w.getBlockAt(ox + dx, oy + dy, oz + dz).setBlockData(data, false);
                }
            }
        }

        Arena a = plugin.arenas().create(newName);
        a.world(w.getName());
        a.corner1(new Location(w, ox, oy, oz));
        a.corner2(new Location(w, ox + cb.sizeX - 1, oy + cb.sizeY - 1, oz + cb.sizeZ - 1));
        if (cb.relSpawn1 != null) a.spawn1(new Location(w, ox + cb.relSpawn1.getX(), oy + cb.relSpawn1.getY(), oz + cb.relSpawn1.getZ(), cb.yaw1, cb.pitch1));
        if (cb.relSpawn2 != null) a.spawn2(new Location(w, ox + cb.relSpawn2.getX(), oy + cb.relSpawn2.getY(), oz + cb.relSpawn2.getZ(), cb.yaw2, cb.pitch2));
        a.enabled(true);
        plugin.arenas().save();
        plugin.msg().send(admin, "arena.pasted", Ctx.of().put("arena", newName));
    }

    private int idx(Clipboard cb, int dx, int dy, int dz) {
        return ((dx * cb.sizeY) + dy) * cb.sizeZ + dz;
    }
}
