package studio.spark.duels.queue;

import org.bukkit.entity.Player;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.model.Mode;
import studio.spark.duels.util.Ctx;

import java.util.*;

public class QueueManager {

    private final SparkDuels plugin;
    private final Map<String, LinkedList<UUID>> queues = new HashMap<>();
    private final Map<UUID, String> playerMode = new HashMap<>();
    private final Map<UUID, Long> joinTime = new HashMap<>();

    public QueueManager(SparkDuels plugin) {
        this.plugin = plugin;
    }

    public boolean isQueued(Player p) { return playerMode.containsKey(p.getUniqueId()); }

    public void join(Player p, String modeId) {
        if (plugin.matches().isInMatch(p)) {
            plugin.msg().send(p, "duel.already-in-match");
            return;
        }
        Mode mode = plugin.modes().get(modeId);
        if (mode == null || !mode.duel()) {
            plugin.msg().send(p, "queue.invalid-mode", Ctx.of().put("duel_mode", modeId));
            return;
        }
        leaveSilent(p);

        String key = mode.id().toLowerCase(Locale.ROOT);
        LinkedList<UUID> q = queues.computeIfAbsent(key, k -> new LinkedList<>());

        // try to pair with a waiting, valid opponent
        while (!q.isEmpty()) {
            UUID otherId = q.peekFirst();
            Player other = plugin.getServer().getPlayer(otherId);
            if (other != null && other.isOnline() && !plugin.matches().isInMatch(other)
                    && !otherId.equals(p.getUniqueId())) {
                q.pollFirst();
                clear(otherId);
                plugin.matches().start(other, p, mode);
                return;
            }
            q.pollFirst();
            clear(otherId);
        }

        q.addLast(p.getUniqueId());
        playerMode.put(p.getUniqueId(), key);
        joinTime.put(p.getUniqueId(), System.currentTimeMillis());
        plugin.msg().send(p, "queue.joined", Ctx.of().put("queue_mode", mode.display()));
        plugin.sounds().play(p, "queue-join");
    }

    public void leave(Player p, boolean announce) {
        if (!isQueued(p)) {
            if (announce) plugin.msg().send(p, "queue.not-queued");
            return;
        }
        clear(p.getUniqueId());
        if (announce) plugin.msg().send(p, "queue.left");
    }

    public void leaveSilent(Player p) { clear(p.getUniqueId()); }

    private void clear(UUID id) {
        String key = playerMode.remove(id);
        joinTime.remove(id);
        if (key != null) {
            LinkedList<UUID> q = queues.get(key);
            if (q != null) q.remove(id);
        }
    }

    // ---- placeholders ----
    public Mode modeOf(Player p) {
        String key = playerMode.get(p.getUniqueId());
        return key == null ? null : plugin.modes().get(key);
    }

    public int positionOf(Player p) {
        String key = playerMode.get(p.getUniqueId());
        if (key == null) return 0;
        LinkedList<UUID> q = queues.get(key);
        return q == null ? 0 : q.indexOf(p.getUniqueId()) + 1;
    }

    public int playersIn(Player p) {
        String key = playerMode.get(p.getUniqueId());
        if (key == null) return 0;
        LinkedList<UUID> q = queues.get(key);
        return q == null ? 0 : q.size();
    }

    public String waitTime(Player p) {
        Long t = joinTime.get(p.getUniqueId());
        if (t == null) return "0s";
        return ((System.currentTimeMillis() - t) / 1000L) + "s";
    }
}
