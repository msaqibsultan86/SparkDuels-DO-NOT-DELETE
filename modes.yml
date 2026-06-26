package studio.spark.duels.arena;

import org.bukkit.Location;

public class Arena {

    private final String name;
    private String world;
    private boolean enabled = true;
    private Location spawn1, spawn2, corner1, corner2;
    private boolean inUse = false;

    public Arena(String name) { this.name = name; }

    public String name() { return name; }
    public String world() { return world; }
    public void world(String w) { this.world = w; }

    public boolean enabled() { return enabled; }
    public void enabled(boolean v) { this.enabled = v; }

    public Location spawn1() { return spawn1; }
    public Location spawn2() { return spawn2; }
    public Location corner1() { return corner1; }
    public Location corner2() { return corner2; }
    public void spawn1(Location l) { this.spawn1 = l; if (l != null) world = l.getWorld().getName(); }
    public void spawn2(Location l) { this.spawn2 = l; }
    public void corner1(Location l) { this.corner1 = l; }
    public void corner2(Location l) { this.corner2 = l; }

    public boolean inUse() { return inUse; }
    public void inUse(boolean v) { this.inUse = v; }

    public boolean isComplete() {
        return spawn1 != null && spawn2 != null;
    }
}
