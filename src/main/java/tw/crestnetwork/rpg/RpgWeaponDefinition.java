package tw.crestnetwork.rpg;

import java.util.List;

record RpgWeaponDefinition(
        long revisionId,
        int version,
        String key,
        String name,
        String baseItem,
        String oraxenId,
        String rarity,
        int level,
        double attackDamage,
        double attackSpeed,
        double criticalChance,
        Integer maxDurability,
        String toolAbility,
        Integer abilityUnlockLevel,
        int abilityMaxBlocks,
        int abilityRadius,
        int abilityCooldownSeconds,
        List<String> lore,
        String textureUrl,
        String modelUrl,
        boolean enabled
) implements RpgOraxenAsset {}
