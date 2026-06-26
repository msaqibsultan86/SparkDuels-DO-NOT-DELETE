package studio.spark.duels.kit;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.util.Ctx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Sandboxed kit editor: edit in creative, close to save (anti-dupe). */
public class KitEditor {

    private final SparkDuels plugin;
    private final Map<UUID, String> editing = new HashMap<>();

    public KitEditor(SparkDuels plugin) { this.plugin = plugin; }

    public boolean isEditing(Player p) { return editing.containsKey(p.getUniqueId()); }

    public void open(Player p, String kitName) {
        if (plugin.matches().isInMatch(p) || plugin.ffa().isInFfa(p)) {
            plugin.msg().send(p, "duel.already-in-match");
            return;
        }
        editing.put(p.getUniqueId(), kitName);
        PlayerInventory inv = p.getInventory();
        inv.clear();
        Kit existing = plugin.kits().get(kitName);
        if (existing != null) existing.apply(p);
        p.setGameMode(GameMode.CREATIVE);
        plugin.msg().send(p, "kit.editor-open", Ctx.of().put("kit", kitName));
        p.openInventory(p.getInventory());
    }

    /** Called from InventoryCloseEvent. */
    public void save(Player p) {
        String name = editing.remove(p.getUniqueId());
        if (name == null) return;
        PlayerInventory inv = p.getInventory();
        Material icon = inv.getItemInMainHand().getType() == Material.AIR
                ? Material.IRON_SWORD : inv.getItemInMainHand().getType();
        plugin.kitSerializer().writeKit(name, icon,
                inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots(),
                inv.getItemInOffHand(), inv.getStorageContents());
        plugin.msg().send(p, "kit.saved", Ctx.of().put("kit", name));
        plugin.sounds().play(p, "success");
        plugin.lobby().toLobby(p);
    }

    /** Quick create from the player's current inventory (no editor). */
    public void createFromInventory(Player p, String name) {
        PlayerInventory inv = p.getInventory();
        Material icon = inv.getItemInMainHand().getType() == Material.AIR
                ? Material.IRON_SWORD : inv.getItemInMainHand().getType();
        plugin.kitSerializer().writeKit(name, icon,
                inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots(),
                inv.getItemInOffHand(), inv.getStorageContents());
        plugin.msg().send(p, "kit.created", Ctx.of().put("kit", name));
    }
}
