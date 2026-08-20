package com.example.crestrpg.lifeskills.manager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家功能開關管理器。
 * 玩家可以透過指令自行切換某項功能的啟用狀態。
 * 所有功能預設為開啟。
 */
public class PlayerToggleManager {

    // Map<UUID, Set<已停用功能名>>
    private final Map<UUID, Set<String>> disabledFeatures = new HashMap<>();

    public static final String CHAIN_MINING = "chain-mining";
    public static final String TREECAPITATOR = "treecapitator";
    public static final String AUTO_HARVEST = "auto-harvest";
    public static final String SORT_INVENTORY = "sort-inventory";
    public static final String SORT_CHEST = "sort-chest";

    /**
     * 判斷玩家的某功能是否已啟用
     */
    public boolean isEnabled(UUID uuid, String feature) {
        Set<String> disabled = disabledFeatures.get(uuid);
        if (disabled == null) return true;
        return !disabled.contains(feature);
    }

    /**
     * 切換玩家的功能開關，回傳切換後的狀態
     * @return true = 已啟用, false = 已停用
     */
    public boolean toggle(UUID uuid, String feature) {
        Set<String> disabled = disabledFeatures.computeIfAbsent(uuid, k -> new HashSet<>());
        if (disabled.contains(feature)) {
            disabled.remove(feature);
            return true; // 現在是啟用
        } else {
            disabled.add(feature);
            return false; // 現在是停用
        }
    }

    /**
     * 清除玩家記錄（離開伺服器時回收記憶體）
     */
    public void clearAll(UUID uuid) {
        disabledFeatures.remove(uuid);
    }
}
