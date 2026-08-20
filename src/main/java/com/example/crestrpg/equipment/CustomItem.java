package com.example.crestrpg.equipment;

import com.example.crestrpg.skills.SkillType;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public class CustomItem {
    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final Integer customModelData;
    private final Map<String, Integer> stats;
    private final Map<SkillType, Integer> requirements;

    public CustomItem(String id, Material material, String displayName, List<String> lore,
                      Integer customModelData, Map<String, Integer> stats, Map<SkillType, Integer> requirements) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.customModelData = customModelData;
        this.stats = stats;
        this.requirements = requirements;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public Map<String, Integer> getStats() {
        return stats;
    }

    public Map<SkillType, Integer> getRequirements() {
        return requirements;
    }

    public int getStat(String statName) {
        if (stats == null) return 0;
        return stats.getOrDefault(statName.toLowerCase(), 0);
    }
}
