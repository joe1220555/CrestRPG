package tw.crestnetwork.rpg;

record ToolAbilityDefinition(
        String key,
        String ability,
        int unlockLevel,
        int maxBlocks,
        int radius,
        int cooldownSeconds
) {}
