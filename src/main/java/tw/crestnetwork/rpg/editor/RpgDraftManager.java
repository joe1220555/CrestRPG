package tw.crestnetwork.rpg.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RpgDraftManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final File draftsFolder;
    private final Plugin plugin;
    private final Map<String, Map<String, JsonObject>> draftsByKind = new ConcurrentHashMap<>();

    public RpgDraftManager(Plugin plugin) {
        this.plugin = plugin;
        this.draftsFolder = new File(plugin.getDataFolder(), "editor-drafts");
        initialize();
    }

    public RpgDraftManager(File draftsFolder) {
        this.plugin = null;
        this.draftsFolder = draftsFolder;
        initialize();
    }

    private void initialize() {
        if (!draftsFolder.exists()) {
            if (!draftsFolder.mkdirs() && !draftsFolder.isDirectory()) {
                report("無法建立 RPG 草稿資料夾：" + draftsFolder, null);
            }
        }
        loadAllDrafts();
        if (plugin != null) importBundledDefaultsOnce();
    }

    public synchronized void loadAllDrafts() {
        draftsByKind.clear();
        String[] kinds = {
                "items", "weapons", "equipments", "gems", "affixes", "rarities", "sets",
                "classes", "skills", "skill-trees", "quests", "monsters", "drop-tables", "crafting-stations",
                "npcs"
        };
        for (String kind : kinds) {
            Map<String, JsonObject> map = new LinkedHashMap<>();
            File file = new File(draftsFolder, kind + ".json");
            if (file.exists() && file.isFile()) {
                try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed != null && parsed.isJsonArray()) {
                        JsonArray array = parsed.getAsJsonArray();
                        for (JsonElement elem : array) {
                            if (elem.isJsonObject()) {
                                JsonObject obj = elem.getAsJsonObject();
                                if (obj.has("key") && obj.get("key").isJsonPrimitive()) {
                                    String key = obj.get("key").getAsString().toLowerCase();
                                    map.put(key, obj);
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                    report("讀取 RPG 草稿失敗，保留原檔案：" + file.getName(), exception);
                }
            }
            draftsByKind.put(kind, map);
        }
    }

    public synchronized Map<String, JsonObject> getDrafts(String kind) {
        return new LinkedHashMap<>(draftsByKind.getOrDefault(kind, Map.of()));
    }

    public synchronized JsonObject getDraft(String kind, String key) {
        Map<String, JsonObject> map = draftsByKind.get(kind);
        return map == null ? null : map.get(key.toLowerCase());
    }

    public synchronized void saveDraft(String kind, String key, JsonObject payload) {
        Map<String, JsonObject> map = draftsByKind.computeIfAbsent(kind, k -> new LinkedHashMap<>());
        payload.addProperty("key", key.toLowerCase());
        map.put(key.toLowerCase(), payload.deepCopy());
        persistKind(kind);
    }

    public synchronized boolean deleteDraft(String kind, String key) {
        Map<String, JsonObject> map = draftsByKind.get(kind);
        if (map != null && map.remove(key.toLowerCase()) != null) {
            persistKind(kind);
            return true;
        }
        return false;
    }

    private void persistKind(String kind) {
        Map<String, JsonObject> map = draftsByKind.get(kind);
        if (map == null) return;
        JsonArray array = new JsonArray();
        for (JsonObject obj : map.values()) {
            array.add(obj);
        }
        File file = new File(draftsFolder, kind + ".json");
        File temporary = new File(draftsFolder, kind + ".json.tmp");
        try (FileWriter writer = new FileWriter(temporary, StandardCharsets.UTF_8)) {
            GSON.toJson(array, writer);
            writer.flush();
        } catch (IOException exception) {
            report("寫入 RPG 草稿暫存檔失敗：" + kind, exception);
            return;
        }
        try {
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            report("發布 RPG 草稿檔案失敗：" + kind, exception);
        }
    }

    public synchronized Map<String, Map<String, JsonObject>> getAllDrafts() {
        Map<String, Map<String, JsonObject>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, JsonObject>> entry : draftsByKind.entrySet()) {
            result.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return result;
    }

    private synchronized void importBundledDefaultsOnce() {
        File marker = new File(draftsFolder, ".defaults-imported-v1");
        if (marker.isFile()) return;
        int importedItems = importLegacyItems();
        int importedQuests = importLegacyQuests();
        seedRarities();
        try {
            Files.writeString(marker.toPath(), "Imported legacy items and quests into web editor drafts.\n", StandardCharsets.UTF_8);
        } catch (IOException exception) {
            report("無法寫入預設草稿匯入標記", exception);
        }
        if (importedItems > 0 || importedQuests > 0) {
            plugin.getLogger().info("已將 " + importedItems + " 個現有物品與 " + importedQuests + " 個任務匯入內建網頁編輯器。");
        }
    }

    private int importLegacyItems() {
        File file = new File(plugin.getDataFolder(), "items.yml");
        ConfigurationSection root = YamlConfiguration.loadConfiguration(file).getConfigurationSection("items");
        if (root == null) return 0;
        int imported = 0;
        for (String key : root.getKeys(false)) {
            ConfigurationSection source = root.getConfigurationSection(key);
            if (source == null) continue;
            String material = source.getString("material", "PAPER").toUpperCase();
            String kind = itemKind(material);
            if (getDraft(kind, key) != null) continue;
            JsonObject draft = new JsonObject();
            draft.addProperty("name", source.getString("display-name", key));
            draft.addProperty("base_item", "minecraft:" + material.toLowerCase());
            if (source.contains("custom-model-data")) draft.addProperty("custom_model_data", source.getInt("custom-model-data"));
            draft.add("lore", stringArray(source.getStringList("lore")));
            draft.addProperty("rarity", "common");
            draft.addProperty("level", highestNumber(source.getConfigurationSection("requirements"), 1));
            draft.add("stats", sectionObject(source.getConfigurationSection("stats")));
            draft.add("requirements", sectionObject(source.getConfigurationSection("requirements")));
            if (kind.equals("equipments")) draft.addProperty("equipment_slot", equipmentSlot(material));
            draft.addProperty("enabled", true);
            saveDraft(kind, key, draft);
            imported++;
        }
        return imported;
    }

    private int importLegacyQuests() {
        File file = new File(plugin.getDataFolder(), "quests.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int imported = 0;
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection source = yaml.getConfigurationSection(key);
            if (source == null || getDraft("quests", key.toLowerCase()) != null) continue;
            JsonObject draft = new JsonObject();
            draft.addProperty("name", source.getString("Title", key));
            draft.addProperty("type", source.getString("Type", "MAIN").toLowerCase());
            draft.add("description", stringArray(source.getStringList("Description")));
            draft.addProperty("npc", source.getString("NPC", ""));
            draft.addProperty("prerequisite", source.getString("Prerequisite", ""));
            draft.add("objectives", valueArray(source.getList("Objectives", List.of())));
            draft.add("rewards", sectionObject(source.getConfigurationSection("Rewards")));
            draft.addProperty("enabled", true);
            saveDraft("quests", key.toLowerCase(), draft);
            imported++;
        }
        return imported;
    }

    private void seedRarities() {
        if (!getDrafts("rarities").isEmpty()) return;
        saveRarity("common", "普通", "<white>", 100);
        saveRarity("rare", "稀有", "<aqua>", 35);
        saveRarity("epic", "史詩", "<light_purple>", 10);
        saveRarity("legendary", "傳說", "<gold>", 2);
    }

    private void saveRarity(String key, String name, String color, int weight) {
        JsonObject rarity = new JsonObject();
        rarity.addProperty("name", name);
        rarity.addProperty("color", color);
        rarity.addProperty("weight", weight);
        rarity.addProperty("enabled", true);
        saveDraft("rarities", key, rarity);
    }

    private static String itemKind(String material) {
        if (material.endsWith("_HELMET") || material.endsWith("_CHESTPLATE") || material.endsWith("_LEGGINGS") || material.endsWith("_BOOTS")) return "equipments";
        if (material.endsWith("_SWORD") || material.endsWith("_AXE") || material.equals("BOW") || material.equals("CROSSBOW") || material.equals("TRIDENT") || material.equals("BLAZE_ROD")) return "weapons";
        return "items";
    }

    private static String equipmentSlot(String material) {
        if (material.endsWith("_HELMET")) return "head";
        if (material.endsWith("_LEGGINGS")) return "legs";
        if (material.endsWith("_BOOTS")) return "feet";
        return "chest";
    }

    private static int highestNumber(ConfigurationSection section, int fallback) {
        if (section == null) return fallback;
        return section.getKeys(false).stream().mapToInt(section::getInt).max().orElse(fallback);
    }

    private static JsonArray stringArray(List<String> values) {
        JsonArray result = new JsonArray();
        values.forEach(result::add);
        return result;
    }

    private static JsonArray valueArray(List<?> values) {
        JsonArray result = new JsonArray();
        values.forEach(value -> result.add(safeJsonValue(value)));
        return result;
    }

    static JsonObject sectionObject(ConfigurationSection section) {
        if (section == null) return new JsonObject();
        JsonObject result = new JsonObject();
        for (String key : section.getKeys(false)) result.add(key, safeJsonValue(section.get(key)));
        return result;
    }

    static JsonElement safeJsonValue(Object value) {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof ConfigurationSection section) return sectionObject(section);
        if (value instanceof Map<?, ?> map) {
            JsonObject result = new JsonObject();
            map.forEach((key, item) -> result.add(String.valueOf(key), safeJsonValue(item)));
            return result;
        }
        if (value instanceof Iterable<?> iterable) {
            JsonArray result = new JsonArray();
            iterable.forEach(item -> result.add(safeJsonValue(item)));
            return result;
        }
        if (value instanceof Boolean bool) return new JsonPrimitive(bool);
        if (value instanceof Number number) return new JsonPrimitive(number);
        if (value instanceof Character character) return new JsonPrimitive(character);
        if (value instanceof String string) return new JsonPrimitive(string);
        return new JsonPrimitive(String.valueOf(value));
    }

    private void report(String message, Exception exception) {
        if (plugin != null) {
            if (exception == null) plugin.getLogger().severe(message);
            else plugin.getLogger().log(java.util.logging.Level.SEVERE, message, exception);
        } else if (exception != null) {
            System.err.println(message + "：" + exception.getMessage());
        }
    }
}
