package tw.crestnetwork.rpg.dungeon;

import com.example.crestrpg.skills.PlayerProfile;
import com.example.crestrpg.skills.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import tw.crestnetwork.rpg.CrestRpgPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RpgDungeonEngine implements Listener {
    private final CrestRpgPlugin plugin;
    private final Map<String, RpgDungeonInstance> activeDungeons = new ConcurrentHashMap<>();

    public RpgDungeonEngine(CrestRpgPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickDungeons, 20L, 20L);
    }

    public RpgDungeonInstance startDungeon(String dungeonId, String name, List<Player> team) {
        RpgDungeonInstance instance = new RpgDungeonInstance(dungeonId, name, team, 600); // 10 mins
        activeDungeons.put(dungeonId, instance);
        return instance;
    }

    private void tickDungeons() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, RpgDungeonInstance> entry : activeDungeons.entrySet()) {
            RpgDungeonInstance dungeon = entry.getValue();
            dungeon.decrementTime();

            if (dungeon.getRemainingSeconds() <= 0) {
                broadcastDungeonMessage(dungeon, Component.text("⏰ 副本時間到！挑戰失敗。", NamedTextColor.RED));
                dungeon.destroy();
                toRemove.add(entry.getKey());
            }
        }
        for (String id : toRemove) {
            activeDungeons.remove(id);
        }
    }

    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity boss)) return;

        for (RpgDungeonInstance dungeon : activeDungeons.values()) {
            if (boss.equals(dungeon.getBossEntity())) {
                double hpPercent = boss.getHealth() / boss.getMaxHealth();

                // Phase 2 Trigger (70% HP)
                if (hpPercent <= 0.70 && dungeon.getCurrentPhase() == 1) {
                    dungeon.setCurrentPhase(2);
                    broadcastDungeonMessage(dungeon, Component.text("⚡ [Boss 狂暴預警] " + dungeon.getName() + " 進入第二階段！攻擊力提升！", NamedTextColor.RED));
                    boss.getWorld().spawnParticle(Particle.FLAME, boss.getLocation(), 50, 0.5, 1.0, 0.5, 0.1);
                    boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.9f);
                }

                // Phase 3 Trigger (30% HP)
                if (hpPercent <= 0.30 && dungeon.getCurrentPhase() == 2) {
                    dungeon.setCurrentPhase(3);
                    broadcastDungeonMessage(dungeon, Component.text("🔥 [Boss 終極暴走] " + dungeon.getName() + " 進入狂暴終極階段！", NamedTextColor.DARK_RED));
                    boss.getWorld().spawnParticle(Particle.EXPLOSION, boss.getLocation(), 10, 0.5, 1.0, 0.5, 0.1);
                    boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
                }
            }
        }
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity boss = event.getEntity();
        List<String> completed = new ArrayList<>();

        for (Map.Entry<String, RpgDungeonInstance> entry : activeDungeons.entrySet()) {
            RpgDungeonInstance dungeon = entry.getValue();
            if (boss.equals(dungeon.getBossEntity())) {
                broadcastDungeonMessage(dungeon, Component.text("🎉 [副本通關] 恭喜擊敗 Boss，成功通關副本: " + dungeon.getName() + "！", NamedTextColor.GOLD));

                for (UUID uid : dungeon.getPlayers()) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p != null && p.isOnline()) {
                        p.giveExp(500);
                        PlayerProfile profile = plugin.getProfileManager().getProfile(p.getUniqueId());
                        if (profile != null) {
                            profile.addXp(SkillType.COMBAT, 500, p);
                        }
                        p.sendMessage(Component.text("🎁 獲得通關獎勵: +500 EXP！", NamedTextColor.GREEN));
                        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                }

                dungeon.destroy();
                completed.add(entry.getKey());
            }
        }
        for (String id : completed) {
            activeDungeons.remove(id);
        }
    }

    private void broadcastDungeonMessage(RpgDungeonInstance dungeon, Component message) {
        for (UUID uid : dungeon.getPlayers()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }
}
