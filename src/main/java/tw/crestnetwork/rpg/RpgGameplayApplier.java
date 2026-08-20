package tw.crestnetwork.rpg;

import com.google.gson.Gson;
import java.util.Map;
import java.util.Set;

final class RpgGameplayApplier {
    private static final Set<String> ALLOWED_SECTIONS = Set.of(
            "attributes", "mana", "progression", "class-skills", "actionbar", "actionbar-healthbar",
            "life-skills", "chain-mining", "treecapitator", "auto-harvest",
            "sort-inventory", "sort-chest", "mob-rules", "quests", "item-system",
            "classes", "skills", "skill-trees", "crafting-stations", "drop-tables", "item-operations", "npcs"
    );
    private final Gson gson = new Gson();
    private final CrestRpgPlugin plugin;

    RpgGameplayApplier(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    void apply(RpgManifest manifest) {
        int applied = 0;
        RpgGameplayFileSynchronizer files = new RpgGameplayFileSynchronizer(plugin);
        for (RpgGameplayDefinition definition : manifest.gameplay()) {
            if (!definition.enabled() || !ALLOWED_SECTIONS.contains(definition.section())) continue;
            if (files.synchronize(definition)) {
                applied++;
                continue;
            }
            Map<?, ?> values = gson.fromJson(definition.data(), Map.class);
            plugin.getConfig().set(definition.section(), values);
            applied++;
        }
        if (applied == 0) return;
        plugin.saveConfig();
        plugin.refreshPlayerProgressionRules();
        if (plugin.getNpcEngine() != null) {
            plugin.getNpcEngine().loadNpcsFromDrafts(plugin.getDraftManager());
        }
        plugin.getLogger().info("已套用 " + applied + " 個網站 RPG 遊戲規則區段。");
    }
}
