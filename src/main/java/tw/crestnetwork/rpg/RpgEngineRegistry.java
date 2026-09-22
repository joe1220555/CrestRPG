package tw.crestnetwork.rpg;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable, validated snapshot of the standalone CrestRPG engine definitions. */
public final class RpgEngineRegistry {
    private static final Pattern KEY = Pattern.compile("[a-z0-9_-]{1,64}");
    private static final Set<String> ITEM_RARITIES = Set.of("common", "uncommon", "rare", "epic", "legendary", "mythic");
    private static final Set<String> TRIGGERS = Set.of(
            "passive", "left_click", "right_click", "shift_left_click", "shift_right_click",
            "damage_dealt", "damage_taken", "kill", "block_break", "projectile_hit", "manual"
    );
    private static final Set<String> CONDITIONS = Set.of(
            "class", "min_level", "permission", "sneaking", "health_below", "chance", "weapon_type", "target_type"
    );
    private static final Set<String> EFFECTS = Set.of(
            "damage", "heal", "mana", "message", "potion", "sound", "particle", "knockback", "fire", "command",
            "area", "beam", "chain", "projectile", "dash", "teleport", "shield", "damage_over_time",
            "heal_over_time", "lifesteal", "summon", "sequence"
    );

    private final Map<String, ItemTemplate> items;
    private final Map<String, AffixDefinition> affixes;
    private final Map<String, GemDefinition> gems;
    private final Map<String, SetDefinition> sets;
    private final Map<String, ClassDefinition> classes;
    private final Map<String, SkillDefinition> skills;
    private final Map<String, TreeDefinition> skillTrees;
    private final Map<String, CraftingStationDefinition> craftingStations;
    private final Map<String, DropTableDefinition> dropTables;

    private RpgEngineRegistry(
            Map<String, ItemTemplate> items,
            Map<String, AffixDefinition> affixes,
            Map<String, GemDefinition> gems,
            Map<String, SetDefinition> sets,
            Map<String, ClassDefinition> classes,
            Map<String, SkillDefinition> skills,
            Map<String, TreeDefinition> skillTrees,
            Map<String, CraftingStationDefinition> craftingStations,
            Map<String, DropTableDefinition> dropTables
    ) {
        this.items = Map.copyOf(items);
        this.affixes = Map.copyOf(affixes);
        this.gems = Map.copyOf(gems);
        this.sets = Map.copyOf(sets);
        this.classes = Map.copyOf(classes);
        this.skills = Map.copyOf(skills);
        this.skillTrees = Map.copyOf(skillTrees);
        this.craftingStations = Map.copyOf(craftingStations);
        this.dropTables = Map.copyOf(dropTables);
    }

    static RpgEngineRegistry empty() {
        return new RpgEngineRegistry(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    static RpgEngineRegistry from(List<RpgGameplayDefinition> gameplay) {
        Map<String, JsonObject> sections = new LinkedHashMap<>();
        for (RpgGameplayDefinition definition : gameplay) {
            if (definition.enabled()) sections.put(definition.section(), definition.data());
        }
        Map<String, ItemTemplate> items = parseItems(sections.get("item-system"));
        Map<String, AffixDefinition> affixes = parseAffixes(sections.get("item-system"));
        Map<String, GemDefinition> gems = parseGems(sections.get("item-system"));
        Map<String, SetDefinition> sets = parseSets(sections.get("item-system"));
        Map<String, ClassDefinition> classes = parseClasses(sections.get("classes"));
        Map<String, SkillDefinition> skills = parseSkills(sections.get("skills"));
        Map<String, TreeDefinition> trees = parseTrees(sections.get("skill-trees"));
        Map<String, CraftingStationDefinition> stations = parseStations(sections.get("crafting-stations"));
        Map<String, DropTableDefinition> drops = parseDropTables(sections.get("drop-tables"));

        for (ItemTemplate item : items.values()) {
            if (item.setId() != null && !sets.containsKey(item.setId())) fail("物品引用不存在的套裝：" + item.key());
            for (String affix : item.affixPool()) if (!affixes.containsKey(affix)) fail("物品引用不存在的詞綴：" + affix);
        }
        for (ClassDefinition playerClass : classes.values()) {
            for (String skill : playerClass.skills()) if (!skills.containsKey(skill)) fail("職業引用不存在的技能：" + skill);
        }
        for (TreeDefinition tree : trees.values()) {
            if (tree.classId() != null && !classes.containsKey(tree.classId())) fail("技能樹引用不存在的職業：" + tree.classId());
            for (TreeNode node : tree.nodes()) if (!skills.containsKey(node.skill())) fail("技能樹引用不存在的技能：" + node.skill());
        }
        return new RpgEngineRegistry(items, affixes, gems, sets, classes, skills, trees, stations, drops);
    }

    public Map<String, ItemTemplate> items() { return items; }
    Map<String, AffixDefinition> affixes() { return affixes; }
    Map<String, GemDefinition> gems() { return gems; }
    Map<String, SetDefinition> sets() { return sets; }
    Map<String, ClassDefinition> classes() { return classes; }
    Map<String, SkillDefinition> skills() { return skills; }
    Map<String, TreeDefinition> skillTrees() { return skillTrees; }
    Map<String, CraftingStationDefinition> craftingStations() { return craftingStations; }
    Map<String, DropTableDefinition> dropTables() { return dropTables; }

    private static Map<String, ItemTemplate> parseItems(JsonObject root) {
        Map<String, ItemTemplate> result = new LinkedHashMap<>();
        for (JsonObject value : objects(root, "templates")) {
            String key = key(value);
            String rarity = string(value, "rarity", "common").toLowerCase();
            if (!ITEM_RARITIES.contains(rarity)) fail("未知稀有度：" + rarity);
            int sockets = integer(value, "sockets", 0, 0, 6);
            int maxUpgrade = integer(value, "max_upgrade", 0, 0, 20);
            put(result, key, new ItemTemplate(key, rarity, number(value, "quality_min", 0, 0, 100),
                    number(value, "quality_max", 100, 0, 100), sockets, maxUpgrade,
                    optionalKey(value, "set_id"), strings(value, "affix_pool"),
                    bool(value, "requires_identification", false), number(value, "salvage_value", 0, 0, 1_000_000),
                    string(value, "material", "stone"), string(value, "name", key), numberMap(value, "base_stats")));
        }
        return result;
    }

    private static Map<String, AffixDefinition> parseAffixes(JsonObject root) {
        Map<String, AffixDefinition> result = new LinkedHashMap<>();
        for (JsonObject value : objects(root, "affixes")) {
            String key = key(value);
            double min = number(value, "min", 0, -1_000_000, 1_000_000);
            double max = number(value, "max", min, -1_000_000, 1_000_000);
            if (max < min) fail("詞綴最大值不可小於最小值：" + key);
            put(result, key, new AffixDefinition(key, required(value, "stat"), min, max,
                    number(value, "weight", 1, 0.0001, 1_000_000), string(value, "name", key)));
        }
        return result;
    }

    private static Map<String, GemDefinition> parseGems(JsonObject root) {
        Map<String, GemDefinition> result = new LinkedHashMap<>();
        for (JsonObject value : objects(root, "gems")) {
            String key = key(value);
            Map<String, Double> stats = new LinkedHashMap<>();
            if (value.has("stats") && value.get("stats").isJsonArray()) for (JsonObject stat : objects(value, "stats")) {
                String statKey = required(stat, "stat").toLowerCase();
                stats.merge(statKey, number(stat, "value", 0, -1_000_000, 1_000_000), Double::sum);
            }
            if (stats.isEmpty() && value.has("stat")) stats.put(required(value, "stat").toLowerCase(), number(value, "value", 0, -1_000_000, 1_000_000));
            put(result, key, new GemDefinition(key, Map.copyOf(stats), strings(value, "allowed_slots"),
                    string(value, "material", "minecraft:amethyst_shard").toLowerCase()));
        }
        return result;
    }

    private static Map<String, SetDefinition> parseSets(JsonObject root) {
        Map<String, SetDefinition> result = new LinkedHashMap<>();
        for (JsonObject value : objects(root, "sets")) {
            String key = key(value);
            Map<Integer, Map<String, Double>> bonuses = new LinkedHashMap<>();
            for (JsonObject bonus : objects(value, "bonuses")) {
                int pieces = integer(bonus, "pieces", -1, 1, 16);
                Map<String, Double> stats = numberMap(bonus, "stats");
                if (bonuses.putIfAbsent(pieces, stats) != null) fail("套裝件數效果重複：" + key);
            }
            put(result, key, new SetDefinition(key, string(value, "name", key), Map.copyOf(bonuses)));
        }
        return result;
    }

    private static Map<String, ClassDefinition> parseClasses(JsonObject root) {
        Map<String, ClassDefinition> result = new LinkedHashMap<>();
        for (JsonObject value : objects(root, "definitions")) {
            String key = key(value);
            put(result, key, new ClassDefinition(key, string(value, "name", key),
                    number(value, "base_mana", 100, 0, 1_000_000), strings(value, "skills"), numberMap(value, "base_stats")));
        }
        return result;
    }

    private static Map<String, SkillDefinition> parseSkills(JsonObject root) {
        Map<String, SkillDefinition> result = new LinkedHashMap<>();
        for (JsonObject value : objects(root, "definitions")) {
            String key = key(value);
            String trigger = string(value, "trigger", "right_click").toLowerCase();
            if (!TRIGGERS.contains(trigger)) fail("未知技能觸發器：" + trigger);
            List<JsonObject> conditions = objects(value, "conditions").stream().map(JsonObject::deepCopy).toList();
            List<JsonObject> effects = objects(value, "effects").stream().map(JsonObject::deepCopy).toList();
            for (JsonObject condition : conditions) {
                String type = required(condition, "type").toLowerCase();
                if (!CONDITIONS.contains(type)) fail("未知技能條件：" + type);
            }
            for (JsonObject effect : effects) {
                validateEffect(effect, 0);
            }
            put(result, key, new SkillDefinition(key, string(value, "name", key), trigger,
                    number(value, "mana_cost", 0, 0, 1_000_000), number(value, "cooldown_seconds", 0, 0, 86_400),
                    number(value, "cast_time_seconds", 0, 0, 60), bool(value, "interruptible", true),
                    integer(value, "repeats", 1, 1, 100), number(value, "repeat_interval_seconds", 0, 0, 60),
                    conditions, effects));
        }
        return result;
    }

    private static void validateEffect(JsonObject effect, int depth) {
        if (depth > 8) fail("技能效果嵌套過深");
        String type = required(effect, "type").toLowerCase();
        if (!EFFECTS.contains(type)) fail("未知技能效果：" + type);
        if (!effect.has("effects")) return;
        if (!effect.get("effects").isJsonArray()) fail("effects 必須是陣列");
        for (JsonElement nested : effect.getAsJsonArray("effects")) {
            if (!nested.isJsonObject()) fail("嵌套技能效果必須是物件");
            validateEffect(nested.getAsJsonObject(), depth + 1);
        }
    }

    private static Map<String, TreeDefinition> parseTrees(JsonObject root) {
        Map<String, TreeDefinition> result = new LinkedHashMap<>();
        for (JsonObject value : objects(root, "definitions")) {
            String key = key(value);
            String classId = optionalKey(value, "class_id");
            List<TreeNode> nodes = new java.util.ArrayList<>();
            if (value.has("nodes") && value.get("nodes").isJsonArray()) {
                int fallbackSlot = 0;
                for (JsonElement element : value.getAsJsonArray("nodes")) {
                    if (element.isJsonPrimitive()) {
                        String skill = checkedKey(element.getAsString());
                        nodes.add(new TreeNode(skill, skill, 1, 1, List.of(), fallbackSlot++));
                        continue;
                    }
                    if (!element.isJsonObject()) fail("技能樹節點格式錯誤：" + key);
                    JsonObject node = element.getAsJsonObject();
                    String nodeKey = key(node);
                    String skill = checkedKey(required(node, "skill"));
                    int slot = node.has("slot") ? integer(node, "slot", fallbackSlot, 0, 53) : fallbackSlot;
                    nodes.add(new TreeNode(nodeKey, skill, integer(node, "cost", 1, 1, 99),
                            integer(node, "required_level", 1, 1, 1000), strings(node, "requires"), slot));
                    fallbackSlot++;
                }
            }
            Set<String> nodeKeys = nodes.stream().map(TreeNode::key).collect(java.util.stream.Collectors.toSet());
            for (TreeNode node : nodes) for (String required : node.requires()) {
                if (!nodeKeys.contains(required)) fail("技能樹引用不存在的前置節點：" + required);
            }
            put(result, key, new TreeDefinition(key, classId, List.copyOf(nodes)));
        }
        return result;
    }

    private static Map<String, CraftingStationDefinition> parseStations(JsonObject root) {
        Map<String, CraftingStationDefinition> result = new LinkedHashMap<>();
        for (JsonObject value : objects(root, "definitions")) {
            String key = key(value);
            List<RecipeDefinition> recipes = new java.util.ArrayList<>();
            for (JsonObject recipe : objects(value, "recipes")) {
                String recipeKey = key(recipe);
                List<IngredientDefinition> ingredients = new java.util.ArrayList<>();
                for (JsonObject ingredient : objects(recipe, "ingredients")) {
                    String ingredientKey = required(ingredient, "key").toLowerCase();
                    if (!KEY.matcher(ingredientKey).matches() && !ingredientKey.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                        fail("製作材料代碼格式錯誤：" + ingredientKey);
                    }
                    ingredients.add(new IngredientDefinition(ingredientKey,
                            integer(ingredient, "amount", 1, 1, 999), bool(ingredient, "content", !ingredientKey.contains(":"))));
                }
                if (ingredients.isEmpty()) continue;
                recipes.add(new RecipeDefinition(recipeKey, required(recipe, "output").toLowerCase(),
                        integer(recipe, "amount", 1, 1, 99), integer(recipe, "required_level", 1, 1, 1000),
                        string(recipe, "permission", ""), List.copyOf(ingredients)));
            }
            put(result, key, new CraftingStationDefinition(key, string(value, "name", key), List.copyOf(recipes)));
        }
        return result;
    }

    private static Map<String, DropTableDefinition> parseDropTables(JsonObject root) {
        Map<String, DropTableDefinition> result = new LinkedHashMap<>();
        for (JsonObject value : objects(root, "definitions")) {
            String key = key(value);
            double totalWeight = 0;
            List<DropEntry> entries = new java.util.ArrayList<>();
            for (JsonObject entry : objects(value, "entries")) {
                String content = required(entry, "key").toLowerCase();
                double weight = number(entry, "weight", 1, 0.0001, 1_000_000);
                int min = integer(entry, "min", 1, 0, 999);
                int max = integer(entry, "max", min, min, 999);
                totalWeight += weight;
                entries.add(new DropEntry(content, weight, min, max, bool(entry, "content", !content.contains(":"))));
            }
            put(result, key, new DropTableDefinition(key, integer(value, "rolls", 1, 1, 100),
                    strings(value, "sources"), List.copyOf(entries), totalWeight));
        }
        return result;
    }

    private static List<JsonObject> objects(JsonObject root, String name) {
        if (root == null || !root.has(name)) return List.of();
        JsonElement element = root.get(name);
        if (!element.isJsonArray()) fail(name + " 必須是陣列");
        JsonArray array = element.getAsJsonArray();
        return array.asList().stream().map(value -> {
            if (!value.isJsonObject()) fail(name + " 內容必須是物件");
            return value.getAsJsonObject();
        }).toList();
    }

    private static String key(JsonObject value) { return checkedKey(required(value, "key")); }
    private static String optionalKey(JsonObject value, String name) {
        return value.has(name) && !value.get(name).isJsonNull() ? checkedKey(value.get(name).getAsString()) : null;
    }
    private static String checkedKey(String key) {
        String normalized = key.trim().toLowerCase();
        if (!KEY.matcher(normalized).matches()) fail("代碼格式錯誤：" + key);
        return normalized;
    }
    private static String required(JsonObject value, String name) {
        if (!value.has(name) || !value.get(name).isJsonPrimitive() || value.get(name).getAsString().isBlank()) fail("缺少 " + name);
        return value.get(name).getAsString().trim();
    }
    private static String string(JsonObject value, String name, String fallback) {
        return value.has(name) && value.get(name).isJsonPrimitive() ? value.get(name).getAsString().trim() : fallback;
    }
    private static boolean bool(JsonObject value, String name, boolean fallback) {
        return value.has(name) && value.get(name).isJsonPrimitive() ? value.get(name).getAsBoolean() : fallback;
    }
    private static int integer(JsonObject value, String name, int fallback, int min, int max) {
        int result = value.has(name) ? value.get(name).getAsInt() : fallback;
        if (result < min || result > max) fail(name + " 超出範圍");
        return result;
    }
    private static double number(JsonObject value, String name, double fallback, double min, double max) {
        double result = value.has(name) ? value.get(name).getAsDouble() : fallback;
        if (!Double.isFinite(result) || result < min || result > max) fail(name + " 超出範圍");
        return result;
    }
    private static List<String> strings(JsonObject value, String name) {
        if (!value.has(name)) return List.of();
        if (!value.get(name).isJsonArray()) fail(name + " 必須是陣列");
        return value.getAsJsonArray(name).asList().stream().map(element -> checkedKey(element.getAsString())).toList();
    }
    private static Map<String, Double> numberMap(JsonObject value, String name) {
        if (!value.has(name)) return Map.of();
        if (!value.get(name).isJsonObject()) fail(name + " 必須是物件");
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject(name).entrySet()) {
            result.put(checkedKey(entry.getKey()), entry.getValue().getAsDouble());
        }
        return Map.copyOf(result);
    }
    private static <T> void put(Map<String, T> values, String key, T value) {
        if (values.putIfAbsent(key, value) != null) fail("代碼重複：" + key);
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }

    record ItemTemplate(String key, String rarity, double qualityMin, double qualityMax, int sockets,
                        int maxUpgrade, String setId, List<String> affixPool, boolean requiresIdentification,
                        double salvageValue, String material, String name, Map<String, Double> baseStats) {}
    record AffixDefinition(String key, String stat, double min, double max, double weight, String name) {}
    record GemDefinition(String key, Map<String, Double> stats, List<String> allowedSlots, String material) {}
    record SetDefinition(String key, String name, Map<Integer, Map<String, Double>> bonuses) {}
    record ClassDefinition(String key, String name, double baseMana, List<String> skills, Map<String, Double> baseStats) {}
    record SkillDefinition(String key, String name, String trigger, double manaCost, double cooldownSeconds,
                           double castTimeSeconds, boolean interruptible, int repeats, double repeatIntervalSeconds,
                           List<JsonObject> conditions, List<JsonObject> effects) {}
    record TreeDefinition(String key, String classId, List<TreeNode> nodes) {}
    record TreeNode(String key, String skill, int cost, int requiredLevel, List<String> requires, int slot) {}
    record CraftingStationDefinition(String key, String name, List<RecipeDefinition> recipes) {}
    record RecipeDefinition(String key, String output, int amount, int requiredLevel, String permission,
                            List<IngredientDefinition> ingredients) {}
    record IngredientDefinition(String key, int amount, boolean content) {}
    record DropTableDefinition(String key, int rolls, List<String> sources, List<DropEntry> entries, double totalWeight) {}
    record DropEntry(String key, double weight, int min, int max, boolean content) {}
}
