package studio.spark.duels.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.arena.Arena;
import studio.spark.duels.gui.DuelMenu;
import studio.spark.duels.kit.Kit;
import studio.spark.duels.model.Mode;
import studio.spark.duels.stats.PlayerStats;
import studio.spark.duels.util.Ctx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Commands implements CommandExecutor, TabCompleter {

    private final SparkDuels plugin;

    public Commands(SparkDuels plugin) { this.plugin = plugin; }

    private Player pl(CommandSender s) { return (s instanceof Player p) ? p : null; }
    private void soon(CommandSender s) {
        plugin.msg().sendRaw(s, plugin.msg().prefix() + "<gray>ᴄᴏᴍɪɴɢ sᴏᴏɴ <dark_gray>(ᴘʜᴀsᴇ 3)", Ctx.of());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] a) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        Player p = pl(sender);

        switch (cmd) {
            case "duel" -> duel(sender, p, a);
            case "queue" -> {
                if (p == null) return true;
                if (a.length < 1) { plugin.msg().send(p, "queue.invalid-mode", Ctx.of().put("duel_mode", "?")); return true; }
                if (a[0].equalsIgnoreCase("leave")) plugin.queue().leave(p, true);
                else plugin.queue().join(p, a[0]);
            }
            case "stats" -> stats(sender, p, a);
            case "party" -> party(sender, p, a);
            case "kit" -> kit(sender, p, a);
            case "modekit" -> modekit(sender, a);
            case "setlobby" -> {
                if (p == null) { plugin.msg().send(sender, "general.players-only"); return true; }
                plugin.lobby().setSpawn(p.getLocation());
                plugin.msg().send(p, "lobby.set", Ctx.of().put("size", plugin.lobby().size()));
            }
            case "arena" -> arena(sender, p, a);
            case "spos1", "spos2", "scorner1", "scorner2" -> arenaPoint(sender, p, cmd);
            case "sparkduels" -> admin(sender, a);
            case "ffa" -> ffa(sender, p, a);
            case "ffarena" -> ffarena(sender, p, a);
            case "kiteditor" -> {
                if (p == null) { plugin.msg().send(sender, "general.players-only"); return true; }
                if (a.length < 1) { plugin.msg().send(p, "kit.not-found", Ctx.of().put("kit", "?")); return true; }
                plugin.kitEditor().open(p, a[0]);
            }
            case "spduplicate" -> {
                if (p == null) { plugin.msg().send(sender, "general.players-only"); return true; }
                if (a.length < 1) { plugin.msg().send(p, "arena.not-found", Ctx.of().put("arena", "?")); return true; }
                plugin.dup().copy(p, a[0]);
            }
            case "sparena" -> {
                if (p == null) { plugin.msg().send(sender, "general.players-only"); return true; }
                if (a.length >= 2 && a[0].equalsIgnoreCase("paste")) plugin.dup().paste(p, a[1]);
                else plugin.msg().send(p, "arena.no-clipboard");
            }
            default -> { return false; }
        }
        return true;
    }

    private void duel(CommandSender sender, Player p, String[] a) {
        if (p == null) return;
        if (a.length == 0) { plugin.msg().send(p, "general.unknown-command"); return; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "accept", "deny" -> {
                if (a.length < 2) { plugin.msg().send(p, "duel.no-request"); return; }
                Player other = Bukkit.getPlayerExact(a[1]);
                if (other == null) { plugin.msg().send(p, "general.player-not-found", Ctx.of().put("target", a[1])); return; }
                if (sub.equals("accept")) plugin.duels().accept(p, other);
                else plugin.duels().deny(p, other);
            }
            case "leave" -> {
                if (plugin.matches().isInMatch(p)) { plugin.matches().handleLeave(p); plugin.msg().send(p, "duel.left"); }
                else if (plugin.queue().isQueued(p)) plugin.queue().leave(p, true);
                else plugin.msg().send(p, "queue.not-queued");
            }
            case "top" -> studio.spark.duels.gui.LeaderboardMenu.open(plugin, p, false);
            default -> {
                Player target = Bukkit.getPlayerExact(a[0]);
                if (target == null) { plugin.msg().send(p, "general.player-not-found", Ctx.of().put("target", a[0])); return; }
                if (target.equals(p)) { plugin.msg().send(p, "duel.self"); return; }
                DuelMenu.open(plugin, p, target);
            }
        }
    }

    private void top(Player p, boolean ffa) {
        plugin.msg().sendRaw(p, "<gradient:#3CFF7E:#11A65B><bold>⚔ ᴛᴏᴘ ᴘʟᴀʏᴇʀs</bold></gradient>", Ctx.of());
        int rank = 1;
        for (Map.Entry<UUID, PlayerStats> e : plugin.stats().top(10, ffa)) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            String name = op.getName() == null ? "?" : op.getName();
            int val = ffa ? e.getValue().ffaKills : e.getValue().wins;
            plugin.msg().sendRaw(p, "<gray>#" + rank + " <white>" + name + " <dark_gray>- <#3CFF7E>" + val, Ctx.of());
            rank++;
        }
    }

    private void stats(CommandSender sender, Player p, String[] a) {
        OfflinePlayer target;
        if (a.length >= 1) target = Bukkit.getOfflinePlayer(a[0]);
        else if (p != null) target = p;
        else { plugin.msg().send(sender, "general.players-only"); return; }

        PlayerStats s = plugin.stats().get(target.getUniqueId());
        Ctx ctx = Ctx.of()
                .put("target", target.getName() == null ? "?" : target.getName())
                .put("wins", s.wins).put("losses", s.losses).put("winrate", s.winrate())
                .put("kills", s.kills).put("deaths", s.deaths).put("kdr", String.format("%.2f", s.kdr()))
                .put("streak", s.streak).put("best_streak", s.bestStreak)
                .put("elo", s.elo).put("coins", s.coins);
        plugin.msg().sendRaw(sender, plugin.msg().raw("stats.header"), ctx);
        plugin.msg().sendRaw(sender, plugin.msg().raw("stats.line-record"), ctx);
        plugin.msg().sendRaw(sender, plugin.msg().raw("stats.line-combat"), ctx);
        plugin.msg().sendRaw(sender, plugin.msg().raw("stats.line-streak"), ctx);
        plugin.msg().sendRaw(sender, plugin.msg().raw("stats.line-elo"), ctx);
    }

    private void party(CommandSender sender, Player p, String[] a) {
        if (p == null) return;
        if (a.length < 1) { plugin.msg().send(p, "party.not-in-party"); return; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> plugin.parties().create(p);
            case "leave" -> plugin.parties().leave(p);
            case "disband" -> plugin.parties().disband(p);
            case "list" -> plugin.parties().list(p);
            case "chat" -> {
                if (a.length < 2) return;
                plugin.parties().chat(p, String.join(" ", java.util.Arrays.copyOfRange(a, 1, a.length)));
            }
            case "invite", "accept", "deny", "kick" -> {
                if (a.length < 2) { plugin.msg().send(p, "general.player-not-found", Ctx.of().put("target", "?")); return; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { plugin.msg().send(p, "general.player-not-found", Ctx.of().put("target", a[1])); return; }
                switch (sub) {
                    case "invite" -> plugin.parties().invite(p, t);
                    case "accept" -> plugin.parties().accept(p, t);
                    case "deny" -> plugin.parties().deny(p, t);
                    case "kick" -> plugin.parties().kick(p, t);
                }
            }
            case "duel" -> plugin.parties().startDuel(p, a.length >= 2 ? a[1] : firstDuelMode());
            case "ffa" -> plugin.parties().startFfa(p, a.length >= 2 ? a[1] : firstFfaMode());
            case "menu" -> studio.spark.duels.gui.PartyMenu.open(plugin, p);
            default -> plugin.msg().send(p, "party.not-in-party");
        }
    }

    private void kit(CommandSender sender, Player p, String[] a) {
        if (a.length < 1) { plugin.msg().send(sender, "kit.list", Ctx.of().put("count", 0).put("list", "")); return; }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                List<String> names = new ArrayList<>();
                plugin.kits().all().forEach(k -> names.add(k.name()));
                plugin.msg().send(sender, "kit.list", Ctx.of().put("count", names.size()).put("list", String.join(", ", names)));
            }
            case "give" -> {
                if (a.length < 3) { plugin.msg().send(sender, "kit.not-found", Ctx.of().put("kit", "?")); return; }
                Player t = Bukkit.getPlayerExact(a[1]);
                Kit kit = plugin.kits().get(a[2]);
                if (t == null) { plugin.msg().send(sender, "general.player-not-found", Ctx.of().put("target", a[1])); return; }
                if (kit == null) { plugin.msg().send(sender, "kit.not-found", Ctx.of().put("kit", a[2])); return; }
                kit.apply(t);
                plugin.msg().send(sender, "kit.given", Ctx.of().put("kit", kit.name()).target(t));
            }
            case "preview" -> {
                if (p == null) { plugin.msg().send(sender, "general.players-only"); return; }
                if (a.length < 2) { plugin.msg().send(p, "kit.not-found", Ctx.of().put("kit", "?")); return; }
                Kit kit = plugin.kits().get(a[1]);
                if (kit == null) { plugin.msg().send(p, "kit.not-found", Ctx.of().put("kit", a[1])); return; }
                Inventory inv = Bukkit.createInventory(null, 54,
                        plugin.msg().parse("<gradient:#3CFF7E:#11A65B>" + kit.name() + "</gradient>"));
                kit.apply(p); // load into player to snapshot then show
                inv.setContents(p.getInventory().getContents());
                p.openInventory(inv);
            }
            case "create" -> {
                if (p == null) { plugin.msg().send(sender, "general.players-only"); return; }
                if (a.length < 2) { plugin.msg().send(p, "kit.not-found", Ctx.of().put("kit", "?")); return; }
                plugin.kitEditor().createFromInventory(p, a[1]);
            }
            case "delete" -> {
                if (a.length < 2) { plugin.msg().send(sender, "kit.not-found", Ctx.of().put("kit", "?")); return; }
                boolean ok = plugin.kitSerializer().deleteKit(a[1]);
                plugin.msg().send(sender, ok ? "kit.deleted" : "kit.not-found", Ctx.of().put("kit", a[1]));
            }
            default -> plugin.msg().send(sender, "kit.list", Ctx.of().put("count", plugin.kits().all().size()).put("list", ""));
        }
    }

    private void modekit(CommandSender sender, String[] a) {
        if (a.length < 2) { plugin.msg().send(sender, "kit.not-found", Ctx.of().put("kit", "?")); return; }
        Mode mode = plugin.modes().get(a[0]);
        Kit kit = plugin.kits().get(a[1]);
        if (mode == null) { plugin.msg().send(sender, "queue.invalid-mode", Ctx.of().put("duel_mode", a[0])); return; }
        if (kit == null) { plugin.msg().send(sender, "kit.not-found", Ctx.of().put("kit", a[1])); return; }
        plugin.modesConfig().set("modes." + mode.id() + ".kit", kit.name());
        plugin.saveModesConfig();
        plugin.modes().load();
        plugin.msg().send(sender, "kit.linked", Ctx.of().put("kit", kit.name()).put("duel_mode", mode.display()));
    }

    private void arena(CommandSender sender, Player p, String[] a) {
        if (a.length < 1) { plugin.msg().send(sender, "arena.no-setup"); return; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            List<String> names = new ArrayList<>();
            plugin.arenas().all().forEach(ar -> names.add(ar.name() + (ar.isComplete() ? "" : "*")));
            plugin.msg().send(sender, "arena.list", Ctx.of().put("list", names.isEmpty() ? "none" : String.join(", ", names)));
            return;
        }
        if (a.length < 2) { plugin.msg().send(sender, "arena.not-found", Ctx.of().put("arena", "?")); return; }
        String name = a[1];
        switch (sub) {
            case "create" -> {
                if (p == null) { plugin.msg().send(sender, "general.players-only"); return; }
                plugin.arenas().create(name);
                plugin.arenas().beginSetup(p.getUniqueId(), name);
                plugin.msg().send(p, "arena.setup-started", Ctx.of().put("arena", name));
            }
            case "delete" -> {
                boolean ok = plugin.arenas().delete(name);
                plugin.msg().send(sender, ok ? "arena.deleted" : "arena.not-found", Ctx.of().put("arena", name));
            }
            case "enable", "disable" -> {
                Arena ar = plugin.arenas().get(name);
                if (ar == null) { plugin.msg().send(sender, "arena.not-found", Ctx.of().put("arena", name)); return; }
                ar.enabled(sub.equals("enable"));
                plugin.arenas().save();
                plugin.msg().send(sender, sub.equals("enable") ? "arena.enabled" : "arena.disabled", Ctx.of().put("arena", name));
            }
            default -> plugin.msg().send(sender, "arena.no-setup");
        }
    }

    private void arenaPoint(CommandSender sender, Player p, String cmd) {
        if (p == null) { plugin.msg().send(sender, "general.players-only"); return; }
        Arena ar = plugin.arenas().currentSetup(p.getUniqueId());
        if (ar == null) { plugin.msg().send(p, "arena.no-setup"); return; }
        switch (cmd) {
            case "spos1" -> { ar.spawn1(p.getLocation()); plugin.arenas().save(); plugin.msg().send(p, "arena.pos1-set"); }
            case "spos2" -> { ar.spawn2(p.getLocation()); plugin.arenas().save(); plugin.msg().send(p, "arena.pos2-set"); }
            case "scorner1" -> { ar.corner1(p.getLocation()); plugin.arenas().save(); plugin.msg().send(p, "arena.corner1-set"); }
            case "scorner2" -> {
                ar.corner2(p.getLocation());
                ar.enabled(true);
                plugin.arenas().save();
                plugin.msg().send(p, "arena.corner2-set", Ctx.of().put("arena", ar.name()));
            }
        }
    }

    private void admin(CommandSender sender, String[] a) {
        if (a.length >= 1 && a[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            plugin.msg().send(sender, "general.reloaded");
            if (sender instanceof Player pp) plugin.sounds().play(pp, "reload");
        } else if (a.length >= 1 && a[0].equalsIgnoreCase("help")) {
            plugin.msg().sendList(sender, "help", Ctx.of());
        } else {
            plugin.msg().sendList(sender, "info", Ctx.of());
        }
    }

    private String firstDuelMode() {
        return plugin.modes().duelModes().stream().findFirst().map(Mode::id).orElse("");
    }

    private String firstFfaMode() {
        return plugin.modes().all().stream().filter(Mode::ffa).findFirst().map(Mode::id).orElse("");
    }

    private void ffa(CommandSender sender, Player p, String[] a) {
        if (p == null) { plugin.msg().send(sender, "general.players-only"); return; }
        if (a.length < 1) { plugin.msg().send(p, "ffa.invalid-mode", Ctx.of().put("ffa_mode", "?")); return; }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "leave" -> plugin.ffa().leave(p);
            case "top" -> studio.spark.duels.gui.LeaderboardMenu.open(plugin, p, true);
            default -> plugin.ffa().join(p, a[0]);
        }
    }

    private void ffarena(CommandSender sender, Player p, String[] a) {
        if (a.length < 1) { plugin.msg().send(sender, "arena.no-setup"); return; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            List<String> names = new ArrayList<>();
            plugin.ffa().all().forEach(ar -> names.add(ar.name() + "(" + ar.spawns().size() + ")"));
            plugin.msg().send(sender, "ffa.list", Ctx.of().put("list", names.isEmpty() ? "none" : String.join(", ", names)));
            return;
        }
        if (a.length < 2) { plugin.msg().send(sender, "ffa.arena-not-found", Ctx.of().put("arena", "?")); return; }
        String name = a[1];
        if (sub.equals("create")) {
            plugin.ffa().create(name);
            plugin.msg().send(sender, "ffa.created", Ctx.of().put("arena", name));
            return;
        }
        var arena = plugin.ffa().get(name);
        if (arena == null) { plugin.msg().send(sender, "ffa.arena-not-found", Ctx.of().put("arena", name)); return; }
        switch (sub) {
            case "addspawn" -> {
                if (p == null) { plugin.msg().send(sender, "general.players-only"); return; }
                plugin.ffa().addSpawn(arena, p.getLocation());
                plugin.msg().send(p, "ffa.spawn-added", Ctx.of().put("arena", name).put("count", arena.spawns().size()));
            }
            case "setkit" -> {
                if (a.length < 3 || plugin.kits().get(a[2]) == null) { plugin.msg().send(sender, "kit.not-found", Ctx.of().put("kit", a.length < 3 ? "?" : a[2])); return; }
                plugin.ffa().setKit(arena, plugin.kits().get(a[2]).name());
                plugin.msg().send(sender, "ffa.kit-set", Ctx.of().put("arena", name).put("kit", a[2]));
            }
            case "setmode" -> {
                Mode mode = a.length < 3 ? null : plugin.modes().get(a[2]);
                if (mode == null || !mode.ffa()) { plugin.msg().send(sender, "ffa.invalid-mode", Ctx.of().put("ffa_mode", a.length < 3 ? "?" : a[2])); return; }
                plugin.ffa().setMode(arena, mode.id());
                plugin.msg().send(sender, "ffa.mode-set", Ctx.of().put("arena", name).put("ffa_mode", mode.display()));
            }
            case "corner1" -> { if (p != null) { arena.corner1(p.getLocation()); plugin.ffa().save(); plugin.msg().send(p, "arena.corner1-set"); } }
            case "corner2" -> { if (p != null) { arena.corner2(p.getLocation()); plugin.ffa().save(); plugin.msg().send(p, "arena.corner2-set", Ctx.of().put("arena", name)); } }
            case "enable", "disable" -> {
                plugin.ffa().setEnabled(arena, sub.equals("enable"));
                plugin.msg().send(sender, sub.equals("enable") ? "ffa.enabled" : "ffa.disabled", Ctx.of().put("arena", name));
            }
            case "delete" -> { plugin.ffa().delete(name); plugin.msg().send(sender, "ffa.deleted", Ctx.of().put("arena", name)); }
            default -> plugin.msg().send(sender, "arena.no-setup");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] a) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (cmd.equals("queue") && a.length == 1) {
            plugin.modes().duelModes().forEach(m -> out.add(m.id()));
            out.add("leave");
        } else if (cmd.equals("duel") && a.length == 1) {
            out.add("accept"); out.add("deny"); out.add("leave"); out.add("top");
            Bukkit.getOnlinePlayers().forEach(o -> out.add(o.getName()));
        } else if (cmd.equals("kit") && a.length == 1) {
            out.addAll(List.of("list", "give", "preview", "create", "delete"));
        } else if (cmd.equals("arena") && a.length == 1) {
            out.addAll(List.of("create", "list", "delete", "enable", "disable"));
        } else if (cmd.equals("party") && a.length == 1) {
            out.addAll(List.of("create", "invite", "accept", "deny", "leave", "disband", "kick", "list", "chat"));
        } else if (cmd.equals("sparkduels") && a.length == 1) {
            out.addAll(List.of("reload", "info", "help"));
        } else if (cmd.equals("ffa") && a.length == 1) {
            plugin.modes().all().stream().filter(Mode::ffa).forEach(m -> out.add(m.id()));
            out.add("leave"); out.add("top");
        } else if (cmd.equals("ffarena") && a.length == 1) {
            out.addAll(List.of("create", "addspawn", "setkit", "setmode", "corner1", "corner2", "enable", "disable", "delete", "list"));
        } else if (cmd.equals("kiteditor") && a.length == 1) {
            plugin.kits().all().forEach(k -> out.add(k.name()));
        } else if ((cmd.equals("modekit")) && a.length == 1) {
            plugin.modes().all().forEach(m -> out.add(m.id()));
        } else if ((cmd.equals("modekit")) && a.length == 2) {
            plugin.kits().all().forEach(k -> out.add(k.name()));
        }
        return out;
    }
}
