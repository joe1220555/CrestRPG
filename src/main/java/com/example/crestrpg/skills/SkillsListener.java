package com.example.crestrpg.skills;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SkillsListener implements Listener {

    private final CrestRpgPlugin plugin;

    public SkillsListener(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getProfileManager().loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getProfileManager().unloadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        Block block = event.getBlock();
        Material mat = block.getType();
        String matName = mat.name();

        // 1. MINING XP
        if (matName.contains("ORE") || mat == Material.STONE || mat == Material.DEEPSLATE || mat == Material.NETHERRACK || mat == Material.AMETHYST_CLUSTER) {
            double xp = 1.0;
            switch (mat) {
                case DIAMOND_ORE:
                case DEEPSLATE_DIAMOND_ORE:
                case EMERALD_ORE:
                case DEEPSLATE_EMERALD_ORE:
                    xp = 50.0;
                    break;
                case ANCIENT_DEBRIS:
                    xp = 100.0;
                    break;
                case GOLD_ORE:
                case DEEPSLATE_GOLD_ORE:
                case NETHER_GOLD_ORE:
                case LAPIS_ORE:
                case DEEPSLATE_LAPIS_ORE:
                    xp = 20.0;
                    break;
                case IRON_ORE:
                case DEEPSLATE_IRON_ORE:
                case REDSTONE_ORE:
                case DEEPSLATE_REDSTONE_ORE:
                    xp = 15.0;
                    break;
                case COAL_ORE:
                case DEEPSLATE_COAL_ORE:
                case COPPER_ORE:
                case DEEPSLATE_COPPER_ORE:
                case NETHER_QUARTZ_ORE:
                    xp = 8.0;
                    break;
                case STONE:
                case DEEPSLATE:
                case NETHERRACK:
                case AMETHYST_CLUSTER:
                    xp = 1.0;
                    break;
            }
            profile.addXp(SkillType.MINING, xp, player);
            return;
        }

        // 2. FORAGING XP
        if (matName.contains("LOG") || matName.contains("WOOD") || matName.contains("STEM")) {
            profile.addXp(SkillType.FORAGING, 10.0, player);
            return;
        }
        if (matName.contains("LEAVES") || matName.contains("SAPLING")) {
            profile.addXp(SkillType.FORAGING, 0.5, player);
            return;
        }

        // 3. FARMING XP
        if (mat == Material.WHEAT || mat == Material.POTATOES || mat == Material.CARROTS || mat == Material.BEETROOTS || mat == Material.NETHER_WART || mat == Material.SWEET_BERRY_BUSH) {
            BlockData bd = block.getBlockData();
            if (bd instanceof Ageable ageable) {
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    profile.addXp(SkillType.FARMING, 5.0, player);
                }
            } else if (mat == Material.SWEET_BERRY_BUSH) {
                profile.addXp(SkillType.FARMING, 2.0, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        PlayerProfile profile = plugin.getProfileManager().getProfile(killer.getUniqueId());
        if (profile == null) return;

        // Determine Ranged or Melee
        boolean isRanged = false;
        if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent) {
            Entity damager = damageEvent.getDamager();
            if (damager instanceof Projectile) {
                isRanged = true;
            }
        }

        double xp = 15.0;
        if (entity instanceof EnderDragon || entity instanceof Wither) {
            xp = 500.0;
        } else if (entity instanceof Boss || entity instanceof ElderGuardian) {
            xp = 150.0;
        } else if (entity instanceof Monster) {
            xp = 25.0;
        } else if (entity instanceof Animals) {
            xp = 5.0;
        }

        if (isRanged) {
            profile.addXp(SkillType.ARCHERY, xp, killer);
        } else {
            profile.addXp(SkillType.COMBAT, xp, killer);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 1. Damager is Player -> Apply Strength Multiplier
        if (event.getDamager() instanceof Player attacker) {
            PlayerProfile profile = plugin.getProfileManager().getProfile(attacker.getUniqueId());
            if (profile != null) {
                double multiplier = profile.getPhysicalDamageMultiplier();
                event.setDamage(event.getDamage() * multiplier);
            }
        }

        // 2. Target is Player -> Apply Defense (Dodge / Defense XP)
        if (event.getEntity() instanceof Player victim) {
            PlayerProfile profile = plugin.getProfileManager().getProfile(victim.getUniqueId());
            if (profile != null) {
                // Apply Dodge
                double dodgeChance = profile.getDodgeChance();
                if (Math.random() < dodgeChance) {
                    event.setCancelled(true);
                    victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
                    victim.getWorld().spawnParticle(Particle.SWEEP_ATTACK, victim.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.0);
                    victim.sendActionBar("§a⚡ 成功閃避攻擊！");
                    return;
                }

                // Award Defense XP based on damage taken
                double damage = event.getFinalDamage();
                if (damage > 0) {
                    profile.addXp(SkillType.DEFENSE, damage * 1.5, victim);
                }
            }
        }
    }
}
