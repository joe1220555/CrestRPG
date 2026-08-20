package tw.crestnetwork.rpg.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class RpgDraftValidator {
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$");

    private RpgDraftValidator() {}

    public static final class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(List<String> errors) {
            this.errors = List.copyOf(errors);
            this.valid = errors.isEmpty();
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public static ValidationResult validate(RpgDraftManager draftManager) {
        List<String> errors = new ArrayList<>();
        Map<String, Map<String, JsonObject>> allDrafts = draftManager.getAllDrafts();

        Map<String, JsonObject> items = allDrafts.getOrDefault("items", Map.of());
        Map<String, JsonObject> weapons = allDrafts.getOrDefault("weapons", Map.of());
        Map<String, JsonObject> equipments = allDrafts.getOrDefault("equipments", Map.of());
        Map<String, JsonObject> gems = allDrafts.getOrDefault("gems", Map.of());
        Map<String, JsonObject> affixes = allDrafts.getOrDefault("affixes", Map.of());
        Map<String, JsonObject> sets = allDrafts.getOrDefault("sets", Map.of());
        Map<String, JsonObject> classes = allDrafts.getOrDefault("classes", Map.of());
        Map<String, JsonObject> skills = allDrafts.getOrDefault("skills", Map.of());
        Map<String, JsonObject> skillTrees = allDrafts.getOrDefault("skill-trees", Map.of());
        Map<String, JsonObject> monsters = allDrafts.getOrDefault("monsters", Map.of());
        Map<String, JsonObject> dropTables = allDrafts.getOrDefault("drop-tables", Map.of());
        Map<String, JsonObject> craftingStations = allDrafts.getOrDefault("crafting-stations", Map.of());

        Set<String> allItemKeys = new HashSet<>();
        allItemKeys.addAll(items.keySet());
        allItemKeys.addAll(weapons.keySet());
        allItemKeys.addAll(equipments.keySet());

        // Validate Keys & Basic values
        validateDraftGroup("items", items, errors, false);
        validateDraftGroup("weapons", weapons, errors, true);
        validateDraftGroup("equipments", equipments, errors, true);
        validateDraftGroup("gems", gems, errors, false);
        validateDraftGroup("affixes", affixes, errors, false);
        validateDraftGroup("sets", sets, errors, false);
        validateDraftGroup("classes", classes, errors, false);
        validateDraftGroup("skills", skills, errors, false);
        validateDraftGroup("skill-trees", skillTrees, errors, false);
        validateDraftGroup("monsters", monsters, errors, false);
        validateDraftGroup("drop-tables", dropTables, errors, false);
        validateDraftGroup("crafting-stations", craftingStations, errors, false);

        // Cross-references validation: Items & Weapons & Equipments -> Affix & Set references
        for (JsonObject item : items.values()) {
            validateItemReferences(item, affixes, sets, errors);
        }

        // Classes -> Skills references
        for (JsonObject classObj : classes.values()) {
            if (classObj.has("skills") && classObj.get("skills").isJsonArray()) {
                for (JsonElement elem : classObj.getAsJsonArray("skills")) {
                    String skillKey = elem.getAsString().toLowerCase();
                    if (!skills.containsKey(skillKey)) {
                        errors.add("職業 [" + classObj.get("key").getAsString() + "] 引用不存在的技能：" + skillKey);
                    }
                }
            }
        }

        // Skill Trees -> Class & Skill & prerequisite node references
        for (JsonObject tree : skillTrees.values()) {
            if (tree.has("class_id") && !tree.get("class_id").isJsonNull()) {
                String classId = tree.get("class_id").getAsString().toLowerCase();
                if (!classId.isBlank() && !classes.containsKey(classId)) {
                    errors.add("技能樹 [" + tree.get("key").getAsString() + "] 引用不存在的職業：" + classId);
                }
            }
            if (tree.has("nodes") && tree.get("nodes").isJsonArray()) {
                Set<String> nodeKeys = new HashSet<>();
                JsonArray nodes = tree.getAsJsonArray("nodes");
                for (JsonElement elem : nodes) {
                    if (elem.isJsonObject()) {
                        JsonObject node = elem.getAsJsonObject();
                        if (node.has("key")) nodeKeys.add(node.get("key").getAsString().toLowerCase());
                        if (node.has("skill")) {
                            String skillKey = node.get("skill").getAsString().toLowerCase();
                            if (!skills.containsKey(skillKey)) {
                                errors.add("技能樹 [" + tree.get("key").getAsString() + "] 節點引用不存在的技能：" + skillKey);
                            }
                        }
                    }
                }
                for (JsonElement elem : nodes) {
                    if (elem.isJsonObject()) {
                        JsonObject node = elem.getAsJsonObject();
                        if (node.has("requires") && node.get("requires").isJsonArray()) {
                            for (JsonElement req : node.getAsJsonArray("requires")) {
                                String reqKey = req.getAsString().toLowerCase();
                                if (!nodeKeys.contains(reqKey)) {
                                    errors.add("技能樹 [" + tree.get("key").getAsString() + "] 節點引用不存在的前置節點：" + reqKey);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Drop Tables -> source monsters & referenced items
        for (JsonObject table : dropTables.values()) {
            if (table.has("entries") && table.get("entries").isJsonArray()) {
                for (JsonElement elem : table.getAsJsonArray("entries")) {
                    if (elem.isJsonObject()) {
                        JsonObject entry = elem.getAsJsonObject();
                        if (entry.has("key")) {
                            String targetKey = entry.get("key").getAsString().toLowerCase();
                            if (!allItemKeys.contains(targetKey) && Material.matchMaterial(targetKey) == null) {
                                errors.add("掉落表 [" + table.get("key").getAsString() + "] 引用無效內容或物品：" + targetKey);
                            }
                        }
                    }
                }
            }
        }

        // Crafting Stations -> Recipes ingredients & outputs
        for (JsonObject station : craftingStations.values()) {
            if (station.has("recipes") && station.get("recipes").isJsonArray()) {
                for (JsonElement elem : station.getAsJsonArray("recipes")) {
                    if (elem.isJsonObject()) {
                        JsonObject recipe = elem.getAsJsonObject();
                        if (recipe.has("output")) {
                            String outputKey = recipe.get("output").getAsString().toLowerCase();
                            if (!allItemKeys.contains(outputKey) && Material.matchMaterial(outputKey) == null) {
                                errors.add("製作站 [" + station.get("key").getAsString() + "] 配方產物不存在：" + outputKey);
                            }
                        }
                        if (recipe.has("ingredients") && recipe.get("ingredients").isJsonArray()) {
                            for (JsonElement ingElem : recipe.getAsJsonArray("ingredients")) {
                                if (ingElem.isJsonObject()) {
                                    JsonObject ing = ingElem.getAsJsonObject();
                                    if (ing.has("key")) {
                                        String ingKey = ing.get("key").getAsString().toLowerCase();
                                        if (!allItemKeys.contains(ingKey) && Material.matchMaterial(ingKey) == null) {
                                            errors.add("製作站 [" + station.get("key").getAsString() + "] 配方材料不存在：" + ingKey);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return new ValidationResult(errors);
    }

    private static void validateDraftGroup(String groupName, Map<String, JsonObject> drafts, List<String> errors, boolean requiresBaseItem) {
        for (Map.Entry<String, JsonObject> entry : drafts.entrySet()) {
            String key = entry.getKey();
            JsonObject obj = entry.getValue();

            if (!KEY_PATTERN.matcher(key).matches()) {
                errors.add("[" + groupName + "] 代碼格式不符：" + key + "（必須為 1-64 位小寫英數、底線或連字號）");
            }

            if (requiresBaseItem) {
                if (!obj.has("base_item") || obj.get("base_item").isJsonNull()) {
                    errors.add("[" + groupName + "] 缺少基礎物品 (base_item)：" + key);
                } else {
                    String baseItem = obj.get("base_item").getAsString();
                    if (Material.matchMaterial(baseItem) == null) {
                        errors.add("[" + groupName + " " + key + "] 無效的 Minecraft Material：" + baseItem);
                    }
                }
            }
        }
    }

    private static void validateItemReferences(JsonObject item, Map<String, JsonObject> affixes, Map<String, JsonObject> sets, List<String> errors) {
        String key = item.has("key") ? item.get("key").getAsString() : "unknown";
        if (item.has("set_id") && !item.get("set_id").isJsonNull()) {
            String setId = item.get("set_id").getAsString().toLowerCase();
            if (!setId.isBlank() && !sets.containsKey(setId)) {
                errors.add("物品 [" + key + "] 引用不存在的套裝：" + setId);
            }
        }
        if (item.has("affix_pool") && item.get("affix_pool").isJsonArray()) {
            for (JsonElement elem : item.getAsJsonArray("affix_pool")) {
                String affixKey = elem.getAsString().toLowerCase();
                if (!affixes.containsKey(affixKey)) {
                    errors.add("物品 [" + key + "] 引用不存在的詞綴：" + affixKey);
                }
            }
        }
    }
}
