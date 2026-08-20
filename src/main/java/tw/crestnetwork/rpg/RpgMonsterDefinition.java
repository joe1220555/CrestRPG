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
        List<DropDefinition> drops,
        String textureUrl,
        String modelUrl,
        boolean enabled
) {
    record DropDefinition(String itemKey, double chance, int min, int max) {}
}
