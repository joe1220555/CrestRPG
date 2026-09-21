package tw.crestnetwork.rpg.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;
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
    }

    public synchronized void loadAllDrafts() {
        draftsByKind.clear();
        String[] kinds = {
                "items", "weapons", "equipments", "gems", "affixes", "rarities", "sets",
                "classes", "skills", "skill-trees", "monsters", "drop-tables", "crafting-stations",
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

    private void report(String message, Exception exception) {
        if (plugin != null) {
            if (exception == null) plugin.getLogger().severe(message);
            else plugin.getLogger().log(java.util.logging.Level.SEVERE, message, exception);
        } else if (exception != null) {
            System.err.println(message + "：" + exception.getMessage());
        }
    }
}
