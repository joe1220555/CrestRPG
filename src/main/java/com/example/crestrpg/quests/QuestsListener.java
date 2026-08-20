package com.example.crestrpg.quests;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestsListener implements Listener {

    private final CrestRpgPlugin plugin;

    public QuestsListener(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getQuestManager().loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getQuestManager().unloadPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.getQuestManager().checkAllInventoryCollects(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getQuestManager().checkAllInventoryCollects(player);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getQuestManager().checkAllInventoryCollects(player);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        String target = entity.getType().name();

        // 新版網站怪物使用 monster_key；保留 mob_id 以相容舊世界中已生成的怪物。
        NamespacedKey monsterKey = new NamespacedKey(plugin, "monster_key");
        NamespacedKey legacyMobKey = new NamespacedKey(plugin, "mob_id");
        String mobId = entity.getPersistentDataContainer().get(monsterKey, PersistentDataType.STRING);
        if (mobId == null) mobId = entity.getPersistentDataContainer().get(legacyMobKey, PersistentDataType.STRING);
        if (mobId != null) {
            target = "CrestMob:" + mobId;
        }

        plugin.getQuestManager().checkObjectiveProgress(killer, "KILL", target, 1);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onNPCInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        String customName = entity.getCustomName();
        if (customName == null || customName.isEmpty()) return;

        String npcName = customName.replaceAll("§[0-9a-fk-orx]", "").replaceAll("&[0-9a-fk-orx]", "").trim();
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        List<QuestModel> npcQuests = new ArrayList<>();
        for (QuestModel quest : plugin.getQuestManager().getAllQuests()) {
            if (quest.getNpcName().equalsIgnoreCase(npcName)) {
                npcQuests.add(quest);
            }
        }

        if (npcQuests.isEmpty()) return;

        event.setCancelled(true);

        for (QuestModel quest : npcQuests) {
            if (plugin.getQuestManager().isQuestActive(uuid, quest.getId())) {
                QuestProgress progress = plugin.getQuestManager().getActiveQuests(uuid).get(quest.getId());
                if (progress.areAllObjectivesComplete(quest)) {
                    plugin.getQuestManager().completeQuest(player, quest.getId());
                    return;
                }
            }
        }

        for (QuestModel quest : npcQuests) {
            if (plugin.getQuestManager().isQuestActive(uuid, quest.getId())) {
                player.sendMessage("§e[" + npcName + "] §f你正在進行任務 【" + quest.getTitle() + "§f】。");
                player.sendMessage("§e[" + npcName + "] §7請加把勁完成剩餘目標！可以使用 §b/quests §7查看進度。");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        for (QuestModel quest : npcQuests) {
            if (plugin.getQuestManager().canAcceptQuest(player, quest)) {
                plugin.getQuestManager().acceptQuest(player, quest.getId());
                player.sendMessage("§e[" + npcName + "] §a已成功接取新任務！祝你順利完成！");
                return;
            }
        }

        player.sendMessage("§e[" + npcName + "] §f你好，旅人！今天又是冒險的好日子。");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
    }
}
