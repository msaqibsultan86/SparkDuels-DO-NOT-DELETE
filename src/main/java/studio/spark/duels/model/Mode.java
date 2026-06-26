package studio.spark.duels.model;

import org.bukkit.Material;

public class Mode {

    public enum Type { NORMAL, SUMO, SPLEEF }

    private final String id;
    private final String display;
    private final Material icon;
    private final String kit;
    private final Type type;
    private final int slot;
    private final boolean duel;
    private final boolean ffa;

    public Mode(String id, String display, Material icon, String kit, Type type, int slot, boolean duel, boolean ffa) {
        this.id = id;
        this.display = display;
        this.icon = icon;
        this.kit = kit;
        this.type = type;
        this.slot = slot;
        this.duel = duel;
        this.ffa = ffa;
    }

    public String id() { return id; }
    public String display() { return display; }
    public Material icon() { return icon; }
    public String kit() { return kit; }
    public Type type() { return type; }
    public int slot() { return slot; }
    public boolean duel() { return duel; }
    public boolean ffa() { return ffa; }
}
