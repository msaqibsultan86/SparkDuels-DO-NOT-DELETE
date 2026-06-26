package studio.spark.duels.party;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Party {
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> invites = new HashSet<>();

    public Party(UUID leader) { this.leader = leader; members.add(leader); }

    public UUID leader() { return leader; }
    public void leader(UUID id) { this.leader = id; }
    public Set<UUID> members() { return members; }
    public Set<UUID> invites() { return invites; }
    public boolean isLeader(UUID id) { return id.equals(leader); }
    public boolean isMember(UUID id) { return members.contains(id); }
    public int size() { return members.size(); }
}
