package tw.crestnetwork.rpg.title;

import java.util.Map;

public record RpgTitleDefinition(
        String key,
        String displayName,
        String description,
        Map<String, Double> statBonuses
) {}
