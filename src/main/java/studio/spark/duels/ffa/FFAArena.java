package studio.spark.duels.ffa;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class FFAArena {

    private final String name;
    private String world;
    private boolean enabled = true;
    private String mode;          // which mode id this FFA arena hosts
    private String kit;           // optional kit override (else mode kit)
    private final List<Location> spawns = new ArrayList<>();
    private Location corner1, corner2;

    public FFAArena(String name) { this.name = name; }

    public String name() { return name; }
    public String world() { return world; }
    public void world(String w) { this.world = w; }
    public boolean enabled() { return enabled; }
    public void enabled(boolean v) { this.enabled = v; }
    public String mode() { return mode; }
    public void mode(String m) { this.mode = m; }
    public String kit() { return kit; }
    public void kit(String k) { this.kit = k; }
    public List<Location> spawns() { return spawns; }
    public Location corner1() { return corner1; }
    public Location corner2() { return corner2; }
    public void corner1(Location l) { this.corner1 = l; }
    public void corner2(Location l) { this.corner2 = l; }

    public boolean isReady(int minSpawns) {
        return enabled && mode != null && spawns.size() >= minSpawns;
    }
}
