package com.example.crestrpg.equipment;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import com.example.crestrpg.skills.PlayerProfile;
import com.example.crestrpg.skills.SkillType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class EquipmentManager {

    private final CrestRpgPlugin plugin;
    private final Map<String, CustomItem> customItems = new HashMap<>();
    private final NamespacedKey itemIdKey;

    public EquipmentManager(CrestRpgPlugin plugin) {
        this.plugin = plugin;
        this.itemIdKey = new NamespacedKey(plugin, "custom_item_id");
        loadItems();
    }

    public void loadItems() {
        customItems.clear();
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("items");
        if (section == null) {
            plugin.getLogger().warning("⚠️ items.yml 沒有設定任何物品 (找不到 'items' 節點)！");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection itemSec = section.getConfigurationSection(id);
            if (itemSec == null) continue;

            String matStr = itemSec.getString("material", "DIAMOND_SWORD");
            Material material = Material.matchMaterial(matStr);
            if (material == null) {
                plugin.getLogger().severe("❌ 載入自訂物品 '" + id + "' 失敗：找不到材質 '" + matStr + "'！");
                continue;
            }

            String displayName = itemSec.getString("display-name", "§f" + id);
            List<String> lore = itemSec.getStringList("lore");
            Integer customModelData = itemSec.contains("custom-model-data") ? itemSec.getInt("custom-model-data") : null;

            // Load Stats
            Map<String, Integer> stats = new HashMap<>();
            ConfigurationSection statsSec = itemSec.getConfigurationSection("stats");
            if (statsSec != null) {
                for (String statName : statsSec.getKeys(false)) {
                    stats.put(statName.toLowerCase(), statsSec.getInt(statName));
                }
            }

            // Load Requirements
            Map<SkillType, Integer> reqs = new HashMap<>();
            ConfigurationSection reqsSec = itemSec.getConfigurationSection("requirements");
            if (reqsSec != null) {
                for (String skillName : reqsSec.getKeys(false)) {
                    SkillType skillType = SkillType.fromKey(skillName);
                    if (skillType != null) {
                        reqs.put(skillType, reqsSec.getInt(skillName));
                    } else {
                        plugin.getLogger().warning("⚠️ 自訂物品 '" + id + "' 包含未知的限制技能：'" + skillName + "'");
                    }
                }
            }

            CustomItem item = new CustomItem(id, material, displayName, lore, customModelData, stats, reqs);
            customItems.put(id.toLowerCase(), item);
        }
        plugin.getLogger().info("⚔️ 成功載入 " + customItems.size() + " 個自訂 RPG 武器與裝備！");
    }

    public void reload() {
        loadItems();
    }

    public NamespacedKey getItemIdKey() {
        return itemIdKey;
    }

    public CustomItem getCustomItem(String id) {
        return customItems.get(id.toLowerCase());
    }

    public Collection<CustomItem> getCustomItems() {
        return customItems.values();
    }

    public ItemStack createItemStack(CustomItem customItem, int amount) {
        ItemStack item = new ItemStack(customItem.getMaterial(), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(customItem.getDisplayName().replace("&", "§"));

            List<String> formattedLore = new ArrayList<>();
            for (String line : customItem.getLore()) {
                formattedLore.add(line.replace("&", "§"));
            }
            meta.setLore(formattedLore);

            if (customItem.getCustomModelData() != null) {
                meta.setCustomModelData(customItem.getCustomModelData());
            }

            // Write PDC tag
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, customItem.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    public CustomItem getCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String id = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (id == null) return null;
        return getCustomItem(id);
    }

    public boolean meetsRequirements(Player player, CustomItem customItem) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return false;

        for (Map.Entry<SkillType, Integer> entry : customItem.getRequirements().entrySet()) {
            if (profile.getLevel(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public int getBonus(Player player, String statName) {
        int totalBonus = 0;

        // 1. Check held item
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        totalBonus += getItemBonus(player, mainHand, statName);

        // 2. Check armor items
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            totalBonus += getItemBonus(player, armor, statName);
        }

        return totalBonus;
    }

    private int getItemBonus(Player player, ItemStack item, String statName) {
        if (item == null || item.getType().isAir()) return 0;

        CustomItem customItem = getCustomItem(item);
        if (customItem == null) return 0;

        if (!meetsRequirements(player, customItem)) {
            return 0; // Skill level too low, no attributes applied
        }

        return customItem.getStat(statName);
    }
}
