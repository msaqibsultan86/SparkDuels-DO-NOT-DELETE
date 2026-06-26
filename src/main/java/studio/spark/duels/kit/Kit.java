package studio.spark.duels.kit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Kit {

    private final String name;
    private final Material icon;
    private ItemStack helmet, chestplate, leggings, boots, offhand;
    private final ItemStack[] contents = new ItemStack[36];

    public Kit(String name, Material icon) {
        this.name = name;
        this.icon = icon == null ? Material.IRON_SWORD : icon;
    }

    public String name() { return name; }
    public Material icon() { return icon; }

    public void helmet(ItemStack i) { this.helmet = i; }
    public void chestplate(ItemStack i) { this.chestplate = i; }
    public void leggings(ItemStack i) { this.leggings = i; }
    public void boots(ItemStack i) { this.boots = i; }
    public void offhand(ItemStack i) { this.offhand = i; }
    public void setSlot(int slot, ItemStack i) { if (slot >= 0 && slot < 36) contents[slot] = i; }

    /** Place an item in the first free slot (0..35). */
    public void add(ItemStack i) {
        for (int s = 0; s < 36; s++) {
            if (contents[s] == null) { contents[s] = i; return; }
        }
    }

    public void apply(Player p) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        ItemStack[] copy = new ItemStack[36];
        for (int i = 0; i < 36; i++) copy[i] = contents[i] == null ? null : contents[i].clone();
        inv.setStorageContents(copy);
        inv.setHelmet(clone(helmet));
        inv.setChestplate(clone(chestplate));
        inv.setLeggings(clone(leggings));
        inv.setBoots(clone(boots));
        inv.setItemInOffHand(clone(offhand));
        p.updateInventory();
    }

    private ItemStack clone(ItemStack i) { return i == null ? null : i.clone(); }
}
