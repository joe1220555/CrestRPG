package tw.crestnetwork.rpg;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpgEngineRegistryTest {
    @Test
    void loadsStandaloneItemClassSkillAndCraftingDefinitions() {
        RpgEngineRegistry registry = RpgEngineRegistry.from(List.of(
                section("item-system", """
                    {"affixes":[{"key":"strong","stat":"strength","min":2,"max":8}],
                     "gems":[{"key":"ruby","stat":"strength","value":5,"allowed_slots":["weapon"]}],
                     "sets":[{"key":"dragon","name":"龍炎套裝","bonuses":[{"pieces":2,"stats":{"strength":10}}]}],
                     "templates":[{"key":"dragon_blade","rarity":"legendary","sockets":2,"max_upgrade":10,
                       "set_id":"dragon","affix_pool":["strong"],"requires_identification":true}]}
                    """),
                section("skills", """
                    {"definitions":[{"key":"fireball","trigger":"right_click","mana_cost":20,
                    "conditions":[{"type":"class","value":"mage"}],"effects":[{"type":"damage","value":12}]}]}
                    """),
                section("classes", """
                    {"definitions":[{"key":"mage","base_mana":150,"skills":["fireball"]}]}
                    """),
                section("skill-trees", """
                    {"definitions":[{"key":"mage_tree","class_id":"mage","nodes":[
                      {"key":"fireball_node","skill":"fireball","cost":2,"required_level":3,"slot":10}]}]}
                    """),
                section("crafting-stations", """
                    {"definitions":[{"key":"forge","recipes":[{"key":"blade","output":"dragon_blade",
                      "ingredients":[{"key":"minecraft:iron_ingot","amount":8}]}]}]}
                    """),
                section("drop-tables", """
                    {"definitions":[{"key":"dragon_loot","rolls":2,"sources":["fire_dragon"],"entries":[
                      {"key":"dragon_blade","weight":1,"min":1,"max":1,"content":true},
                      {"key":"minecraft:diamond","weight":3,"min":1,"max":2,"content":false}]}]}
                    """)
        ));

        assertEquals(2, registry.items().get("dragon_blade").sockets());
        assertEquals(1, registry.affixes().size());
        assertEquals(1, registry.gems().size());
        assertEquals(1, registry.sets().size());
        assertEquals(150, registry.classes().get("mage").baseMana());
        assertEquals("right_click", registry.skills().get("fireball").trigger());
        assertEquals(2, registry.skillTrees().get("mage_tree").nodes().getFirst().cost());
        assertEquals(1, registry.craftingStations().get("forge").recipes().size());
        assertEquals(2, registry.dropTables().get("dragon_loot").rolls());
        assertEquals(4, registry.dropTables().get("dragon_loot").totalWeight());
    }

    @Test
    void rejectsUnknownAffixReferences() {
        RpgGameplayDefinition items = section("item-system", """
                {"templates":[{"key":"bad","affix_pool":["missing"]}]}
                """);
        assertThrows(IllegalArgumentException.class, () -> RpgEngineRegistry.from(List.of(items)));
    }

    private static RpgGameplayDefinition section(String key, String json) {
        return new RpgGameplayDefinition(1, 1, key, key, JsonParser.parseString(json).getAsJsonObject(), true);
    }
}
