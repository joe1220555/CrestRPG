package tw.crestnetwork.rpg;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpgItemRollerTest {
    @Test
    void rollsQualitySocketsAndRarityAffixCountDeterministically() {
        RpgGameplayDefinition section = new RpgGameplayDefinition(1, 1, "item-system", "items",
                JsonParser.parseString("""
                    {"affixes":[
                      {"key":"power","stat":"strength","min":5,"max":10,"weight":1},
                      {"key":"life","stat":"vitality","min":3,"max":8,"weight":1},
                      {"key":"speed","stat":"dexterity","min":1,"max":4,"weight":1}],
                     "templates":[{"key":"blade","rarity":"epic","quality_min":70,"quality_max":90,
                       "sockets":2,"max_upgrade":10,"affix_pool":["power","life","speed"],
                       "requires_identification":true}]}
                    """).getAsJsonObject(), true);

        RpgItemRoller.RolledItem item = RpgItemRoller.roll(RpgEngineRegistry.from(List.of(section)), "blade", new Random(42));

        assertNotNull(item);
        assertTrue(item.quality() >= 70 && item.quality() <= 90);
        assertEquals(3, item.affixes().size());
        assertEquals(2, item.gems().size());
        assertTrue(item.unidentified());
    }
}
