package tw.crestnetwork.rpg;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class RpgManifestParser {
    private static final Pattern KEY = Pattern.compile("[a-z0-9_-]{1,64}");
    private static final Pattern NAMESPACED_KEY = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern ORAXEN_ID = Pattern.compile("[a-z0-9_.-]{1,128}");

    private RpgManifestParser() {}

    static RpgManifest parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int schemaVersion = requiredInt(root, "schema_version");
        if (schemaVersion < 1 || schemaVersion > 2) throw new IllegalArgumentException("不支援的 RPG manifest schema");
        String checksum = requiredString(root, "manifest_checksum");
        if (!checksum.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("manifest_checksum 格式錯誤");

        List<RpgMonsterDefinition> monsters = new ArrayList<>();
        long lastRevisionId = 0;
        for (JsonElement element : requiredArray(root, "monsters")) {
            JsonObject monster = element.getAsJsonObject();
            long revisionId = requiredLong(monster, "revision_id");
            int version = requiredInt(monster, "version");
            String key = requiredString(monster, "key");
            String entityType = requiredString(monster, "entity_type").toLowerCase();
            if (!KEY.matcher(key).matches()) throw new IllegalArgumentException("怪物代碼格式錯誤：" + key);
            if (!NAMESPACED_KEY.matcher(entityType).matches()) throw new IllegalArgumentException("實體 ID 格式錯誤：" + entityType);

            List<RpgMonsterDefinition.DropDefinition> drops = new ArrayList<>();
            JsonArray dropArray = monster.has("drops") && monster.get("drops").isJsonArray()
                    ? monster.getAsJsonArray("drops") : new JsonArray();
            for (JsonElement dropElement : dropArray) {
                JsonObject drop = dropElement.getAsJsonObject();
                String itemKey = requiredString(drop, "item_key").toLowerCase();
                double chance = requiredDouble(drop, "chance");
                int min = requiredInt(drop, "min");
                int max = requiredInt(drop, "max");
                if (!NAMESPACED_KEY.matcher(itemKey).matches()) throw new IllegalArgumentException("物品 ID 格式錯誤：" + itemKey);
                if (chance < 0 || chance > 1 || min < 0 || max < min || max > 999) {
                    throw new IllegalArgumentException("掉落物設定錯誤：" + itemKey);
                }
                drops.add(new RpgMonsterDefinition.DropDefinition(itemKey, chance, min, max));
            }

            double maxHealth = requiredDouble(monster, "max_health");
            double damage = requiredDouble(monster, "damage");
            int level = requiredInt(monster, "level");
            if (revisionId < 1 || version < 1 || level < 1 || maxHealth <= 0 || damage < 0) {
                throw new IllegalArgumentException("怪物數值錯誤：" + key);
            }
            monsters.add(new RpgMonsterDefinition(
                    revisionId,
                    version,
                    key,
                    requiredString(monster, "name"),
                    entityType,
                    optionalString(monster, "model_engine_id"),
                    level,
                    maxHealth,
                    damage,
                    List.copyOf(drops),
                    optionalString(monster, "texture_url"),
                    optionalString(monster, "model_url"),
                    !monster.has("enabled") || monster.get("enabled").getAsBoolean()
            ));
            lastRevisionId = Math.max(lastRevisionId, revisionId);
        }

        List<RpgWeaponDefinition> weapons = new ArrayList<>();
        JsonArray weaponArray = root.has("weapons") && root.get("weapons").isJsonArray()
                ? root.getAsJsonArray("weapons") : new JsonArray();
        for (JsonElement element : weaponArray) {
            JsonObject weapon = element.getAsJsonObject();
            long revisionId = requiredLong(weapon, "revision_id");
            int version = requiredInt(weapon, "version");
            String key = requiredString(weapon, "key").toLowerCase();
            String baseItem = requiredString(weapon, "base_item").toLowerCase();
            String oraxenId = optionalString(weapon, "oraxen_id");
            int level = requiredInt(weapon, "level");
            double attackDamage = requiredDouble(weapon, "attack_damage");
            double attackSpeed = requiredDouble(weapon, "attack_speed");
            double criticalChance = requiredDouble(weapon, "critical_chance");
            Integer maxDurability = weapon.has("max_durability") && !weapon.get("max_durability").isJsonNull()
                    ? weapon.get("max_durability").getAsInt() : null;
            String toolAbility = optionalString(weapon, "tool_ability");
            Integer abilityUnlockLevel = optionalInteger(weapon, "ability_unlock_level");
            int abilityMaxBlocks = optionalInt(weapon, "ability_max_blocks", 32);
            int abilityRadius = optionalInt(weapon, "ability_radius", 3);
            int abilityCooldownSeconds = optionalInt(weapon, "ability_cooldown_seconds", 2);

            if (!KEY.matcher(key).matches()) throw new IllegalArgumentException("武器代碼格式錯誤：" + key);
            if (!NAMESPACED_KEY.matcher(baseItem).matches()) throw new IllegalArgumentException("基礎物品 ID 格式錯誤：" + baseItem);
            if (oraxenId != null && !ORAXEN_ID.matcher(oraxenId).matches()) throw new IllegalArgumentException("Oraxen ID 格式錯誤：" + oraxenId);
            if (revisionId < 1 || version < 1 || level < 1 || attackDamage < 0 || attackSpeed <= 0
                    || criticalChance < 0 || criticalChance > 1 || (maxDurability != null && maxDurability < 1)) {
                throw new IllegalArgumentException("武器數值錯誤：" + key);
            }
            if (toolAbility != null && !List.of("vein_mining", "tree_felling", "right_click_harvest").contains(toolAbility)) {
                throw new IllegalArgumentException("工具能力格式錯誤：" + key);
            }
            if (abilityMaxBlocks < 1 || abilityMaxBlocks > 256 || abilityRadius < 1 || abilityRadius > 8
                    || abilityCooldownSeconds < 0 || (abilityUnlockLevel != null && abilityUnlockLevel < 1)) {
                throw new IllegalArgumentException("工具能力數值錯誤：" + key);
            }

            List<String> lore = parseLore(weapon, key);

            weapons.add(new RpgWeaponDefinition(
                    revisionId,
                    version,
                    key,
                    requiredString(weapon, "name"),
                    baseItem,
                    oraxenId == null ? key : oraxenId,
                    optionalString(weapon, "rarity") == null ? "common" : optionalString(weapon, "rarity"),
                    level,
                    attackDamage,
                    attackSpeed,
                    criticalChance,
                    maxDurability,
                    toolAbility,
                    abilityUnlockLevel,
                    abilityMaxBlocks,
                    abilityRadius,
                    abilityCooldownSeconds,
                    List.copyOf(lore),
                    optionalString(weapon, "texture_url"),
                    optionalString(weapon, "model_url"),
                    !weapon.has("enabled") || weapon.get("enabled").getAsBoolean()
            ));
            lastRevisionId = Math.max(lastRevisionId, revisionId);
        }

        List<RpgEquipmentDefinition> equipments = new ArrayList<>();
        JsonArray equipmentArray = root.has("equipments") && root.get("equipments").isJsonArray()
                ? root.getAsJsonArray("equipments") : new JsonArray();
        for (JsonElement element : equipmentArray) {
            JsonObject value = element.getAsJsonObject();
            long revisionId = requiredLong(value, "revision_id");
            String key = requiredString(value, "key").toLowerCase();
            String baseItem = requiredString(value, "base_item").toLowerCase();
            String oraxenId = optionalString(value, "oraxen_id");
            String slot = requiredString(value, "equipment_slot").toLowerCase();
            if (!KEY.matcher(key).matches() || !NAMESPACED_KEY.matcher(baseItem).matches()
                    || !List.of("head", "chest", "legs", "feet", "off_hand").contains(slot)) {
                throw new IllegalArgumentException("裝備格式錯誤：" + key);
            }
            equipments.add(new RpgEquipmentDefinition(
                    revisionId, requiredInt(value, "version"), key, requiredString(value, "name"), baseItem,
                    oraxenId == null ? key : oraxenId, defaultString(value, "rarity", "common"), requiredInt(value, "level"), slot,
                    requiredDouble(value, "armor"), requiredDouble(value, "armor_toughness"),
                    requiredDouble(value, "knockback_resistance"), requiredDouble(value, "health_bonus"),
                    List.copyOf(parseLore(value, key)), optionalString(value, "texture_url"), optionalString(value, "model_url"),
                    !value.has("enabled") || value.get("enabled").getAsBoolean()
            ));
            lastRevisionId = Math.max(lastRevisionId, revisionId);
        }

        List<RpgItemDefinition> items = new ArrayList<>();
        JsonArray itemArray = root.has("items") && root.get("items").isJsonArray()
                ? root.getAsJsonArray("items") : new JsonArray();
        for (JsonElement element : itemArray) {
            JsonObject value = element.getAsJsonObject();
            long revisionId = requiredLong(value, "revision_id");
            String key = requiredString(value, "key").toLowerCase();
            String baseItem = requiredString(value, "base_item").toLowerCase();
            String oraxenId = optionalString(value, "oraxen_id");
            int maxStack = requiredInt(value, "max_stack_size");
            if (!KEY.matcher(key).matches() || !NAMESPACED_KEY.matcher(baseItem).matches() || maxStack < 1 || maxStack > 99) {
                throw new IllegalArgumentException("物品格式錯誤：" + key);
            }
            items.add(new RpgItemDefinition(
                    revisionId, requiredInt(value, "version"), key, requiredString(value, "name"), baseItem,
                    oraxenId == null ? key : oraxenId, defaultString(value, "rarity", "common"), requiredInt(value, "level"), maxStack,
                    List.copyOf(parseLore(value, key)), optionalString(value, "texture_url"), optionalString(value, "model_url"),
                    !value.has("enabled") || value.get("enabled").getAsBoolean()
            ));
            lastRevisionId = Math.max(lastRevisionId, revisionId);
        }

        List<RpgGameplayDefinition> gameplay = new ArrayList<>();
        JsonArray gameplayArray = root.has("gameplay") && root.get("gameplay").isJsonArray()
                ? root.getAsJsonArray("gameplay") : new JsonArray();
        for (JsonElement element : gameplayArray) {
            JsonObject value = element.getAsJsonObject();
            long revisionId = requiredLong(value, "revision_id");
            int version = requiredInt(value, "version");
            String section = requiredString(value, "section").toLowerCase();
            if (!KEY.matcher(section).matches() || revisionId < 1 || version < 1) {
                throw new IllegalArgumentException("遊戲規則格式錯誤：" + section);
            }
            if (!value.has("data") || !value.get("data").isJsonObject()) {
                throw new IllegalArgumentException("遊戲規則缺少 data：" + section);
            }
            gameplay.add(new RpgGameplayDefinition(
                    revisionId, version, section, requiredString(value, "name"),
                    value.getAsJsonObject("data").deepCopy(),
                    !value.has("enabled") || value.get("enabled").getAsBoolean()
            ));
            lastRevisionId = Math.max(lastRevisionId, revisionId);
        }

        return new RpgManifest(checksum, List.copyOf(monsters), List.copyOf(weapons),
                List.copyOf(equipments), List.copyOf(items), List.copyOf(gameplay), lastRevisionId);
    }

    private static List<String> parseLore(JsonObject object, String key) {
        List<String> lore = new ArrayList<>();
        if (object.has("lore") && object.get("lore").isJsonArray()) {
            for (JsonElement line : object.getAsJsonArray("lore")) {
                if (!line.isJsonPrimitive()) throw new IllegalArgumentException("Lore 格式錯誤：" + key);
                String text = line.getAsString().trim();
                if (!text.isEmpty()) lore.add(text.substring(0, Math.min(200, text.length())));
                if (lore.size() >= 30) break;
            }
        }
        return lore;
    }

    private static String defaultString(JsonObject object, String key, String fallback) {
        String value = optionalString(object, key);
        return value == null ? fallback : value;
    }

    private static Integer optionalInteger(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : null;
    }

    private static int optionalInt(JsonObject object, String key, int fallback) {
        Integer value = optionalInteger(object, key);
        return value == null ? fallback : value;
    }

    private static JsonArray requiredArray(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) throw new IllegalArgumentException("缺少 " + key);
        return object.getAsJsonArray(key);
    }

    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("缺少 " + key);
        String value = object.get(key).getAsString().trim();
        if (value.isEmpty()) throw new IllegalArgumentException(key + " 不可空白");
        return value;
    }

    private static String optionalString(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()) return null;
        String value = object.get(key).getAsString().trim();
        return value.isEmpty() ? null : value;
    }

    private static int requiredInt(JsonObject object, String key) {
        if (!object.has(key)) throw new IllegalArgumentException("缺少 " + key);
        return object.get(key).getAsInt();
    }

    private static long requiredLong(JsonObject object, String key) {
        if (!object.has(key)) throw new IllegalArgumentException("缺少 " + key);
        return object.get(key).getAsLong();
    }

    private static double requiredDouble(JsonObject object, String key) {
        if (!object.has(key)) throw new IllegalArgumentException("缺少 " + key);
        return object.get(key).getAsDouble();
    }
}
