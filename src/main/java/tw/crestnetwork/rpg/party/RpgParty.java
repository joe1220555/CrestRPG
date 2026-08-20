package tw.crestnetwork.rpg.party;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class RpgParty {
    private final UUID id;
    private UUID leader;
    private final List<UUID> members = Collections.synchronizedList(new ArrayList<>());
    private String lootMode = "FREE_FOR_ALL"; // FREE_FOR_ALL, ROUND_ROBIN, LEADER_ONLY

    public RpgParty(Player leader) {
        this.id = UUID.randomUUID();
        this.leader = leader.getUniqueId();
        this.members.add(leader.getUniqueId());
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public boolean isMember(UUID playerUuid) {
        return members.contains(playerUuid);
    }

    public boolean isLeader(UUID playerUuid) {
        return leader.equals(playerUuid);
    }

    public void addMember(UUID playerUuid) {
        if (!members.contains(playerUuid)) {
            members.add(playerUuid);
        }
    }

    public void removeMember(UUID playerUuid) {
        members.remove(playerUuid);
        if (leader.equals(playerUuid) && !members.isEmpty()) {
            leader = members.get(0);
        }
    }

    public String getLootMode() {
        return lootMode;
    }

    public void setLootMode(String lootMode) {
        this.lootMode = lootMode;
    }
}
