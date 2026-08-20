package com.example.crestrpg.quests;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestProgress {
    private final UUID uuid;
    private final String questId;
    private final Map<Integer, Integer> progress = new HashMap<>();

    public QuestProgress(UUID uuid, String questId) {
        this.uuid = uuid;
        this.questId = questId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getQuestId() {
        return questId;
    }

    public int getProgress(int objectiveIndex) {
        return progress.getOrDefault(objectiveIndex, 0);
    }

    public void setProgress(int objectiveIndex, int current) {
        progress.put(objectiveIndex, current);
    }

    public boolean incrementProgress(int objectiveIndex, int max, int amount) {
        int current = getProgress(objectiveIndex);
        if (current >= max) return false;

        int newProgress = Math.min(max, current + amount);
        progress.put(objectiveIndex, newProgress);
        return newProgress > current;
    }

    public boolean isObjectiveComplete(int objectiveIndex, int max) {
        return getProgress(objectiveIndex) >= max;
    }

    public boolean areAllObjectivesComplete(QuestModel quest) {
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            if (!isObjectiveComplete(i, quest.getObjectives().get(i).getCount())) {
                return false;
            }
        }
        return true;
    }

    public Map<Integer, Integer> getProgressMap() {
        return progress;
    }
}
