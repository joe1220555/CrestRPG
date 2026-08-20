package com.example.crestrpg.gui;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import com.example.crestrpg.skills.PlayerProfile;
import com.example.crestrpg.skills.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class SkillsGUI {

    public static final String TITLE = "§8⚔ RPG 技能與統計 ⚔";
    private final CrestRpgPlugin plugin;

    public SkillsGUI(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage("§c無法加載您的屬性存檔！");
            return;
        }

        Inventory inv = Bukkit.createInventory(new RPGInventoryHolder.SkillsGUIHolder(), 54, TITLE);
        populateGUI(inv, player, profile);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public void populateGUI(Inventory inv, Player player, PlayerProfile profile) {
        // 1. Fill border with Black Stained Glass Pane
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

        // 2. Player Head (Slot 13)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName("§e⭐ §l" + player.getName() + " 的冒險屬性");
            List<String> lore = new ArrayList<>();
            int totalLevel = 0;
            for (SkillType t : SkillType.values()) {
                totalLevel += profile.getLevel(t);
            }
            lore.add("§7──────────────────");
            lore.add("§7角色等級: §e" + profile.getCharacterLevel());
            lore.add("§7角色經驗: §a" + String.format("%.1f", profile.getCharacterXp()) + " / " + String.format("%.1f", profile.getCharacterXpRequired()));
            lore.add("§7總技能等級: §b" + totalLevel);
            lore.add("§7當前生命: §c❤️ " + (int) player.getHealth() + " / " + (int) player.getMaxHealth() + " HP");
            lore.add("§7當前法力: §b⚡ " + (int) profile.getCurrentMana() + " / " + (int) profile.getMaxMana());
            lore.add("§7剩餘屬性點: §d✦ " + profile.getUnusedAp() + " §7(AP)");
            lore.add("§7──────────────────");
            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
        }
        inv.setItem(13, head);

        // 3. Attributes (Slots 29, 30, 31, 32)
        int baseStr = profile.getStrength();
        int bonusStr = plugin.getEquipmentManager().getBonus(player, "strength");
        int totalStr = baseStr + bonusStr;
        inv.setItem(29, createAttrItem(Material.IRON_SWORD, "§c💪 力量 (Strength)", totalStr, profile.getUnusedAp(),
                "§7力量可以增加物理傷害輸出。",
                "§7基礎屬性: §e" + baseStr + " §7| 裝備加成: §a+" + bonusStr,
                "§7物理傷害加成: §a+" + String.format("%.1f", (totalStr * plugin.getConfig().getDouble("attributes.strength-damage-percent", 1.0))) + "%"));

        int baseDex = profile.getDexterity();
        int bonusDex = plugin.getEquipmentManager().getBonus(player, "dexterity");
        int totalDex = baseDex + bonusDex;
        inv.setItem(30, createAttrItem(Material.FEATHER, "§a⚡ 敏捷 (Dexterity)", totalDex, profile.getUnusedAp(),
                "§7敏捷增加移動速度與被攻擊時的閃避機率。",
                "§7基礎屬性: §e" + baseDex + " §7| 裝備加成: §a+" + bonusDex,
                "§7移動速度加成: §a+" + String.format("%.1f", (totalDex * plugin.getConfig().getDouble("attributes.dexterity-speed-percent", 0.2))) + "%",
                "§7物理閃避機率: §a" + String.format("%.1f", (profile.getDodgeChance() * 100.0)) + "%"));

        int baseInt = profile.getIntelligence();
        int bonusInt = plugin.getEquipmentManager().getBonus(player, "intelligence");
        int totalInt = baseInt + bonusInt;
        inv.setItem(31, createAttrItem(Material.BOOK, "§b🧠 智慧 (Intelligence)", totalInt, profile.getUnusedAp(),
                "§7智慧會影響最大法力值與魔法傷害。",
                "§7基礎屬性: §e" + baseInt + " §7| 裝備加成: §a+" + bonusInt,
                "§7最大法力值加成: §a+" + (int) (totalInt * plugin.getConfig().getDouble("attributes.intelligence-mana-bonus", 2.0)) + " Mana",
                "§7技能與魔法傷害: §a+" + String.format("%.1f", (totalInt * plugin.getConfig().getDouble("attributes.intelligence-magic-damage-percent", 1.5))) + "%"));

        int baseVit = profile.getVitality();
        int bonusVit = plugin.getEquipmentManager().getBonus(player, "vitality");
        int totalVit = baseVit + bonusVit;
        inv.setItem(32, createAttrItem(Material.APPLE, "§d❤️ 生命 (Vitality)", totalVit, profile.getUnusedAp(),
                "§7體質可以增加您的最大生命值上限。",
                "§7基礎屬性: §e" + baseVit + " §7| 裝備加成: §a+" + bonusVit,
                "§7生命上限加成: §a+" + (totalVit * plugin.getConfig().getDouble("attributes.vitality-health-bonus", 0.5)) + " HP §7(半顆心)"));

        // 4. Skills (Slots 37 to 43)
        inv.setItem(37, createSkillItem(Material.WHEAT, SkillType.FARMING, profile));
        inv.setItem(38, createSkillItem(Material.IRON_PICKAXE, SkillType.MINING, profile));
        inv.setItem(39, createSkillItem(Material.IRON_AXE, SkillType.FORAGING, profile));
        inv.setItem(40, createSkillItem(Material.IRON_SWORD, SkillType.COMBAT, profile));
        inv.setItem(41, createSkillItem(Material.BOW, SkillType.ARCHERY, profile));
        inv.setItem(42, createSkillItem(Material.ENDER_PEARL, SkillType.SORCERY, profile));
        inv.setItem(43, createSkillItem(Material.SHIELD, SkillType.DEFENSE, profile));
    }

    private ItemStack createAttrItem(Material material, String name, int val, int ap, String... details) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add("§7──────────────────");
            lore.add("§7當前屬性值: §e" + val);
            for (String detail : details) {
                lore.add(detail);
            }
            lore.add("§7──────────────────");
            if (ap > 0) {
                lore.add("§b左鍵點擊: §a配點 +1 點");
                lore.add("§7(可用點數: §d✦ " + ap + "§7)");
            } else {
                lore.add("§c無剩餘點數，升級技能獲得 AP");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSkillItem(Material material, SkillType type, PlayerProfile profile) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.getIconPrefix() + " " + type.getDisplayName() + " 技能");
            List<String> lore = new ArrayList<>();
            int lvl = profile.getLevel(type);
            double xp = profile.getXp(type);
            double req = profile.getXpRequiredForNextLevel(type);

            lore.add("§7──────────────────");
            lore.add("§7目前等級: §e" + lvl + " / 100");
            lore.add("§7經驗進度: §a" + String.format("%.1f", xp) + " / " + String.format("%.0f", req));

            // Build progress bar
            int totalBars = 10;
            int activeBars = (int) Math.round((xp / req) * totalBars);
            activeBars = Math.max(0, Math.min(totalBars, activeBars));
            StringBuilder sb = new StringBuilder("§f[");
            for (int i = 0; i < totalBars; i++) {
                if (i < activeBars) {
                    sb.append("§a■");
                } else {
                    sb.append("§7□");
                }
            }
            sb.append("§f] §a").append(String.format("%.0f", (xp / req) * 100)).append("%");

            lore.add(sb.toString());
            lore.add("§7──────────────────");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
