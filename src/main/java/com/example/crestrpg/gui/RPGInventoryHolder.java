package com.example.crestrpg.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class RPGInventoryHolder {

    public static class SkillsGUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class QuestsMainHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class QuestCategoryHolder implements InventoryHolder {
        private final String category; // 'MAIN', 'SIDE', or 'COMPLETED'

        public QuestCategoryHolder(String category) {
            this.category = category;
        }

        public String getCategory() {
            return category;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
