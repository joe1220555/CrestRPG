package com.example.crestrpg.skills;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class PlayerProfile {
    private final UUID uuid;
    private String playerName;
    private final Map<SkillType, Integer> levels = new HashMap<>();
    private final Map<SkillType, Double> xp = new HashMap<>();
    private int strength = 0;
    private int dexterity = 0;
    private int intelligence = 0;
    private int vitality = 0;
    private int unusedAp = 0;
    private double currentMana = 0;
    private int characterLevel = 1;
    private double characterXp = 0;
    private String classId = "adventurer";
    private int skillPoints = 1;
    private final Set<String> unlockedSkillNodes = new HashSet<>();
    private final Map<String, Integer> classSkillLevels = new HashMap<>();
    private final Map<String, Double> classSkillXp = new HashMap<>();
    private final Map<Integer, String> skillBar = new HashMap<>();

    public PlayerProfile(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        for (SkillType type : SkillType.values()) {
            levels.put(type, 1);
            xp.put(type, 0.0);
        }
        this.currentMana = 100.0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getCharacterLevel() {
        return characterLevel;
    }

    public void setCharacterLevel(int characterLevel) {
        this.characterLevel = Math.max(1, characterLevel);
    }
    public double getCharacterXp() { return characterXp; }
    public void setCharacterXp(double characterXp) { this.characterXp = Math.max(0, characterXp); }

    public double getCharacterXpRequired() {
        double base = CrestRpgPlugin.getInstance().getConfig().getDouble("progression.character-xp.base", 250.0);
        double exponent = CrestRpgPlugin.getInstance().getConfig().getDouble("progression.character-xp.exponent", 1.6);
        return base * Math.pow(characterLevel, exponent);
    }

    public void addCharacterXp(double amount, Player player) {
        if (!Double.isFinite(amount) || amount <= 0) return;
        double gained = amount * CrestRpgPlugin.getInstance().getConfig().getDouble("progression.character-xp.skill-xp-conversion", 0.25);
        characterXp += gained;
        int oldLevel = characterLevel;
        int maxLevel = CrestRpgPlugin.getInstance().getConfig().getInt("progression.max-character-level", 100);
        while (characterLevel < maxLevel && characterXp >= getCharacterXpRequired()) {
            characterXp -= getCharacterXpRequired();
            characterLevel++;
        }
        if (characterLevel > oldLevel) {
            int levels = characterLevel - oldLevel;
            int pointsPerLevel = CrestRpgPlugin.getInstance().getConfig().getInt("progression.skill-points-per-level", 1);
            skillPoints += levels * Math.max(0, pointsPerLevel);
            if (player != null && player.isOnline()) {
                player.sendMessage("§6★ §a角色等級提升至 §e" + characterLevel + "§a，獲得 §b" + (levels * Math.max(0, pointsPerLevel)) + " §a技能點！");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId == null || classId.isBlank() ? "adventurer" : classId.toLowerCase(java.util.Locale.ROOT);
    }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = Math.max(0, skillPoints); }
    public Set<String> getUnlockedSkillNodes() { return unlockedSkillNodes; }
    public boolean unlockSkillNode(String key, int cost) {
        if (cost < 1 || skillPoints < cost || unlockedSkillNodes.contains(key)) return false;
        skillPoints -= cost;
        unlockedSkillNodes.add(key);
        return true;
    }
    public Map<String, Integer> getClassSkillLevels() { return classSkillLevels; }
    public Map<String, Double> getClassSkillXp() { return classSkillXp; }
    public Map<Integer, String> getSkillBar() { return skillBar; }
    public int getClassSkillLevel(String key) { return classSkillLevels.getOrDefault(key, 1); }
    public double getClassSkillXp(String key) { return classSkillXp.getOrDefault(key, 0.0); }
    public double getClassSkillXpRequired(String key) {
        double base = CrestRpgPlugin.getInstance().getConfig().getDouble("class-skills.leveling.base-xp", 100.0);
        double exponent = CrestRpgPlugin.getInstance().getConfig().getDouble("class-skills.leveling.exponent", 1.45);
        return base * Math.pow(getClassSkillLevel(key), exponent);
    }
    public boolean bindSkill(int slot, String key) {
        if (slot < 1 || slot > 9 || key == null || key.isBlank()) return false;
        skillBar.put(slot, key.toLowerCase(java.util.Locale.ROOT)); return true;
    }
    public void unbindSkill(int slot) { skillBar.remove(slot); }
    public boolean addClassSkillXp(String key, double amount, Player player) {
        if (key == null || !Double.isFinite(amount) || amount <= 0) return false;
        key = key.toLowerCase(java.util.Locale.ROOT); int level = getClassSkillLevel(key);
        int maximum = CrestRpgPlugin.getInstance().getConfig().getInt("class-skills.leveling.max-level", 20);
        double value = getClassSkillXp(key) + amount; boolean leveled = false;
        while (level < maximum) {
            double required = CrestRpgPlugin.getInstance().getConfig().getDouble("class-skills.leveling.base-xp", 100.0)
                    * Math.pow(level, CrestRpgPlugin.getInstance().getConfig().getDouble("class-skills.leveling.exponent", 1.45));
            if (value < required) break; value -= required; level++; leveled = true;
        }
        classSkillLevels.put(key, level); classSkillXp.put(key, value);
        if (leveled && player != null) player.sendMessage("§6★ §a技能 §e" + key + " §a提升至 §e" + level + " §a級！");
        return leveled;
    }

    public int getLevel(SkillType type) {
        return levels.getOrDefault(type, 1);
    }

    public void setLevel(SkillType type, int level) {
        levels.put(type, level);
    }

    public double getXp(SkillType type) {
        return xp.getOrDefault(type, 0.0);
    }

    public void setXp(SkillType type, double value) {
        xp.put(type, value);
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public void setDexterity(int dexterity) {
        this.dexterity = dexterity;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }

    public int getVitality() {
        return vitality;
    }

    public void setVitality(int vitality) {
        this.vitality = vitality;
    }

    public int getUnusedAp() {
        return unusedAp;
    }

    public void setUnusedAp(int unusedAp) {
        this.unusedAp = unusedAp;
    }

    public double getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(double currentMana) {
        this.currentMana = Math.max(0, Math.min(currentMana, getMaxMana()));
    }

    // ==========================================
    // Total Stats (Base + Equipment)
    // ==========================================

    public int getTotalStrength() {
        if (CrestRpgPlugin.getInstance() == null || CrestRpgPlugin.getInstance().getEquipmentManager() == null) {
            return strength;
        }
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return strength;
        }
        return strength + CrestRpgPlugin.getInstance().getClassStatBonus(uuid, "strength")
                + CrestRpgPlugin.getInstance().getEquipmentManager().getBonus(player, "strength")
                + CrestRpgPlugin.getInstance().getAdvancedStatBonus(player, "strength");
    }

    public int getTotalDexterity() {
        if (CrestRpgPlugin.getInstance() == null || CrestRpgPlugin.getInstance().getEquipmentManager() == null) {
            return dexterity;
        }
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return dexterity;
        }
        return dexterity + CrestRpgPlugin.getInstance().getClassStatBonus(uuid, "dexterity")
                + CrestRpgPlugin.getInstance().getEquipmentManager().getBonus(player, "dexterity")
                + CrestRpgPlugin.getInstance().getAdvancedStatBonus(player, "dexterity");
    }

    public int getTotalIntelligence() {
        if (CrestRpgPlugin.getInstance() == null || CrestRpgPlugin.getInstance().getEquipmentManager() == null) {
            return intelligence;
        }
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return intelligence;
        }
        return intelligence + CrestRpgPlugin.getInstance().getClassStatBonus(uuid, "intelligence")
                + CrestRpgPlugin.getInstance().getEquipmentManager().getBonus(player, "intelligence")
                + CrestRpgPlugin.getInstance().getAdvancedStatBonus(player, "intelligence");
    }

    public int getTotalVitality() {
        if (CrestRpgPlugin.getInstance() == null || CrestRpgPlugin.getInstance().getEquipmentManager() == null) {
            return vitality;
        }
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return vitality;
        }
        return vitality + CrestRpgPlugin.getInstance().getClassStatBonus(uuid, "vitality")
                + CrestRpgPlugin.getInstance().getEquipmentManager().getBonus(player, "vitality")
                + CrestRpgPlugin.getInstance().getAdvancedStatBonus(player, "vitality");
    }

    // ==========================================
    // Calculated Values
    // ==========================================

    public double getMaxMana() {
        double configuredBase = CrestRpgPlugin.getInstance().getConfig().getDouble("mana.base-max-mana", 100.0);
        double baseMax = CrestRpgPlugin.getInstance().getClassBaseMana(uuid, configuredBase);
        double bonusPerInt = CrestRpgPlugin.getInstance().getConfig().getDouble("attributes.intelligence-mana-bonus", 2.0);
        return baseMax + (getTotalIntelligence() * bonusPerInt);
    }

    public double getManaRegenPerSecond() {
        double baseRegen = CrestRpgPlugin.getInstance().getConfig().getDouble("mana.base-regen-per-second", 5.0);
        double regenPerInt = CrestRpgPlugin.getInstance().getConfig().getDouble("mana.regen-per-intelligence", 0.1);
        return baseRegen + (getTotalIntelligence() * regenPerInt);
    }

    public double getMaxHealth() {
        double bonusPerVit = CrestRpgPlugin.getInstance().getConfig().getDouble("attributes.vitality-health-bonus", 0.5);
        return 20.0 + (getTotalVitality() * bonusPerVit);
    }

    public double getSpeedBonus() {
        double speedPercent = CrestRpgPlugin.getInstance().getConfig().getDouble("attributes.dexterity-speed-percent", 0.2);
        return (getTotalDexterity() * speedPercent) / 100.0;
    }

    public double getPhysicalDamageMultiplier() {
        double damagePercent = CrestRpgPlugin.getInstance().getConfig().getDouble("attributes.strength-damage-percent", 1.0);
        return 1.0 + (getTotalStrength() * damagePercent / 100.0);
    }

    public double getMagicDamageMultiplier() {
        double magicPercent = CrestRpgPlugin.getInstance().getConfig().getDouble("attributes.intelligence-magic-damage-percent", 1.5);
        return 1.0 + (getTotalIntelligence() * magicPercent / 100.0);
    }

    public double getDodgeChance() {
        double dodgePercent = CrestRpgPlugin.getInstance().getConfig().getDouble("attributes.dexterity-dodge-percent", 0.1);
        return Math.min(0.50, (getTotalDexterity() * dodgePercent / 100.0)); // Cap at 50% dodge
    }

    public double getXpRequiredForNextLevel(SkillType type) {
        int currentLvl = getLevel(type);
        double base = CrestRpgPlugin.getInstance().getConfig().getDouble("skills.formula.base-xp", 100.0);
        double exponent = CrestRpgPlugin.getInstance().getConfig().getDouble("skills.formula.exponent", 1.5);
        return base * Math.pow(currentLvl, exponent);
    }

    public void addXp(SkillType skill, double amount, Player player) {
        addCharacterXp(amount, player);
        double currentXp = getXp(skill);
        double required = getXpRequiredForNextLevel(skill);
        currentXp += amount;

        int levelBefore = getLevel(skill);
        int currentLvl = levelBefore;

        while (currentXp >= required && currentLvl < 100) {
            currentXp -= required;
            currentLvl++;
            required = 100.0 * Math.pow(currentLvl, 1.5); // Fast recalculate
        }

        setXp(skill, currentXp);
        if (currentLvl > levelBefore) {
            setLevel(skill, currentLvl);
            int apPerLvl = CrestRpgPlugin.getInstance().getConfig().getInt("skills.ap-per-level", 2);
            int apGain = (currentLvl - levelBefore) * apPerLvl;
            unusedAp += apGain;

            if (player != null && player.isOnline()) {
                player.sendMessage("§e⚔️§f §a§l技能升級！§f 您的 " + skill.getIconPrefix() + " " + skill.getDisplayName() + " §f技能已提升至 §e" + currentLvl + " 級§f！");
                player.sendMessage("§b✦ 獲得了 §d" + apGain + " 點 §b屬性點！輸入 §e/skills §b分配點數。");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                updatePlayerAttributes(player);
            }
        } else {
            if (player != null && player.isOnline() && amount > 0) {
                player.sendActionBar("§a" + skill.getIconPrefix() + " " + skill.getDisplayName() + " 經驗 + " + String.format("%.1f", amount) + " §7(" + String.format("%.1f", currentXp) + "/" + String.format("%.0f", required) + ")");
            }
        }
    }

    public void updatePlayerAttributes(Player player) {
        if (player == null || !player.isOnline()) return;

        double maxHp = getMaxHealth();
        AttributeInstance hpAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.setBaseValue(maxHp);
            if (player.getHealth() > maxHp) {
                player.setHealth(maxHp);
            }
        }

        double speedBonus = getSpeedBonus();
        player.setWalkSpeed((float) (0.2f * (1.0 + speedBonus)));
    }
}
