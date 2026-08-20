package tw.crestnetwork.rpg;

import java.util.List;

record RpgItemDefinition(
        long revisionId, int version, String key, String name, String baseItem, String oraxenId,
        String rarity, int level, int maxStackSize, List<String> lore,
        String textureUrl, String modelUrl, boolean enabled
) implements RpgOraxenAsset {}
