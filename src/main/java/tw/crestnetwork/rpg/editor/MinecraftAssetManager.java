package tw.crestnetwork.rpg.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Downloads the official Mojang asset index and lazily caches vanilla item/block icons. */
public final class MinecraftAssetManager {
    private static final URI VERSION_MANIFEST = URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Plugin plugin;
    private final File root;
    private final File catalogFile;
    private final File iconFolder;
    private final HttpClient client;
    private final Map<String, Icon> icons = new LinkedHashMap<>();

    public MinecraftAssetManager(Plugin plugin) {
        this.plugin = plugin;
        root = new File(plugin.getDataFolder(), "minecraft-assets");
        catalogFile = new File(root, "catalog.json");
        iconFolder = new File(root, "icons");
        iconFolder.mkdirs();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
        loadCatalog();
    }

    public synchronized List<Map<String, String>> list() {
        return icons.values().stream().sorted(Comparator.comparing(Icon::id)).map(icon -> Map.of(
                "id", icon.id(), "name", icon.name(), "category", icon.category(), "type", icon.type()
        )).toList();
    }

    public synchronized SyncResult sync() throws IOException, InterruptedException {
        String version = plugin.getConfig().getString("minecraft-assets.version", "26.2").trim();
        JsonObject manifest = fetchJson(VERSION_MANIFEST);
        String versionUrl = null;
        for (var element : manifest.getAsJsonArray("versions")) {
            JsonObject candidate = element.getAsJsonObject();
            if (candidate.get("id").getAsString().equals(version)) {
                versionUrl = candidate.get("url").getAsString();
                break;
            }
        }
        if (versionUrl == null) throw new IllegalArgumentException("在 Mojang 官方版本清單找不到 " + version + "，請調整 minecraft-assets.version");
        JsonObject versionMeta = fetchJson(URI.create(versionUrl));
        String assetIndexUrl = versionMeta.getAsJsonObject("assetIndex").get("url").getAsString();
        JsonObject objects = fetchJson(URI.create(assetIndexUrl)).getAsJsonObject("objects");
        Map<String, String> translations = fetchTranslations(objects);
        Map<String, Icon> imported = new LinkedHashMap<>();
        importCategory(objects, imported, translations, "minecraft/textures/item/", "item");
        importCategory(objects, imported, translations, "minecraft/textures/block/", "block");
        if (imported.isEmpty()) {
            JsonObject clientDownload = versionMeta.getAsJsonObject("downloads").getAsJsonObject("client");
            importClientResources(URI.create(clientDownload.get("url").getAsString()), imported, translations);
        }
        if (imported.isEmpty()) throw new IOException("Minecraft " + version + " 中找不到可用的物品或方塊圖示");
        icons.clear();
        icons.putAll(imported);
        saveCatalog(version);
        return new SyncResult(version, icons.size());
    }

    public synchronized File icon(String materialId) throws IOException, InterruptedException {
        String id = normalizeId(materialId);
        Icon icon = icons.get(id);
        if (icon == null) throw new IllegalArgumentException("找不到 Minecraft 材質圖示：" + id);
        File target = new File(iconFolder, id.substring("minecraft:".length()) + ".png");
        if (!target.isFile()) {
            if (icon.hash().isBlank()) throw new IOException("圖示快取不完整，請重新執行 Mojang 同步");
            URI url = URI.create("https://resources.download.minecraft.net/" + icon.hash().substring(0, 2) + "/" + icon.hash());
            HttpRequest request = HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new IOException("Mojang 圖示下載失敗：HTTP " + response.statusCode());
            byte[] bytes = response.body();
            if (bytes.length < 8 || bytes[0] != (byte) 137 || bytes[1] != 80 || bytes[2] != 78 || bytes[3] != 71) {
                throw new IOException("Mojang 返回的圖示不是有效 PNG");
            }
            Files.write(target.toPath(), bytes);
        }
        return target;
    }

    public synchronized boolean isReady() { return !icons.isEmpty(); }

    private JsonObject fetchJson(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).header("User-Agent", "CrestRPG/2.4").GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) throw new IOException("Mojang API 請求失敗：HTTP " + response.statusCode());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private Map<String, String> fetchTranslations(JsonObject objects) throws IOException, InterruptedException {
        Map<String, String> translations = new LinkedHashMap<>();
        var language = objects.get("minecraft/lang/zh_tw.json");
        if (language == null) return translations;
        String hash = language.getAsJsonObject().get("hash").getAsString();
        URI url = URI.create("https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash);
        JsonObject values = fetchJson(url);
        for (Map.Entry<String, com.google.gson.JsonElement> entry : values.entrySet()) {
            if ((entry.getKey().startsWith("item.minecraft.") || entry.getKey().startsWith("block.minecraft.")) && entry.getValue().isJsonPrimitive()) {
                translations.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return translations;
    }

    private static void importCategory(JsonObject objects, Map<String, Icon> result, Map<String, String> translations, String prefix, String type) {
        for (Map.Entry<String, com.google.gson.JsonElement> entry : objects.entrySet()) {
            String path = entry.getKey();
            if (!path.startsWith(prefix) || !path.endsWith(".png")) continue;
            String name = path.substring(prefix.length(), path.length() - 4);
            if (name.contains("/") || name.endsWith("_overlay")) continue;
            String id = "minecraft:" + name.toLowerCase(Locale.ROOT);
            String translated = translations.getOrDefault(type + ".minecraft." + name, name);
            Icon icon = new Icon(id, translated, classify(name, type), type, entry.getValue().getAsJsonObject().get("hash").getAsString());
            if (type.equals("item")) result.put(id, icon); else result.putIfAbsent(id, icon);
        }
    }

    private void importClientResources(URI clientUrl, Map<String, Icon> result, Map<String, String> translations) throws IOException, InterruptedException {
        root.mkdirs();
        File archive = File.createTempFile("minecraft-client-", ".jar", root);
        try {
            HttpRequest request = HttpRequest.newBuilder(clientUrl).timeout(Duration.ofMinutes(2)).header("User-Agent", "CrestRPG/2.4").GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) throw new IOException("Minecraft 客戶端資源下載失敗：HTTP " + response.statusCode());
            try (InputStream input = response.body()) {
                Files.copy(input, archive.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive.toPath()))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String path = entry.getName();
                    String type;
                    String prefix;
                    if (path.startsWith("assets/minecraft/textures/item/")) {
                        type = "item"; prefix = "assets/minecraft/textures/item/";
                    } else if (path.startsWith("assets/minecraft/textures/block/")) {
                        type = "block"; prefix = "assets/minecraft/textures/block/";
                    } else continue;
                    if (entry.isDirectory() || !path.endsWith(".png")) continue;
                    String name = path.substring(prefix.length(), path.length() - 4);
                    if (name.contains("/") || name.endsWith("_overlay")) continue;
                    String id = "minecraft:" + name.toLowerCase(Locale.ROOT);
                    if (result.containsKey(id)) continue;
                    String translated = translations.getOrDefault(type + ".minecraft." + name, name);
                    File target = new File(iconFolder, name.toLowerCase(Locale.ROOT) + ".png");
                    Files.copy(zip, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    result.put(id, new Icon(id, translated, classify(name, type), type, ""));
                }
            }
        } finally {
            Files.deleteIfExists(archive.toPath());
        }
    }

    private static String classify(String name, String type) {
        if (type.equals("block")) {
            if (containsAny(name, "glass", "pane")) return "glass";
            if (containsAny(name, "redstone", "repeater", "comparator", "observer", "piston", "lever", "button", "pressure_plate", "rail", "dispenser", "dropper", "hopper", "target")) return "redstone";
            return "block";
        }
        if (containsAny(name, "sword", "bow", "crossbow", "trident", "mace", "spear")) return "weapon";
        if (containsAny(name, "helmet", "chestplate", "leggings", "boots", "shield", "elytra")) return "armor";
        if (containsAny(name, "pickaxe", "axe", "shovel", "hoe", "shears", "fishing_rod", "flint_and_steel", "brush")) return "tool";
        if (containsAny(name, "apple", "bread", "beef", "porkchop", "chicken", "mutton", "rabbit", "cod", "salmon", "potato", "carrot", "melon", "cookie", "cake", "stew", "soup", "berries", "honey_bottle", "dried_kelp")) return "food";
        if (containsAny(name, "potion", "enchanted", "ender", "experience", "totem", "amethyst", "echo_shard", "nether_star", "dragon_breath")) return "magic";
        return "other";
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private void loadCatalog() {
        if (!catalogFile.isFile()) return;
        try {
            JsonObject rootObject = JsonParser.parseString(Files.readString(catalogFile.toPath())).getAsJsonObject();
            if (!rootObject.has("catalogVersion") || rootObject.get("catalogVersion").getAsInt() < 2) {
                plugin.getLogger().info("Minecraft 圖示目錄需要升級，請在網頁圖鑑中執行 Mojang 同步。");
                return;
            }
            for (var element : rootObject.getAsJsonArray("icons")) {
                JsonObject value = element.getAsJsonObject();
                String id = value.get("id").getAsString();
                String type = value.has("type") ? value.get("type").getAsString() : value.get("category").getAsString();
                String category = value.has("type") ? value.get("category").getAsString() : classify(id.substring(id.indexOf(':') + 1), type);
                Icon icon = new Icon(id, value.get("name").getAsString(), category, type, value.get("hash").getAsString());
                icons.put(icon.id(), icon);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("無法讀取 Minecraft 圖示快取，請在網頁後台重新同步。");
        }
    }

    private void saveCatalog(String version) throws IOException {
        root.mkdirs();
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty("catalogVersion", 2);
        rootObject.addProperty("version", version);
        rootObject.add("icons", GSON.toJsonTree(new ArrayList<>(icons.values())));
        Files.writeString(catalogFile.toPath(), GSON.toJson(rootObject), StandardCharsets.UTF_8);
    }

    private static String normalizeId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!value.startsWith("minecraft:")) value = "minecraft:" + value;
        if (!value.matches("minecraft:[a-z0-9_]{1,100}")) throw new IllegalArgumentException("材質 ID 無效");
        return value;
    }

    private record Icon(String id, String name, String category, String type, String hash) {}
    public record SyncResult(String version, int count) {}
}
