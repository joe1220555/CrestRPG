package tw.crestnetwork.rpg;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpgManifestParserTest {
    @Test
    void parsesWebsiteManagedGameplayRules() {
        String json = """
                {
                  "schema_version": 2,
                  "manifest_checksum": "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                  "monsters": [],
                  "gameplay": [{
                    "revision_id": 30,
                    "version": 2,
                    "section": "life-skills",
                    "name": "生活技能",
                    "enabled": true,
                    "data": {"chain-mining":{"enabled":true,"max-blocks":64}}
                  }]
                }
                """;

        RpgManifest manifest = RpgManifestParser.parse(json);

        assertEquals("life-skills", manifest.gameplay().getFirst().section());
        assertEquals(64, manifest.gameplay().getFirst().data().getAsJsonObject("chain-mining").get("max-blocks").getAsInt());
        assertEquals(30, manifest.lastRevisionId());
    }

    @Test
    void parsesPublishedMonsterManifest() {
        String json = """
                {
                  "schema_version": 1,
                  "manifest_checksum": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "monsters": [{
                    "revision_id": 12,
                    "version": 3,
                    "key": "forest_slime",
                    "name": "森林史萊姆",
                    "entity_type": "minecraft:slime",
                    "level": 4,
                    "max_health": 40,
                    "damage": 6,
                    "enabled": true,
                    "drops": [{"item_key":"minecraft:slime_ball","chance":0.5,"min":1,"max":2}]
                  }]
                }
                """;

        RpgManifest manifest = RpgManifestParser.parse(json);

        assertEquals(12, manifest.lastRevisionId());
        assertEquals("forest_slime", manifest.monsters().getFirst().key());
        assertEquals("minecraft:slime_ball", manifest.monsters().getFirst().drops().getFirst().itemKey());
    }

    @Test
    void rejectsUnsafeDropValues() {
        String json = """
                {
                  "schema_version": 1,
                  "manifest_checksum": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "monsters": [{
                    "revision_id": 1, "version": 1, "key": "bad", "name": "Bad",
                    "entity_type": "minecraft:zombie", "level": 1, "max_health": 20, "damage": 1,
                    "drops": [{"item_key":"minecraft:diamond","chance":2,"min":1,"max":1}]
                  }]
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> RpgManifestParser.parse(json));
    }

    @Test
    void parsesPublishedWebsiteWeapon() {
        String json = """
                {
                  "schema_version": 1,
                  "manifest_checksum": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                  "monsters": [],
                  "weapons": [{
                    "revision_id": 18,
                    "version": 2,
                    "key": "forest_blade",
                    "name": "森林之刃",
                    "base_item": "minecraft:iron_sword",
                    "oraxen_id": "forest_blade",
                    "rarity": "rare",
                    "level": 5,
                    "attack_damage": 12,
                    "attack_speed": 1.6,
                    "critical_chance": 0.15,
                    "max_durability": 500,
                    "lore": ["森林守護者使用的長劍"],
                    "texture_url": "https://crest.example/storage/forest_blade.png",
                    "model_url": null,
                    "enabled": true
                  }]
                }
                """;

        RpgManifest manifest = RpgManifestParser.parse(json);

        assertEquals(18, manifest.lastRevisionId());
        assertEquals("forest_blade", manifest.weapons().getFirst().key());
        assertEquals("minecraft:iron_sword", manifest.weapons().getFirst().baseItem());
        assertEquals(12, manifest.weapons().getFirst().attackDamage());
        assertEquals("森林守護者使用的長劍", manifest.weapons().getFirst().lore().getFirst());
    }

    @Test
    void rejectsUnsafeWeaponValues() {
        String json = """
                {
                  "schema_version": 1,
                  "manifest_checksum": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                  "monsters": [],
                  "weapons": [{
                    "revision_id": 1, "version": 1, "key": "bad", "name": "Bad",
                    "base_item": "minecraft:iron_sword", "level": 1,
                    "attack_damage": 1, "attack_speed": 1.6, "critical_chance": 2
                  }]
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> RpgManifestParser.parse(json));
    }

    @Test
    void parsesToolAbilitiesEquipmentAndItems() {
        String json = """
                {
                  "schema_version": 1,
                  "manifest_checksum": "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                  "monsters": [],
                  "weapons": [{
                    "revision_id": 20, "version": 1, "key": "miner_pickaxe", "name": "礦工鎬",
                    "base_item": "minecraft:diamond_pickaxe", "level": 5, "attack_damage": 5,
                    "attack_speed": 1.2, "critical_chance": 0, "tool_ability": "vein_mining",
                    "ability_unlock_level": 12, "ability_max_blocks": 48, "ability_radius": 4,
                    "ability_cooldown_seconds": 3
                  }],
                  "equipments": [{
                    "revision_id": 21, "version": 1, "key": "forest_helm", "name": "森林頭盔",
                    "base_item": "minecraft:diamond_helmet", "rarity": "rare", "level": 8,
                    "equipment_slot": "head", "armor": 4, "armor_toughness": 2,
                    "knockback_resistance": 0.1, "health_bonus": 2
                  }],
                  "items": [{
                    "revision_id": 22, "version": 1, "key": "forest_crystal", "name": "森林結晶",
                    "base_item": "minecraft:amethyst_shard", "rarity": "uncommon", "level": 1,
                    "max_stack_size": 64
                  }]
                }
                """;

        RpgManifest manifest = RpgManifestParser.parse(json);

        assertEquals("vein_mining", manifest.weapons().getFirst().toolAbility());
        assertEquals(12, manifest.weapons().getFirst().abilityUnlockLevel());
        assertEquals("head", manifest.equipments().getFirst().equipmentSlot());
        assertEquals("forest_crystal", manifest.items().getFirst().key());
        assertEquals(22, manifest.lastRevisionId());
    }
}
