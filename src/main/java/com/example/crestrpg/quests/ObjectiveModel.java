package com.example.crestrpg.quests;

public class ObjectiveModel {
    private final String type; // KILL, COLLECT, CHAT, REACH
    private final String target; // ZOMBIE, WHEAT, 村長, etc.
    private final int count;
    private final String display;

    public ObjectiveModel(String type, String target, int count, String display) {
        this.type = type;
        this.target = target;
        this.count = count;
        this.display = display;
    }

    public String getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getCount() {
        return count;
    }

    public String getDisplay() {
        return display;
    }
}
