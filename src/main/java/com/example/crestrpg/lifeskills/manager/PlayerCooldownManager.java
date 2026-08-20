package com.example.crestrpg.lifeskills.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家各功能的冷卻管理器。
 * 記錄每個玩家對每個功能的最後觸發時間，並提供冷卻判斷。
 */
public class PlayerCooldownManager {

    // Map<UUID, Map<功能名, 最後觸發時間 (ms)>>
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    /**
     * 判斷玩家的某功能是否在冷卻中
     * @param uuid 玩家 UUID
     * @param feature 功能名 (如 "chain-mining", "treecapitator")
     * @param cooldownSeconds 設定的冷卻秒數
     * @return true 代表仍在冷卻中
     */
    public boolean isOnCooldown(UUID uuid, String feature, double cooldownSeconds) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return false;
        Long last = playerCooldowns.get(feature);
        if (last == null) return false;
        return System.currentTimeMillis() - last < (long)(cooldownSeconds * 1000);
    }

    /**
     * 取得某功能的剩餘冷卻秒數
     * @return 剩餘秒數（小數），若不在冷卻中則返回 0
     */
    public double getRemainingCooldown(UUID uuid, String feature, double cooldownSeconds) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0;
        Long last = playerCooldowns.get(feature);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long totalMs = (long)(cooldownSeconds * 1000);
        double remaining = (totalMs - elapsed) / 1000.0;
        return Math.max(0, remaining);
    }

    /**
     * 為玩家的某功能設定冷卻（記錄觸發時間）
     */
    public void setCooldown(UUID uuid, String feature) {
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(feature, System.currentTimeMillis());
    }

    /**
     * 強制清除玩家某功能的冷卻
     */
    public void clearCooldown(UUID uuid, String feature) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns != null) {
            playerCooldowns.remove(feature);
        }
    }

    /**
     * 清除玩家所有功能的冷卻（如玩家離開時回收記憶體）
     */
    public void clearAll(UUID uuid) {
        cooldowns.remove(uuid);
    }
}
