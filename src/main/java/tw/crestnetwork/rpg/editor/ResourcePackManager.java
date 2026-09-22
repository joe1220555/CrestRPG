package tw.crestnetwork.rpg.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Validated asset storage and deterministic Minecraft resource-pack builder. */
public final class ResourcePackManager {
    public static final int MAX_ASSET_BYTES = 5 * 1024 * 1024;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Plugin plugin;
    private final File textures;
    private final File models;
    private final File output;

    public ResourcePackManager(Plugin plugin) {
        this.plugin = plugin;
        File root = new File(plugin.getDataFolder(), "web-assets");
        textures = new File(root, "textures/item");
        models = new File(root, "models/item");
        output = new File(root, "generated/CrestRPG-ResourcePack.zip");
        textures.mkdirs();
        models.mkdirs();
        output.getParentFile().mkdirs();
    }

    public synchronized List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();
        addFiles(result, textures, "texture", ".png");
        addFiles(result, models, "model", ".json");
        result.sort(Comparator.comparing(value -> value.get("type") + ":" + value.get("name")));
        return result;
    }

    public synchronized Map<String, Object> save(String type, String requestedName, byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > MAX_ASSET_BYTES) throw new IllegalArgumentException("素材必須介於 1 byte 至 5 MB");
        String name = safeName(requestedName);
        File folder;
        String extension;
        if (type.equals("texture")) {
            validatePng(bytes);
            folder = textures;
            extension = ".png";
        } else if (type.equals("model")) {
            validateModel(bytes);
            folder = models;
            extension = ".json";
        } else throw new IllegalArgumentException("只支援 texture 或 model 素材");
        String base = stripExtension(name).toLowerCase(Locale.ROOT);
        File target = new File(folder, base + extension);
        Files.write(target.toPath(), bytes);
        return describe(target, type, extension);
    }

    public synchronized boolean delete(String type, String requestedName) throws IOException {
        String base = stripExtension(safeName(requestedName)).toLowerCase(Locale.ROOT);
        File target = type.equals("texture") ? new File(textures, base + ".png")
                : type.equals("model") ? new File(models, base + ".json") : null;
        return target != null && Files.deleteIfExists(target.toPath());
    }

    public synchronized BuildResult build(Map<String, Map<String, JsonObject>> drafts) throws IOException {
        int packFormat = Math.max(1, plugin.getConfig().getInt("resource-pack.pack-format", 55));
        String description = plugin.getConfig().getString("resource-pack.description", "CrestRPG generated resources");
        output.getParentFile().mkdirs();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output.toPath()), StandardCharsets.UTF_8)) {
            JsonObject pack = new JsonObject();
            JsonObject meta = new JsonObject();
            meta.addProperty("pack_format", packFormat);
            meta.addProperty("description", description);
            pack.add("pack", meta);
            add(zip, "pack.mcmeta", GSON.toJson(pack).getBytes(StandardCharsets.UTF_8));
            addFolder(zip, textures, "assets/crestrpg/textures/item/", ".png");
            addFolder(zip, models, "assets/crestrpg/models/item/", ".json");
            addGeneratedModels(zip, drafts);
            addVanillaOverrides(zip, drafts);
        }
        writeOraxenConfig(drafts);
        byte[] bytes = Files.readAllBytes(output.toPath());
        String sha1;
        try {
            sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JVM 不支援 SHA-1", impossible);
        }
        return new BuildResult(output, sha1, bytes.length);
    }

    public synchronized File getOutput() { return output.isFile() ? output : null; }

    private void addGeneratedModels(ZipOutputStream zip, Map<String, Map<String, JsonObject>> drafts) throws IOException {
        for (String kind : List.of("items", "weapons", "equipments", "gems")) {
            for (Map.Entry<String, JsonObject> entry : drafts.getOrDefault(kind, Map.of()).entrySet()) {
                JsonObject draft = entry.getValue();
                if (!draft.has("texture") || draft.get("texture").getAsString().isBlank()) continue;
                String texture = stripExtension(safeName(draft.get("texture").getAsString())).toLowerCase(Locale.ROOT);
                if ((draft.has("model") && !draft.get("model").getAsString().isBlank()) || new File(models, entry.getKey() + ".json").isFile()) continue;
                JsonObject model = new JsonObject();
                model.addProperty("parent", "minecraft:item/generated");
                JsonObject layers = new JsonObject();
                layers.addProperty("layer0", "crestrpg:item/" + texture);
                model.add("textures", layers);
                add(zip, "assets/crestrpg/models/item/" + entry.getKey() + ".json", GSON.toJson(model).getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void addVanillaOverrides(ZipOutputStream zip, Map<String, Map<String, JsonObject>> drafts) throws IOException {
        Map<String, JsonArray> overrides = new LinkedHashMap<>();
        for (String kind : List.of("items", "weapons", "equipments", "gems")) {
            for (Map.Entry<String, JsonObject> entry : drafts.getOrDefault(kind, Map.of()).entrySet()) {
                JsonObject draft = entry.getValue();
                if (!draft.has("texture") || !draft.has("custom_model_data") || (!draft.has("base_item") && !draft.has("material"))) continue;
                String material = (draft.has("base_item") ? draft.get("base_item") : draft.get("material")).getAsString().replace("minecraft:", "").toLowerCase(Locale.ROOT);
                JsonObject override = new JsonObject();
                JsonObject predicate = new JsonObject();
                predicate.addProperty("custom_model_data", draft.get("custom_model_data").getAsInt());
                override.add("predicate", predicate);
                String modelName = draft.has("model") && !draft.get("model").getAsString().isBlank()
                        ? stripExtension(safeName(draft.get("model").getAsString())).toLowerCase(Locale.ROOT) : entry.getKey();
                override.addProperty("model", "crestrpg:item/" + modelName);
                overrides.computeIfAbsent(material, ignored -> new JsonArray()).add(override);
            }
        }
        for (Map.Entry<String, JsonArray> entry : overrides.entrySet()) {
            JsonObject base = new JsonObject();
            String parent = isHandheld(entry.getKey()) ? "minecraft:item/handheld" : "minecraft:item/generated";
            base.addProperty("parent", parent);
            JsonObject texturesObject = new JsonObject();
            texturesObject.addProperty("layer0", "minecraft:item/" + entry.getKey());
            base.add("textures", texturesObject);
            base.add("overrides", entry.getValue());
            add(zip, "assets/minecraft/models/item/" + entry.getKey() + ".json", GSON.toJson(base).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static boolean isHandheld(String material) {
        return material.endsWith("_sword") || material.endsWith("_axe") || material.endsWith("_pickaxe")
                || material.endsWith("_shovel") || material.endsWith("_hoe") || material.equals("stick") || material.equals("blaze_rod");
    }

    private void writeOraxenConfig(Map<String, Map<String, JsonObject>> drafts) throws IOException {
        File target = new File(plugin.getDataFolder(), "generated/oraxen/items/crestrpg.yml");
        target.getParentFile().mkdirs();
        StringBuilder yaml = new StringBuilder("# Generated by CrestRPG. Copy into plugins/Oraxen/items/ if desired.\n");
        for (String kind : List.of("items", "weapons", "equipments", "gems")) {
            for (Map.Entry<String, JsonObject> entry : drafts.getOrDefault(kind, Map.of()).entrySet()) {
                JsonObject draft = entry.getValue();
                if (!draft.has("texture") || draft.get("texture").getAsString().isBlank()) continue;
                String material = draft.has("base_item") ? draft.get("base_item").getAsString().replace("minecraft:", "").toUpperCase(Locale.ROOT) : "PAPER";
                String name = draft.has("name") ? draft.get("name").getAsString() : entry.getKey();
                String texture = stripExtension(safeName(draft.get("texture").getAsString())).toLowerCase(Locale.ROOT);
                yaml.append(entry.getKey()).append(":\n  displayname: '").append(yamlEscape(name)).append("'\n  material: ").append(material)
                        .append("\n  Pack:\n    generate_model: true\n    textures:\n      - crestrpg:item/").append(texture).append("\n");
            }
        }
        Files.writeString(target.toPath(), yaml.toString(), StandardCharsets.UTF_8);
    }

    private static void addFolder(ZipOutputStream zip, File folder, String prefix, String extension) throws IOException {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(extension));
        if (files == null) return;
        java.util.Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) add(zip, prefix + file.getName(), Files.readAllBytes(file.toPath()));
    }

    private static void add(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private void addFiles(List<Map<String, Object>> result, File folder, String type, String extension) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(extension));
        if (files == null) return;
        for (File file : files) result.add(describe(file, type, extension));
    }

    private static Map<String, Object> describe(File file, String type, String extension) {
        return Map.of("type", type, "name", stripExtension(file.getName()), "filename", file.getName(), "size", file.length(),
                "reference", type.equals("texture") ? "crestrpg:item/" + stripExtension(file.getName()) : "crestrpg:item/" + stripExtension(file.getName()));
    }

    private static String safeName(String raw) {
        String name = new File(raw == null ? "" : raw).getName();
        if (!name.matches("[A-Za-z0-9_.-]{1,128}") || name.contains("..")) throw new IllegalArgumentException("檔名只能包含英數、底線、點與連字號");
        return name;
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static void validatePng(byte[] bytes) {
        byte[] signature = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length < signature.length) throw new IllegalArgumentException("這不是有效的 PNG 檔案");
        for (int i = 0; i < signature.length; i++) if (bytes[i] != signature[i]) throw new IllegalArgumentException("這不是有效的 PNG 檔案");
    }

    private static void validateModel(byte[] bytes) {
        try {
            var value = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            if (!value.isJsonObject()) throw new IllegalArgumentException();
        } catch (RuntimeException exception) { throw new IllegalArgumentException("模型必須是有效的 JSON 物件"); }
    }

    private static String yamlEscape(String value) { return value.replace("'", "''").replace("\n", " ").replace("\r", " "); }
    public record BuildResult(File file, String sha1, long size) {}
}
