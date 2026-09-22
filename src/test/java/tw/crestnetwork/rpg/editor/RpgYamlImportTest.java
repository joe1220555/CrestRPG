package tw.crestnetwork.rpg.editor;

import com.google.gson.JsonObject;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RpgYamlImportTest {
    @Test
    void convertsNestedBukkitConfigurationWithoutReflectingIntoServerObjects() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("Rewards.Money", 500);
        yaml.set("Rewards.SkillsXP.FARMING", 250.0);
        yaml.set("Rewards.Items", List.of(Map.of("Material", "BREAD", "Amount", 16)));

        JsonObject rewards = RpgDraftManager.sectionObject(yaml.getConfigurationSection("Rewards"));

        assertEquals(500, rewards.get("Money").getAsInt());
        assertEquals(250.0, rewards.getAsJsonObject("SkillsXP").get("FARMING").getAsDouble());
        assertTrue(rewards.getAsJsonArray("Items").get(0).getAsJsonObject().has("Material"));
    }
}
