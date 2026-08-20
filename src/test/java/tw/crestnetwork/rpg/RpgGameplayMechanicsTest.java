package tw.crestnetwork.rpg;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class RpgGameplayMechanicsTest {

    @Test
    void testRegistryParsingAndSetBonuses() {
        JsonObject itemSystem = new JsonObject();

        JsonObject setObj = new JsonObject();
        setObj.addProperty("key", "dragon_set");
        setObj.addProperty("name", "Dragon Set");

        JsonObject bonus2 = new JsonObject();
        bonus2.addProperty("pieces", 2);
        JsonObject stats2 = new JsonObject();
        stats2.addProperty("damage", 15.0);
        bonus2.add("stats", stats2);

        com.google.gson.JsonArray bonuses = new com.google.gson.JsonArray();
        bonuses.add(bonus2);
        setObj.add("bonuses", bonuses);

        com.google.gson.JsonArray setsArray = new com.google.gson.JsonArray();
        setsArray.add(setObj);
        itemSystem.add("sets", setsArray);

        RpgGameplayDefinition def = new RpgGameplayDefinition(1, 1, "item-system", "Item System", itemSystem, true);
        RpgEngineRegistry registry = RpgEngineRegistry.from(List.of(def));

        assertNotNull(registry.sets().get("dragon_set"));
        assertEquals("Dragon Set", registry.sets().get("dragon_set").name());
        assertEquals(15.0, registry.sets().get("dragon_set").bonuses().get(2).get("damage"));
    }

    @Test
    void testSkillDefinitionValidation() {
        JsonObject skillsObj = new JsonObject();
        JsonObject skill = new JsonObject();
        skill.addProperty("key", "fireball");
        skill.addProperty("name", "Fireball");
        skill.addProperty("trigger", "right_click");
        skill.addProperty("mana_cost", 25.0);
        skill.addProperty("cooldown_seconds", 5.0);

        com.google.gson.JsonArray effects = new com.google.gson.JsonArray();
        JsonObject effect = new JsonObject();
        effect.addProperty("type", "damage");
        effects.add(effect);
        skill.add("effects", effects);

        com.google.gson.JsonArray defs = new com.google.gson.JsonArray();
        defs.add(skill);
        skillsObj.add("definitions", defs);

        RpgGameplayDefinition def = new RpgGameplayDefinition(1, 1, "skills", "Skills", skillsObj, true);
        RpgEngineRegistry registry = RpgEngineRegistry.from(List.of(def));

        assertNotNull(registry.skills().get("fireball"));
        assertEquals(25.0, registry.skills().get("fireball").manaCost());
    }

    @Test
    void loadsAdvancedCastPipelineAndEffects() {
        JsonObject skills = com.google.gson.JsonParser.parseString("""
                {"definitions":[{"key":"storm","trigger":"manual","mana_cost":30,"cooldown_seconds":8,
                "cast_time_seconds":1.5,"interruptible":true,"repeats":3,"repeat_interval_seconds":0.25,
                "effects":[
                  {"type":"projectile","speed":2,"effects":[{"type":"damage","base":8,"intelligence_scale":0.5}]},
                  {"type":"area","radius":5,"effects":[{"type":"damage_over_time","value":2,"pulses":4}]},
                  {"type":"shield","base":20,"level_scale":1.5},
                  {"type":"summon","entity":"WOLF","seconds":10}
                ]}]}
                """).getAsJsonObject();
        RpgEngineRegistry registry = RpgEngineRegistry.from(List.of(
                new RpgGameplayDefinition(1, 1, "skills", "skills", skills, true)));
        RpgEngineRegistry.SkillDefinition skill = registry.skills().get("storm");
        assertEquals(1.5, skill.castTimeSeconds());
        assertEquals(3, skill.repeats());
        assertEquals("manual", skill.trigger());
        assertEquals(4, skill.effects().size());
    }
}
