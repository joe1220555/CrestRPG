package com.example.crestrpg.quests;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import com.example.crestrpg.skills.PlayerProfile;
import com.example.crestrpg.skills.SkillType;
import tw.crestnetwork.rpg.VaultEconomyBridge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class QuestManager {

    private final CrestRpgPlugin plugin;
    private final Map<String, QuestModel> quests = new HashMap<>();

    // Player UUID -> Set of completed Quest IDs
    private final Map<UUID, Set<String>> completedQuests = new HashMap<>();
    // Player UUID -> Map of Active Quest ID -> Progress
    private final Map<UUID, Map<String, QuestProgress>> activeQuests = new HashMap<>();

    public QuestManager(CrestRpgPlugin plugin) {
        this.plugin = plugin;
        loadQuests();
    }

    public void loadQuests() {
        quests.clear();
        File file = new File(plugin.getDataFolder(), "quests.yml");
        if (!file.exists()) {
            plugin.saveResource("quests.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String id : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(id);
            if (sec == null) continue;

            QuestModel quest = new QuestModel(id);
            quest.setTitle(sec.getString("Title", id));
            quest.setType(sec.getString("Type", "MAIN").toUpperCase());
            quest.getDescription().addAll(sec.getStringList("Description"));
            quest.setNpcName(sec.getString("NPC", ""));
            quest.setPrerequisite(sec.getString("Prerequisite", ""));

            // Objectives
            List<?> objList = sec.getList("Objectives");
            if (objList != null) {
                for (Object item : objList) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        String type = (String) map.get("Type");
                        String target = String.valueOf(map.get("Target"));
                        int count = ((Number) map.getOrDefault("Count", 1)).intValue();
                        String display = (String) map.get("Display");

                        quest.getObjectives().add(new ObjectiveModel(type, target, count, display));
                    }
                }
            }

            // Rewards
            ConfigurationSection rewardSec = sec.getConfigurationSection("Rewards");
            if (rewardSec != null) {
                quest.setMoney(rewardSec.getDouble("Money", 0.0));
                quest.setVanillaXp(rewardSec.getInt("VanillaXP", 0));

                // Skill XP Rewards
                ConfigurationSection skillXpSec = rewardSec.getConfigurationSection("SkillsXP");
                if (skillXpSec == null) {
                    // Fallback to legacy key name in case it was used
                    skillXpSec = rewardSec.getConfigurationSection("CrestSkillsXP");
                }
                if (skillXpSec != null) {
                    for (String skillKey : skillXpSec.getKeys(false)) {
                        quest.getSkillXp().put(skillKey.toUpperCase(), skillXpSec.getDouble(skillKey));
                    }
                }

                // Item Rewards
                List<?> itemRewardList = rewardSec.getList("Items");
                if (itemRewardList != null) {
                    for (Object obj : itemRewardList) {
                        if (obj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) obj;
                            int amount = ((Number) map.getOrDefault("Amount", 1)).intValue();

                            Object content = map.get("Content");
                            if (content != null && !content.toString().isBlank()) {
                                quest.getRewardContent().merge(content.toString().toLowerCase(Locale.ROOT), amount, Integer::sum);
                                continue;
                            }

                            String matStr = (String) map.get("Material");
                            if (matStr != null) {
                                Material mat = Material.matchMaterial(matStr);
                                if (mat != null) {
                                    quest.getRewardItems().add(new ItemStack(mat, amount));
                                }
                            }
                        }
                    }
                }

            }

            quests.put(id, quest);
        }
        plugin.getLogger().info("✅ 成功載入 " + quests.size() + " 個任務模板。");
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<String> completed = plugin.getDatabaseManager().loadCompletedQuests(uuid);
            Map<String, QuestProgress> active = plugin.getDatabaseManager().loadActiveQuests(uuid);
            completedQuests.put(uuid, completed);
            activeQuests.put(uuid, active);
        });
    }

    public void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> active = activeQuests.remove(uuid);
        if (active != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                for (QuestProgress prog : active.values()) {
                    plugin.getDatabaseManager().saveQuestProgress(prog);
                }
            });
        }
        completedQuests.remove(uuid);
    }

    public QuestModel getQuest(String id) {
        return quests.get(id);
    }

    public Collection<QuestModel> getAllQuests() {
        return quests.values();
    }

    public Set<String> getCompletedQuests(UUID uuid) {
        return completedQuests.getOrDefault(uuid, Collections.emptySet());
    }

    public Map<String, QuestProgress> getActiveQuests(UUID uuid) {
        return activeQuests.getOrDefault(uuid, Collections.emptyMap());
    }

    public boolean isQuestCompleted(UUID uuid, String questId) {
        return getCompletedQuests(uuid).contains(questId);
    }

    public boolean isQuestActive(UUID uuid, String questId) {
        return getActiveQuests(uuid).containsKey(questId);
    }

    public boolean canAcceptQuest(Player player, QuestModel quest) {
        UUID uuid = player.getUniqueId();
        if (isQuestCompleted(uuid, quest.getId())) return false;
        if (isQuestActive(uuid, quest.getId())) return false;

        String prereq = quest.getPrerequisite();
        if (prereq != null && !prereq.isEmpty()) {
            return isQuestCompleted(uuid, prereq);
        }
        return true;
    }

    public void acceptQuest(Player player, String questId) {
        QuestModel quest = getQuest(questId);
        if (quest == null) return;
        if (!canAcceptQuest(player, quest)) return;

        UUID uuid = player.getUniqueId();
        QuestProgress progress = new QuestProgress(uuid, questId);

        // Initialize objectives progress at 0
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            progress.setProgress(i, 0);
        }

        activeQuests.computeIfAbsent(uuid, k -> new HashMap<>()).put(questId, progress);

        // Save async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().saveQuestProgress(progress);
        });

        player.sendMessage("§e🌟 §f接受任務： " + quest.getTitle());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);

        // Check if COLLECT objectives are already met in inventory
        checkInventoryCollectObjectives(player, progress, quest);
    }

    public void completeQuest(Player player, String questId) {
        QuestModel quest = getQuest(questId);
        if (quest == null) return;

        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> active = activeQuests.get(uuid);
        if (active == null || !active.containsKey(questId)) return;

        QuestProgress progress = active.get(questId);
        if (!progress.areAllObjectivesComplete(quest)) {
            player.sendMessage("§c任務尚未完成所有目標！");
            return;
        }

        // 1. Remove required items from inventory for COLLECT objectives
        for (ObjectiveModel obj : quest.getObjectives()) {
            if (obj.getType().equalsIgnoreCase("COLLECT")) {
                Material mat = Material.matchMaterial(obj.getTarget());
                if (mat != null) {
                    removeItems(player, mat, obj.getCount());
                }
            }
        }

        // 2. Move from active to completed
        active.remove(questId);
        completedQuests.computeIfAbsent(uuid, k -> new HashSet<>()).add(questId);

        // 3. Update database async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().deleteQuestProgress(uuid, questId);
            plugin.getDatabaseManager().addCompletedQuest(uuid, questId);
        });

        // 4. Reward Player
        player.sendMessage("§e🌟 §a§l任務完成！ §f恭喜完成任務 【" + quest.getTitle() + "§f】！");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Vault Money
        if (quest.getMoney() > 0 && VaultEconomyBridge.isAvailable()) {
            VaultEconomyBridge.deposit(player, quest.getMoney());
            player.sendMessage("§b✦ 獲得獎金: §e$" + quest.getMoney());
        }

        // Vanilla XP
        if (quest.getVanillaXp() > 0) {
            player.giveExp(quest.getVanillaXp());
            player.sendMessage("§b✦ 獲得一般經驗值: §a+" + quest.getVanillaXp());
        }

        // Skill XP
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile != null) {
            quest.getSkillXp().forEach((skill, amount) -> {
                SkillType type = SkillType.fromKey(skill);
                if (type != null) {
                    profile.addXp(type, amount, player);
                    player.sendMessage("§b✦ 獲得技能經驗值: " + type.getIconPrefix() + " " + type.getDisplayName() + " §a+" + amount);
                }
            });
        }

        // Items
        for (ItemStack item : quest.getRewardItems()) {
            HashMap<Integer, ItemStack> remain = player.getInventory().addItem(item.clone());
            if (!remain.isEmpty()) {
                for (ItemStack r : remain.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), r);
                }
                player.sendMessage("§7(部分獎勵物品已掉落在地面)");
            }
            player.sendMessage("§b✦ 獲得物品: §e" + item.getType().name() + " x" + item.getAmount());
        }

        quest.getRewardContent().forEach((key, amount) -> {
            for (int index = 0; index < amount; index++) {
                ItemStack item = plugin.createContentItem(key);
                if (item == null) { player.sendMessage("§c⚠ CrestRPG 獎勵內容不存在：" + key); break; }
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                overflow.values().forEach(value -> player.getWorld().dropItemNaturally(player.getLocation(), value));
            }
            player.sendMessage("§b✦ 獲得 CrestRPG 內容：§e" + key + " x" + amount);
        });
    }

    public void checkObjectiveProgress(Player player, String type, String target, int amount) {
        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> active = activeQuests.get(uuid);
        if (active == null || active.isEmpty()) return;

        for (QuestProgress prog : active.values()) {
            QuestModel quest = quests.get(prog.getQuestId());
            if (quest == null) continue;

            boolean changed = false;
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                ObjectiveModel obj = quest.getObjectives().get(i);
                if (obj.getType().equalsIgnoreCase(type)) {
                    boolean match = false;
                    if (type.equalsIgnoreCase("KILL")) {
                        if (obj.getTarget().startsWith("CrestMob:")) {
                            String targetMobId = obj.getTarget().substring(9);
                            if (target.equalsIgnoreCase(targetMobId)) {
                                match = true;
                            }
                        } else {
                            if (obj.getTarget().equalsIgnoreCase(target)) {
                                match = true;
                            }
                        }
                    } else if (type.equalsIgnoreCase("COLLECT") || type.equalsIgnoreCase("CHAT")) {
                        if (obj.getTarget().equalsIgnoreCase(target)) {
                            match = true;
                        }
                    }

                    if (match) {
                        boolean inc = prog.incrementProgress(i, obj.getCount(), amount);
                        if (inc) {
                            changed = true;
                            player.sendActionBar("§e🌟 任務進度: " + obj.getDisplay() + " §a(" + prog.getProgress(i) + "/" + obj.getCount() + ")");
                        }
                    }
                }
            }

            if (changed) {
                // Save progress
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getDatabaseManager().saveQuestProgress(prog);
                });

                if (prog.areAllObjectivesComplete(quest)) {
                    player.sendMessage("§e🌟 §a§l任務目標已全部完成！§f 請找 NPC §b【" + quest.getNpcName() + "】 §f提交任務並領取獎勵！");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            }
        }
    }

    private void checkInventoryCollectObjectives(Player player, QuestProgress prog, QuestModel quest) {
        boolean changed = false;
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            ObjectiveModel obj = quest.getObjectives().get(i);
            if (obj.getType().equalsIgnoreCase("COLLECT")) {
                Material mat = Material.matchMaterial(obj.getTarget());
                if (mat != null) {
                    int count = getItemCount(player, mat);
                    int current = prog.getProgress(i);
                    int targetCount = Math.min(obj.getCount(), count);
                    if (targetCount > current) {
                        prog.setProgress(i, targetCount);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().saveQuestProgress(prog);
            });
            if (prog.areAllObjectivesComplete(quest)) {
                player.sendMessage("§e🌟 §a§l任務目標已全部完成！§f 請找 NPC §b【" + quest.getNpcName() + "】 §f提交任務並領取獎勵！");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            }
        }
    }

    public void checkAllInventoryCollects(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> active = activeQuests.get(uuid);
        if (active == null || active.isEmpty()) return;

        for (QuestProgress prog : active.values()) {
            QuestModel quest = quests.get(prog.getQuestId());
            if (quest != null) {
                checkInventoryCollectObjectives(player, prog, quest);
            }
        }
    }

    private int getItemCount(Player player, Material mat) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, Material mat, int amount) {
        int left = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == mat) {
                if (item.getAmount() <= left) {
                    left -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - left);
                    left = 0;
                }
            }
            if (left <= 0) break;
        }
    }
}
