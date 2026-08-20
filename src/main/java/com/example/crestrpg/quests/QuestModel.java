package com.example.crestrpg.quests;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestModel {
    private final String id;
    private String title;
    private String type = "MAIN"; // MAIN, SIDE
    private final List<String> description = new ArrayList<>();
    private String npcName = "";
    private String prerequisite = "";
    private final List<ObjectiveModel> objectives = new ArrayList<>();

    // Rewards
    private double money = 0.0;
    private int vanillaXp = 0;
    private final Map<String, Double> skillXp = new HashMap<>();
    private final List<ItemStack> rewardItems = new ArrayList<>();
    private final Map<String, Integer> rewardContent = new HashMap<>();

    public QuestModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getDescription() {
        return description;
    }

    public String getNpcName() {
        return npcName;
    }

    public void setNpcName(String npcName) {
        this.npcName = npcName;
    }

    public String getPrerequisite() {
        return prerequisite;
    }

    public void setPrerequisite(String prerequisite) {
        this.prerequisite = prerequisite;
    }

    public List<ObjectiveModel> getObjectives() {
        return objectives;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public int getVanillaXp() {
        return vanillaXp;
    }

    public void setVanillaXp(int vanillaXp) {
        this.vanillaXp = vanillaXp;
    }

    public Map<String, Double> getSkillXp() {
        return skillXp;
    }

    public List<ItemStack> getRewardItems() {
        return rewardItems;
    }

    public Map<String, Integer> getRewardContent() { return rewardContent; }
}
