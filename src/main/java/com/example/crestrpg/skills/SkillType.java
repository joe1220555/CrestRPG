package com.example.crestrpg.skills;

public enum SkillType {
    FARMING("FARMING", "農業", "§e🌾"),
    MINING("MINING", "挖礦", "§7⛏️"),
    FORAGING("FORAGING", "伐木", "§6🪓"),
    COMBAT("COMBAT", "戰鬥", "§c⚔️"),
    ARCHERY("ARCHERY", "弓箭", "§a🏹"),
    SORCERY("SORCERY", "魔法", "§b🔮"),
    DEFENSE("DEFENSE", "防禦", "§9🛡️");

    private final String key;
    private final String displayName;
    private final String iconPrefix;

    SkillType(String key, String displayName, String iconPrefix) {
        this.key = key;
        this.displayName = displayName;
        this.iconPrefix = iconPrefix;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconPrefix() {
        return iconPrefix;
    }

    public static SkillType fromKey(String key) {
        for (SkillType type : values()) {
            if (type.name().equalsIgnoreCase(key) || type.getKey().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
