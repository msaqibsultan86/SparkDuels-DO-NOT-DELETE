package studio.spark.duels.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import studio.spark.duels.SparkDuels;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Reads messages.yml and renders MiniMessage with placeholders. */
public class Msg {

    public static final MiniMessage MM = MiniMessage.miniMessage();
    private final SparkDuels plugin;

    public Msg(SparkDuels plugin) {
        this.plugin = plugin;
    }

    public String prefix() {
        return plugin.messages().getString("prefix", "");
    }

    /** Raw string from messages.yml (no prefix, no parse). */
    public String raw(String path) {
        return plugin.messages().getString(path, path);
    }

    public List<String> rawList(String path) {
        List<String> l = plugin.messages().getStringList(path);
        return l.isEmpty() ? new ArrayList<>() : l;
    }

    public Component parse(String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    public Component item(String miniMessage) {
        return MM.deserialize("<!italic>" + miniMessage);
    }

    /** Send a prefixed message from messages.yml. */
    public void send(CommandSender to, String path, Ctx ctx) {
        String text = raw(path);
        if (text == null || text.isEmpty()) return;
        Player p = (to instanceof Player) ? (Player) to : null;
        String resolved = plugin.placeholders().apply(prefix() + text, p, ctx);
        for (String line : resolved.split("\n")) {
            to.sendMessage(MM.deserialize(line));
        }
    }

    public void send(CommandSender to, String path) {
        send(to, path, Ctx.of());
    }

    /** Send without the prefix. */
    public void sendRaw(CommandSender to, String miniMessage, Ctx ctx) {
        Player p = (to instanceof Player) ? (Player) to : null;
        String resolved = plugin.placeholders().apply(miniMessage, p, ctx);
        for (String line : resolved.split("\n")) {
            to.sendMessage(MM.deserialize(line));
        }
    }

    /** Send a list path (e.g. info/help). */
    public void sendList(CommandSender to, String path, Ctx ctx) {
        Player p = (to instanceof Player) ? (Player) to : null;
        for (String line : rawList(path)) {
            to.sendMessage(MM.deserialize(plugin.placeholders().apply(line, p, ctx)));
        }
    }

    public void title(Player p, String titlePath, String subPath, Ctx ctx,
                      long inMs, long stayMs, long outMs) {
        String t = plugin.placeholders().apply(raw(titlePath), p, ctx);
        String s = plugin.placeholders().apply(raw(subPath), p, ctx);
        p.showTitle(Title.title(MM.deserialize(t), MM.deserialize(s),
                Title.Times.times(Duration.ofMillis(inMs), Duration.ofMillis(stayMs), Duration.ofMillis(outMs))));
    }

    public void actionbar(Player p, String miniMessage, Ctx ctx) {
        p.sendActionBar(MM.deserialize(plugin.placeholders().apply(miniMessage, p, ctx)));
    }
}
