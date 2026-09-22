package tw.crestnetwork.rpg;

import java.util.List;

record RpgMonsterDefinition(
        long revisionId,
        int version,
        String key,
        String name,
        String entityType,
        String modelEngineId,
        int level,
        double maxHealth,
        double damage,
        double movementSpeed,
        double followRange,
        double armor,
        double knockbackResistance,
        List<EquipmentDefinition> equipment,
        List<DropDefinition> drops,
        String textureUrl,
        String modelUrl,
        boolean enabled
) {
    record EquipmentDefinition(String slot, String itemKey, double chance) {}
    record DropDefinition(String itemKey, double chance, int min, int max) {}
}
