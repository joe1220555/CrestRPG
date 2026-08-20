package tw.crestnetwork.rpg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

final class RpgItemRoller {
    private RpgItemRoller() {}

    static RolledItem roll(RpgEngineRegistry registry, String templateKey, RandomGenerator random) {
        RpgEngineRegistry.ItemTemplate template = registry.items().get(templateKey);
        if (template == null) return null;
        double quality = template.qualityMin() + random.nextDouble() * (template.qualityMax() - template.qualityMin());
        int affixCount = Math.min(template.affixPool().size(), switch (template.rarity()) {
            case "uncommon" -> 1;
            case "rare" -> 2;
            case "epic" -> 3;
            case "legendary" -> 4;
            case "mythic" -> 5;
            default -> 0;
        });
        List<String> candidates = new ArrayList<>(template.affixPool());
        Map<String, Double> affixes = new LinkedHashMap<>();
        for (int i = 0; i < affixCount && !candidates.isEmpty(); i++) {
            String chosen = chooseWeighted(candidates, registry, random);
            candidates.remove(chosen);
            RpgEngineRegistry.AffixDefinition definition = registry.affixes().get(chosen);
            double raw = definition.min() + random.nextDouble() * (definition.max() - definition.min());
            double qualityMultiplier = 0.75 + quality / 200.0;
            affixes.put(chosen, round(raw * qualityMultiplier));
        }
        return new RolledItem(templateKey, round(quality), template.requiresIdentification(), 0,
                Map.copyOf(affixes), java.util.Collections.nCopies(template.sockets(), ""));
    }

    private static String chooseWeighted(List<String> candidates, RpgEngineRegistry registry, RandomGenerator random) {
        double total = candidates.stream().map(registry.affixes()::get).mapToDouble(RpgEngineRegistry.AffixDefinition::weight).sum();
        double cursor = random.nextDouble(total);
        for (String key : candidates) {
            cursor -= registry.affixes().get(key).weight();
            if (cursor <= 0) return key;
        }
        return candidates.getLast();
    }

    private static double round(double value) { return Math.round(value * 100.0) / 100.0; }

    record RolledItem(String templateKey, double quality, boolean unidentified, int upgradeLevel,
                      Map<String, Double> affixes, List<String> gems) {
        RolledItem {
            affixes = Map.copyOf(affixes);
            gems = List.copyOf(gems);
        }
    }
}
