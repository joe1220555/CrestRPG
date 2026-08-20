package tw.crestnetwork.rpg;

import java.util.List;

record RpgEquipmentDefinition(
        long revisionId, int version, String key, String name, String baseItem, String oraxenId,
        String rarity, int level, String equipmentSlot, double armor, double armorToughness,
        double knockbackResistance, double healthBonus, List<String> lore,
        String textureUrl, String modelUrl, boolean enabled
) implements RpgOraxenAsset {}
