package tw.crestnetwork.rpg.dungeon;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RpgDungeonInstance {
    private final String dungeonId;
    private final String name;
    private final List<UUID> players = new ArrayList<>();
    private final BossBar bossBar;
    private int remainingSeconds;
    private LivingEntity bossEntity;
    private int currentPhase = 1;

    public RpgDungeonInstance(String dungeonId, String name, List<Player> team, int timeLimitSeconds) {
        this.dungeonId = dungeonId;
        this.name = name;
        this.remainingSeconds = timeLimitSeconds;
        for (Player p : team) {
            this.players.add(p.getUniqueId());
        }
        this.bossBar = BossBar.bossBar(
                Component.text("🏰 副本: " + name + " [剩餘時間: " + timeLimitSeconds + "s]", NamedTextColor.RED),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS
        );

        for (Player p : team) {
            p.showBossBar(bossBar);
            p.sendMessage(Component.text("----------------------------------------", NamedTextColor.DARK_GRAY));
            p.sendMessage(Component.text("🏰 您已進入副本: " + name, NamedTextColor.GOLD));
            p.sendMessage(Component.text("⏰ 限時 " + (timeLimitSeconds / 60) + " 分鐘！請擊敗守護 Boss 通關！", NamedTextColor.YELLOW));
            p.sendMessage(Component.text("----------------------------------------", NamedTextColor.DARK_GRAY));
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
        }
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public String getName() {
        return name;
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void decrementTime() {
        if (remainingSeconds > 0) {
            remainingSeconds--;
            float progress = Math.max(0.0f, (float) remainingSeconds / 600.0f);
            bossBar.progress(progress);
            bossBar.name(Component.text("🏰 副本: " + name + " [剩餘時間: " + remainingSeconds + "s]", NamedTextColor.RED));
        }
    }

    public LivingEntity getBossEntity() {
        return bossEntity;
    }

    public void setBossEntity(LivingEntity bossEntity) {
        this.bossEntity = bossEntity;
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(int phase) {
        this.currentPhase = phase;
    }

    public void destroy() {
        for (UUID uid : players) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                p.hideBossBar(bossBar);
            }
        }
    }
}
