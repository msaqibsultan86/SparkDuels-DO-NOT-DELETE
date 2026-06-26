package studio.spark.duels.party;

import org.bukkit.entity.Player;
import studio.spark.duels.SparkDuels;
import studio.spark.duels.util.Ctx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager {

    private final SparkDuels plugin;
    private final Map<UUID, Party> byPlayer = new HashMap<>();

    public PartyManager(SparkDuels plugin) { this.plugin = plugin; }

    public Party getParty(Player p) { return byPlayer.get(p.getUniqueId()); }
    public boolean inParty(Player p) { return byPlayer.containsKey(p.getUniqueId()); }

    public Party create(Player leader) {
        if (inParty(leader)) return getParty(leader);
        Party party = new Party(leader.getUniqueId());
        byPlayer.put(leader.getUniqueId(), party);
        plugin.msg().send(leader, "party.created");
        return party;
    }

    public void invite(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null) party = create(leader);
        if (!party.isLeader(leader.getUniqueId())) { plugin.msg().send(leader, "party.not-leader"); return; }
        if (party.size() >= plugin.getConfig().getInt("party.max-size", 8)) { plugin.msg().send(leader, "party.full"); return; }
        party.invites().add(target.getUniqueId());
        plugin.msg().send(leader, "party.invited", Ctx.of().target(target));
        plugin.msg().sendRaw(target, plugin.msg().raw("party.invite-received"), Ctx.of().put("player", leader.getName()));
        plugin.sounds().play(target, "party-invite");
    }

    public void accept(Player target, Player leader) {
        Party party = getParty(leader);
        if (party == null || !party.invites().contains(target.getUniqueId())) { plugin.msg().send(target, "party.not-in-party"); return; }
        if (inParty(target)) { plugin.msg().send(target, "party.not-in-party"); return; }
        party.invites().remove(target.getUniqueId());
        party.members().add(target.getUniqueId());
        byPlayer.put(target.getUniqueId(), party);
        broadcast(party, "party.joined", Ctx.of().target(target));
    }

    public void deny(Player target, Player leader) {
        Party party = getParty(leader);
        if (party != null) party.invites().remove(target.getUniqueId());
    }

    public void leave(Player p) {
        Party party = getParty(p);
        if (party == null) { plugin.msg().send(p, "party.not-in-party"); return; }
        party.members().remove(p.getUniqueId());
        byPlayer.remove(p.getUniqueId());
        plugin.msg().send(p, "party.left");
        if (party.isLeader(p.getUniqueId()) && !party.members().isEmpty()) {
            party.leader(party.members().iterator().next());
        }
    }

    public void disband(Player p) {
        Party party = getParty(p);
        if (party == null) { plugin.msg().send(p, "party.not-in-party"); return; }
        if (!party.isLeader(p.getUniqueId())) { plugin.msg().send(p, "party.not-leader"); return; }
        broadcast(party, "party.disbanded", Ctx.of());
        for (UUID id : party.members()) byPlayer.remove(id);
    }

    public void kick(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) { plugin.msg().send(leader, "party.not-leader"); return; }
        if (!party.isMember(target.getUniqueId())) { plugin.msg().send(leader, "party.not-in-party"); return; }
        party.members().remove(target.getUniqueId());
        byPlayer.remove(target.getUniqueId());
        plugin.msg().send(target, "party.kicked");
    }

    public void chat(Player p, String message) {
        Party party = getParty(p);
        if (party == null) { plugin.msg().send(p, "party.not-in-party"); return; }
        for (UUID id : party.members()) {
            Player m = plugin.getServer().getPlayer(id);
            if (m != null) plugin.msg().sendRaw(m, plugin.msg().raw("party.chat-format"),
                    Ctx.of().put("player", p.getName()).put("message", message));
        }
    }

    public void list(Player p) {
        Party party = getParty(p);
        if (party == null) { plugin.msg().send(p, "party.not-in-party"); return; }
        StringBuilder sb = new StringBuilder();
        for (UUID id : party.members()) {
            Player m = plugin.getServer().getPlayer(id);
            if (sb.length() > 0) sb.append("<gray>, ");
            sb.append(party.isLeader(id) ? "<#3CFF7E>\u2605" : "<white>")
              .append(m != null ? m.getName() : "?");
        }
        plugin.msg().sendRaw(p, plugin.msg().prefix() + "<gray>ᴘᴀʀᴛʏ: " + sb, Ctx.of());
    }

    private void broadcast(Party party, String path, Ctx ctx) {
        for (UUID id : party.members()) {
            Player m = plugin.getServer().getPlayer(id);
            if (m != null) plugin.msg().send(m, path, ctx);
        }
    }

    /** Pair party members into 1v1 duels of the given mode. */
    public void startDuel(Player leader, String modeId) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) { plugin.msg().send(leader, "party.not-leader"); return; }
        studio.spark.duels.model.Mode mode = plugin.modes().get(modeId);
        if (mode == null || !mode.duel()) { plugin.msg().send(leader, "queue.invalid-mode", Ctx.of().put("duel_mode", modeId)); return; }
        java.util.List<UUID> members = new java.util.ArrayList<>(party.members());
        if (members.size() < 2) { plugin.msg().send(leader, "party.too-small"); return; }
        int paired = 0;
        for (int i = 0; i + 1 < members.size(); i += 2) {
            Player a = plugin.getServer().getPlayer(members.get(i));
            Player b = plugin.getServer().getPlayer(members.get(i + 1));
            if (a != null && b != null) { plugin.matches().start(a, b, mode); paired++; }
        }
        broadcast(party, "party.duel-started", Ctx.of().put("duel_mode", mode.display()).put("count", paired));
    }

    /** Send all party members into the FFA arena for the given mode. */
    public void startFfa(Player leader, String modeId) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) { plugin.msg().send(leader, "party.not-leader"); return; }
        studio.spark.duels.model.Mode mode = plugin.modes().get(modeId);
        if (mode == null || !mode.ffa()) { plugin.msg().send(leader, "ffa.invalid-mode", Ctx.of().put("ffa_mode", modeId)); return; }
        for (UUID id : party.members()) {
            Player m = plugin.getServer().getPlayer(id);
            if (m != null) plugin.ffa().join(m, modeId);
        }
        broadcast(party, "party.ffa-started", Ctx.of().put("ffa_mode", mode.display()));
    }
}
