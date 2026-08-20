package com.example.crestrpg.lifeskills.listener;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import com.example.crestrpg.lifeskills.manager.PlayerToggleManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 自動整理背包和箱子 (Sort Inventory / Sort Chest) 監聽器。
 * - 背包整理：透過指令 /cls sort 觸發
 * - 箱子整理：潛行 + 右鍵點擊箱子觸發
 */
public class SortListener implements Listener {

    private static final String FEATURE_INV = PlayerToggleManager.SORT_INVENTORY;
    private static final String FEATURE_CHEST = PlayerToggleManager.SORT_CHEST;

    private final CrestRpgPlugin plugin;

    public SortListener(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 潛行 + 右鍵箱子 → 整理箱子
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!plugin.getConfig().getBoolean("sort-chest.enabled", true)) return;
        if (plugin.getConfig().getBoolean("sort-chest.require-sneak", true) && !event.getPlayer().isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Container)) return;
        // 排除玩家相關的容器（如末影箱、信標等）
        if (block.getType() == Material.ENDER_CHEST || block.getType() == Material.BEACON) return;

        Player player = event.getPlayer();
        if (!plugin.getToggleManager().isEnabled(player.getUniqueId(), FEATURE_CHEST)) return;

        double cooldown = plugin.getConfig().getDouble("sort-chest.cooldown-seconds", 2.0);
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), FEATURE_CHEST, cooldown)) {
            double remaining = plugin.getCooldownManager().getRemainingCooldown(player.getUniqueId(), FEATURE_CHEST, cooldown);
            player.sendActionBar("§c📦 箱子整理冷卻中... §f" + String.format("%.1f", remaining) + " 秒");
            event.setCancelled(true);
            return;
        }

        double manaCost = plugin.getConfig().getDouble("sort-chest.mana-cost", 5.0);
        if (!plugin.hasLifeSkillMana(player, manaCost)) {
            player.sendActionBar("§c⚡ 法力不足！整理箱子需要 §f" + (int) manaCost + " §c點法力！");
            return;
        }

        event.setCancelled(true); // 阻止打開箱子界面

        String sortMode = plugin.getConfig().getString("sort-chest.sort-mode", "TYPE");
        Inventory inv = ((Container) block.getState()).getInventory();
        sortInventory(inv, sortMode, false);

        plugin.consumeLifeSkillMana(player, manaCost);
        plugin.getCooldownManager().setCooldown(player.getUniqueId(), FEATURE_CHEST);

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.8f, 1.3f);
        player.sendActionBar("§a📦 箱子已整理完畢！§7 | §b法力 -" + (int) manaCost);
    }

    // ==========================================
    // 離開時清理冷卻記錄
    // ==========================================
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getCooldownManager().clearAll(event.getPlayer().getUniqueId());
        plugin.getToggleManager().clearAll(event.getPlayer().getUniqueId());
    }

    // ==========================================
    // 公開方法：整理玩家背包（供指令呼叫）
    // ==========================================
    public boolean sortPlayerInventory(Player player) {
        if (!plugin.getConfig().getBoolean("sort-inventory.enabled", true)) {
            player.sendMessage("§c[CrestRPG] 自動整理背包功能已停用！");
            return false;
        }
        if (!plugin.getToggleManager().isEnabled(player.getUniqueId(), FEATURE_INV)) {
            player.sendMessage("§e[CrestRPG] 您已關閉自動整理背包功能。輸入 §f/cls toggle sort-inventory §e來開啟。");
            return false;
        }

        double cooldown = plugin.getConfig().getDouble("sort-inventory.cooldown-seconds", 2.0);
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), FEATURE_INV, cooldown)) {
            double remaining = plugin.getCooldownManager().getRemainingCooldown(player.getUniqueId(), FEATURE_INV, cooldown);
            player.sendActionBar("§c🎒 背包整理冷卻中... §f" + String.format("%.1f", remaining) + " 秒");
            return false;
        }

        double manaCost = plugin.getConfig().getDouble("sort-inventory.mana-cost", 5.0);
        if (!plugin.hasLifeSkillMana(player, manaCost)) {
            player.sendActionBar("§c⚡ 法力不足！整理背包需要 §f" + (int) manaCost + " §c點法力！");
            return false;
        }

        String sortMode = plugin.getConfig().getString("sort-inventory.sort-mode", "TYPE");
        boolean includeHotbar = plugin.getConfig().getBoolean("sort-inventory.include-hotbar", false);

        // 整理背包主區域 (slots 9-35)
        sortInventory(player.getInventory(), sortMode, includeHotbar);

        plugin.consumeLifeSkillMana(player, manaCost);
        plugin.getCooldownManager().setCooldown(player.getUniqueId(), FEATURE_INV);

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.8f, 1.5f);
        player.sendActionBar("§a🎒 背包已整理完畢！§7 | §b法力 -" + (int) manaCost);
        return true;
    }

    // ==========================================
    // 核心整理演算法
    // ==========================================

    /**
     * 整理一個 Inventory（可以是玩家背包或箱子）
     * @param inv 要整理的物品欄
     * @param sortMode "TYPE" 或 "ALPHA"
     * @param includeHotbar 若是玩家背包，是否包含快捷列 (slots 0-8)
     */
    public void sortInventory(Inventory inv, String sortMode, boolean includeHotbar) {
        ItemStack[] contents = inv.getContents();
        int startSlot = (inv.getHolder() instanceof org.bukkit.entity.HumanEntity && !includeHotbar) ? 9 : 0;
        int endSlot = contents.length;
        if (inv.getHolder() instanceof org.bukkit.entity.HumanEntity) {
            endSlot = 36; // 避免整理到裝備欄 (36-39) 與副手欄 (40)
        }

        // 收集需要整理的物品
        List<ItemStack> items = new ArrayList<>();
        for (int i = startSlot; i < endSlot; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }

        // 合堆疊（把相同物品疊在一起）
        items = stackItems(items);

        // 排序
        if ("ALPHA".equalsIgnoreCase(sortMode)) {
            items.sort(Comparator.comparing(i -> i.getType().name()));
        } else {
            // TYPE 模式：按物品類別分組排序
            items.sort(Comparator.comparingInt(SortListener::getCategoryOrder)
                    .thenComparing(i -> i.getType().name()));
        }

        // 清空對應區域並重新填入
        for (int i = startSlot; i < endSlot; i++) {
            contents[i] = null;
        }
        int slot = startSlot;
        for (ItemStack item : items) {
            if (slot >= endSlot) break;
            contents[slot++] = item;
        }
        inv.setContents(contents);
    }

    /**
     * 將相同類型的物品合堆（最大堆疊）
     */
    private List<ItemStack> stackItems(List<ItemStack> items) {
        Map<String, ItemStack> stacked = new LinkedHashMap<>();
        for (ItemStack item : items) {
            String key = item.getType().name() + "|" + item.getDurability() + "|" + (item.hasItemMeta() ? item.getItemMeta().toString() : "");
            if (stacked.containsKey(key)) {
                ItemStack existing = stacked.get(key);
                int maxStack = item.getMaxStackSize();
                int combined = existing.getAmount() + item.getAmount();
                if (combined <= maxStack) {
                    existing.setAmount(combined);
                } else {
                    // 超過最大堆疊量，分成兩份
                    existing.setAmount(maxStack);
                    ItemStack overflow = item.clone();
                    overflow.setAmount(combined - maxStack);
                    stacked.put(key + "|overflow" + System.nanoTime(), overflow);
                }
            } else {
                stacked.put(key, item.clone());
            }
        }
        return new ArrayList<>(stacked.values());
    }

    /**
     * 按物品類別決定排序優先順序（TYPE 模式）
     */
    private static int getCategoryOrder(ItemStack item) {
        String name = item.getType().name();
        // 武器
        if (name.endsWith("_SWORD") || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("TRIDENT")) return 0;
        // 工具
        if (name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.equals("SHEARS") || name.equals("FISHING_ROD") || name.equals("FLINT_AND_STEEL")) return 1;
        // 裝備
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.equals("SHIELD")) return 2;
        // 食物
        if (item.getType().isEdible()) return 3;
        // 方塊（建材）
        if (item.getType().isBlock()) return 5;
        // 資源/礦石
        if (name.contains("_ORE") || name.contains("_INGOT") || name.contains("_NUGGET") || name.contains("_GEM") || name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("QUARTZ") || name.contains("COAL") || name.contains("REDSTONE") || name.contains("LAPIS")) return 4;
        // 其他
        return 6;
    }
}
