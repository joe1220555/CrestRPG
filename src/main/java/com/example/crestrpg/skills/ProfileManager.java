package com.example.crestrpg.skills;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManager {

    private final CrestRpgPlugin plugin;
    private final Map<UUID, PlayerProfile> activeProfiles = new ConcurrentHashMap<>();

    public ProfileManager(CrestRpgPlugin plugin) {
        this.plugin = plugin;
        startManaRegenAndActionBarTask();
    }

    public PlayerProfile getProfile(UUID uuid) {
        return activeProfiles.get(uuid);
    }

    public Map<UUID, PlayerProfile> getActiveProfiles() {
        return activeProfiles;
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerProfile profile = plugin.getDatabaseManager().loadProfile(uuid, name);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    activeProfiles.put(uuid, profile);
                    profile.updatePlayerAttributes(player);
                }
            });
        });
    }

    public void unloadPlayer(UUID uuid) {
        PlayerProfile profile = activeProfiles.remove(uuid);
        if (profile != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().saveProfile(profile);
            });
        }
    }

    public void saveAll() {
        for (PlayerProfile profile : activeProfiles.values()) {
            plugin.getDatabaseManager().saveProfile(profile);
        }
    }

    private void startManaRegenAndActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerProfile profile = getProfile(player.getUniqueId());
                    if (profile == null) continue;

                    // 1. Mana Regen (1 second interval)
                    double currentMana = profile.getCurrentMana();
                    double maxMana = profile.getMaxMana();
                    if (currentMana < maxMana) {
                        double regen = profile.getManaRegenPerSecond();
                        profile.setCurrentMana(currentMana + regen);
                    }

                    // 2. Action Bar Update (if enabled)
                    if (plugin.getConfig().getBoolean("actionbar.enabled", true)) {
                        String format = plugin.getConfig().getString("actionbar.format", "§c❤️ 生命值: {hp}/{max_hp}  §b⚡ 法力值: {mana}/{max_mana}");
                        int currentHp = (int) player.getHealth();
                        int maxHp = (int) player.getMaxHealth();
                        int currentMp = (int) profile.getCurrentMana();
                        int maxMp = (int) profile.getMaxMana();

                        String msg = format
                                .replace("{hp}", String.valueOf(currentHp))
                                .replace("{max_hp}", String.valueOf(maxHp))
                                .replace("{mana}", String.valueOf(currentMp))
                                .replace("{max_mana}", String.valueOf(maxMp));
                        player.sendActionBar(msg);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
