package tw.crestnetwork.rpg.party;

import com.example.crestrpg.skills.PlayerProfile;
import com.example.crestrpg.skills.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import tw.crestnetwork.rpg.CrestRpgPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RpgPartyManager implements Listener {
    private final CrestRpgPlugin plugin;
    private final Map<UUID, RpgParty> playerParties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>(); // Invited -> Leader

    public RpgPartyManager(CrestRpgPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public RpgParty getParty(Player player) {
        return playerParties.get(player.getUniqueId());
    }

    public RpgParty createParty(Player leader) {
        if (playerParties.containsKey(leader.getUniqueId())) {
            return playerParties.get(leader.getUniqueId());
        }
        RpgParty party = new RpgParty(leader);
        playerParties.put(leader.getUniqueId(), party);
        leader.sendMessage(Component.text("[隊伍系統] 已成功建立隊伍！您是隊長。", NamedTextColor.GREEN));
        return party;
    }

    public void invitePlayer(Player leader, Player target) {
        RpgParty party = getParty(leader);
        if (party == null) {
            party = createParty(leader);
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(Component.text("[隊伍系統] 只有隊長可以發送組隊邀請！", NamedTextColor.RED));
            return;
        }
        if (playerParties.containsKey(target.getUniqueId())) {
            leader.sendMessage(Component.text("[隊伍系統] 該玩家已經在其他隊伍中！", NamedTextColor.YELLOW));
            return;
        }

        pendingInvites.put(target.getUniqueId(), leader.getUniqueId());
        leader.sendMessage(Component.text("[隊伍系統] 已向 " + target.getName() + " 發送組隊邀請！", NamedTextColor.AQUA));
        target.sendMessage(Component.text("[隊伍系統] 玩家 " + leader.getName() + " 邀請您加入隊伍！輸入 /party accept 接受邀請。", NamedTextColor.GOLD));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    public void acceptInvite(Player player) {
        UUID leaderUuid = pendingInvites.remove(player.getUniqueId());
        if (leaderUuid == null) {
            player.sendMessage(Component.text("[隊伍系統] 您目前沒有收到任何組隊邀請！", NamedTextColor.RED));
            return;
        }
        Player leader = Bukkit.getPlayer(leaderUuid);
        if (leader == null || !leader.isOnline()) {
            player.sendMessage(Component.text("[隊伍系統] 邀請發送者已離線。", NamedTextColor.RED));
            return;
        }
        RpgParty party = getParty(leader);
        if (party == null) {
            party = createParty(leader);
        }
        party.addMember(player.getUniqueId());
        playerParties.put(player.getUniqueId(), party);

        broadcastPartyMessage(party, Component.text("[隊伍系統] 玩家 " + player.getName() + " 加入了隊伍！", NamedTextColor.GREEN));
    }

    public void leaveParty(Player player) {
        RpgParty party = playerParties.remove(player.getUniqueId());
        if (party == null) {
            player.sendMessage(Component.text("[隊伍系統] 您目前不在任何隊伍中。", NamedTextColor.RED));
            return;
        }
        party.removeMember(player.getUniqueId());
        player.sendMessage(Component.text("[隊伍系統] 您已離開隊伍。", NamedTextColor.YELLOW));

        if (party.getMembers().isEmpty()) {
            playerParties.values().removeIf(p -> p.getId().equals(party.getId()));
        } else {
            broadcastPartyMessage(party, Component.text("[隊伍系統] 玩家 " + player.getName() + " 離開了隊伍。", NamedTextColor.YELLOW));
        }
    }

    public void broadcastPartyMessage(RpgParty party, Component message) {
        for (UUID memberUuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    public void distributePartyExp(Player killer, int baseExp) {
        RpgParty party = getParty(killer);
        if (party == null || party.getMembers().size() <= 1) {
            giveExp(killer, baseExp);
            return;
        }

        double partyBonus = 1.0 + (party.getMembers().size() * 0.15); // +15% per member
        int totalExp = (int) Math.round(baseExp * partyBonus);
        int sharedExp = Math.max(1, totalExp / party.getMembers().size());

        for (UUID memberUuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline() && p.getWorld().equals(killer.getWorld()) && p.getLocation().distanceSquared(killer.getLocation()) <= 2304) {
                giveExp(p, sharedExp);
                p.sendActionBar(Component.text("👥 [隊伍共享] +" + sharedExp + " EXP (隊伍加成 +" + Math.round((partyBonus - 1) * 100) + "%)", NamedTextColor.GREEN));
            }
        }
    }

    private void giveExp(Player player, int amount) {
        player.giveExp(amount);
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile != null) {
            profile.addXp(SkillType.COMBAT, amount, player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingInvites.remove(event.getPlayer().getUniqueId());
    }
}
