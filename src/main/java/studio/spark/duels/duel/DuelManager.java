package studio.spark.duels.duel;

import org.bukkit.entity.Player;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.model.Mode;
import studio.spark.duels.util.Ctx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelManager {

    public record Request(UUID from, String modeId, long expiresAt) {}

    private final SparkDuels plugin;
    private final Map<UUID, Map<UUID, Request>> requests = new HashMap<>(); // target -> from -> req
    private final Map<String, Long> cooldown = new HashMap<>();             // from:target -> time

    public DuelManager(SparkDuels plugin) { this.plugin = plugin; }

    public void send(Player from, Player target, String modeId) {
        if (from.equals(target)) { plugin.msg().send(from, "duel.self"); return; }
        if (plugin.matches().isInMatch(from)) { plugin.msg().send(from, "duel.already-in-match"); return; }
        if (plugin.matches().isInMatch(target)) {
            plugin.msg().send(from, "duel.target-busy", Ctx.of().target(target));
            return;
        }
        Mode mode = plugin.modes().get(modeId);
        if (mode == null) { plugin.msg().send(from, "queue.invalid-mode", Ctx.of().put("duel_mode", modeId)); return; }

        String ckey = from.getUniqueId() + ":" + target.getUniqueId();
        long now = System.currentTimeMillis();
        long cd = plugin.getConfig().getInt("duel.request-cooldown", 3) * 1000L;
        if (cooldown.getOrDefault(ckey, 0L) > now) { plugin.msg().send(from, "general.cooldown"); return; }
        cooldown.put(ckey, now + cd);

        long expiry = plugin.getConfig().getInt("duel.request-expiry", 30) * 1000L;
        requests.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(from.getUniqueId(), new Request(from.getUniqueId(), mode.id(), now + expiry));

        plugin.msg().send(from, "duel.request-sent", Ctx.of().target(target).put("duel_mode", mode.display()));

        Ctx tctx = Ctx.of().put("player", from.getName()).put("duel_mode", mode.display());
        plugin.msg().sendRaw(target, plugin.msg().raw("duel.request-header"), tctx);
        plugin.msg().sendRaw(target, plugin.msg().raw("duel.request-body"), tctx);
        plugin.msg().sendRaw(target, plugin.msg().raw("duel.request-buttons"), tctx);
        plugin.sounds().play(target, "duel-request");
    }

    public void accept(Player target, Player from) {
        Map<UUID, Request> map = requests.get(target.getUniqueId());
        Request req = map == null ? null : map.remove(from.getUniqueId());
        if (req == null) { plugin.msg().send(target, "duel.no-request"); return; }
        if (System.currentTimeMillis() > req.expiresAt()) { plugin.msg().send(target, "duel.expired"); return; }
        if (plugin.matches().isInMatch(target) || plugin.matches().isInMatch(from)) {
            plugin.msg().send(target, "duel.target-busy", Ctx.of().target(from));
            return;
        }
        Mode mode = plugin.modes().get(req.modeId());
        if (mode == null) { plugin.msg().send(target, "queue.invalid-mode", Ctx.of().put("duel_mode", req.modeId())); return; }

        plugin.msg().send(from, "duel.accepted", Ctx.of().put("player", target.getName()));
        plugin.sounds().play(from, "duel-accept");
        plugin.sounds().play(target, "duel-accept");
        plugin.matches().start(from, target, mode);
    }

    public void deny(Player target, Player from) {
        Map<UUID, Request> map = requests.get(target.getUniqueId());
        Request req = map == null ? null : map.remove(from.getUniqueId());
        if (req == null) { plugin.msg().send(target, "duel.no-request"); return; }
        plugin.msg().send(from, "duel.denied", Ctx.of().put("player", target.getName()));
        plugin.sounds().play(target, "duel-deny");
    }
}
