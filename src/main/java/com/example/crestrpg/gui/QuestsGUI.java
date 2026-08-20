package com.example.crestrpg.gui;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import com.example.crestrpg.quests.ObjectiveModel;
import com.example.crestrpg.quests.QuestModel;
import com.example.crestrpg.quests.QuestProgress;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class QuestsGUI {

    public static final String MAIN_TITLE = "§8📜 冒險任務主選單";
    public static final String TITLE_MAIN = "§8🌟 故事主線任務";
    public static final String TITLE_SIDE = "§8📜 冒險支線任務";
    public static final String TITLE_DONE = "§8✅ 已完成的冒險";

    private final CrestRpgPlugin plugin;

    public QuestsGUI(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new RPGInventoryHolder.QuestsMainHolder(), 27, MAIN_TITLE);

        // Fill border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        // Main Quests Button (Slot 11)
        ItemStack mainQuests = new ItemStack(Material.BOOK);
        ItemMeta mainMeta = mainQuests.getItemMeta();
        if (mainMeta != null) {
            mainMeta.setDisplayName("§e🌟 主線任務 (Main Quests)");
            mainMeta.setLore(Arrays.asList(
                    "§7──────────────────",
                    "§7查看您目前進行中的主線故事，",
                    "§7引領您探索克雷斯特的世界起源。",
                    "§7──────────────────",
                    "§b點擊開啟選單"
            ));
            mainQuests.setItemMeta(mainMeta);
        }
        inv.setItem(11, mainQuests);

        // Side Quests Button (Slot 13)
        ItemStack sideQuests = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta sideMeta = sideQuests.getItemMeta();
        if (sideMeta != null) {
            sideMeta.setDisplayName("§b📜 支線任務 (Side Quests)");
            sideMeta.setLore(Arrays.asList(
                    "§7──────────────────",
                    "§7探索村民的委託與各類挑戰，",
                    "§7獲得額外的獎金與稀有技能經驗。",
                    "§7──────────────────",
                    "§b點擊開啟選單"
            ));
            sideQuests.setItemMeta(sideMeta);
        }
        inv.setItem(13, sideQuests);

        // Completed Quests Button (Slot 15)
        ItemStack completedQuests = new ItemStack(Material.MAP);
        ItemMeta compMeta = completedQuests.getItemMeta();
        if (compMeta != null) {
            compMeta.setDisplayName("§a✅ 已完成的冒險 (Completed)");
            compMeta.setLore(Arrays.asList(
                    "§7──────────────────",
                    "§7回顧您過去光榮完成的所有任務，",
                    "§7每一頁都見證了您的成長與強大。",
                    "§7──────────────────",
                    "§b點擊開啟選單"
            ));
            completedQuests.setItemMeta(compMeta);
        }
        inv.setItem(15, completedQuests);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public void openQuestCategory(Player player, String category, String guiTitle) {
        Inventory inv = Bukkit.createInventory(new RPGInventoryHolder.QuestCategoryHolder(category), 54, guiTitle);

        // Fill border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        // Back Button (Slot 49)
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c◀ 返回主選單");
            back.setItemMeta(backMeta);
        }
        inv.setItem(49, back);

        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> active = plugin.getQuestManager().getActiveQuests(uuid);
        Set<String> completed = plugin.getQuestManager().getCompletedQuests(uuid);

        int slot = 10;
        NamespacedKey questIdKey = new NamespacedKey(plugin, "quest_id");

        for (QuestModel quest : plugin.getQuestManager().getAllQuests()) {
            if (slot > 43) break; // page limit

            boolean matches = false;
            if (category.equals("COMPLETED")) {
                matches = completed.contains(quest.getId());
            } else {
                if (quest.getType().equalsIgnoreCase(category) && !completed.contains(quest.getId())) {
                    matches = true;
                }
            }

            if (!matches) continue;

            ItemStack item = new ItemStack(Material.PAPER);
            if (category.equals("COMPLETED")) {
                item.setType(Material.FILLED_MAP);
            } else if (active.containsKey(quest.getId())) {
                item.setType(Material.WRITTEN_BOOK);
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(quest.getTitle());
                List<String> lore = new ArrayList<>();
                lore.add("§7──────────────────");

                // Description
                for (String desc : quest.getDescription()) {
                    lore.add("§f" + desc);
                }
                lore.add("§7──────────────────");

                // Prerequisite check
                if (quest.getPrerequisite() != null && !quest.getPrerequisite().isEmpty() && !category.equals("COMPLETED")) {
                    QuestModel pre = plugin.getQuestManager().getQuest(quest.getPrerequisite());
                    if (pre != null && !completed.contains(pre.getId())) {
                        lore.add("§c⚠️ 需要先完成任務: " + pre.getTitle());
                        item.setType(Material.BARRIER);
                    }
                }

                // Objectives Progress
                if (category.equals("COMPLETED")) {
                    lore.add("§a✔ 任務已完成！");
                } else if (active.containsKey(quest.getId())) {
                    lore.add("§e任務目標進度：");
                    QuestProgress progress = active.get(quest.getId());
                    for (int i = 0; i < quest.getObjectives().size(); i++) {
                        ObjectiveModel obj = quest.getObjectives().get(i);
                        int current = progress.getProgress(i);
                        if (current >= obj.getCount()) {
                            lore.add(" §a✔ " + obj.getDisplay() + " (" + current + "/" + obj.getCount() + ")");
                        } else {
                            lore.add(" §e• " + obj.getDisplay() + " §7(" + current + "/" + obj.getCount() + ")");
                        }
                    }
                    if (progress.areAllObjectivesComplete(quest)) {
                        lore.add("§7");
                        lore.add("§a★ 目標已全部完成！找 NPC §b" + quest.getNpcName() + " §a對話提交任務。");
                    }
                } else {
                    lore.add("§d任務目標：");
                    for (ObjectiveModel obj : quest.getObjectives()) {
                        lore.add(" §7- " + obj.getDisplay() + " (" + obj.getCount() + "次)");
                    }
                    lore.add("§7");
                    if (quest.getPrerequisite() != null && !quest.getPrerequisite().isEmpty() && !completed.contains(quest.getPrerequisite())) {
                        lore.add("§c無法接取 (前置任務未完成)");
                    } else {
                        lore.add("§a尋找 NPC §b【" + quest.getNpcName() + "】 §a接取任務");
                        if (quest.getType().equalsIgnoreCase("SIDE")) {
                            lore.add("§b(支線任務可直接點擊此卡片接取！)");
                        }
                    }
                }

                lore.add("§7──────────────────");

                // Write quest ID metadata to prevent name spoofing
                meta.getPersistentDataContainer().set(questIdKey, PersistentDataType.STRING, quest.getId());
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inv.setItem(slot, item);
            slot++;
            if (slot % 9 == 8) slot += 2; // skip border
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.7f, 1.2f);
    }
}
