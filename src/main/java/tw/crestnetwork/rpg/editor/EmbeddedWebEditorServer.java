package tw.crestnetwork.rpg.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.Plugin;
import tw.crestnetwork.rpg.CrestRpgPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A small, dependency-free admin server backed by the same drafts as the in-game editor. */
public final class EmbeddedWebEditorServer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int MAX_REQUEST_BYTES = 1_048_576;
    private static final Set<String> KINDS = Set.of(
            "items", "weapons", "equipments", "gems", "affixes", "rarities", "sets",
            "classes", "skills", "skill-trees", "quests", "monsters", "drop-tables", "crafting-stations", "npcs", "gui-layouts", "guild-settings"
    );
    private static final Map<String, String> ASSETS = Map.of(
            "/", "web-editor/index.html",
            "/index.html", "web-editor/index.html",
            "/app.js", "web-editor/app.js",
            "/admin.css", "web-editor/admin.css"
    );

    private final Plugin plugin;
    private final RpgDraftManager drafts;
    private final ResourcePackManager resourcePacks;
    private final MinecraftAssetManager minecraftAssets;
    private HttpServer server;
    private ExecutorService executor;
    private volatile String token;
    private String statusMessage = "尚未啟動";

    public EmbeddedWebEditorServer(Plugin plugin, RpgDraftManager drafts) {
        this.plugin = plugin;
        this.drafts = drafts;
        this.resourcePacks = new ResourcePackManager(plugin);
        this.minecraftAssets = new MinecraftAssetManager(plugin);
    }

    public synchronized void start() {
        if (!plugin.getConfig().getBoolean("web-editor.enabled", false)) {
            statusMessage = "已在 config.yml 中關閉";
            return;
        }
        token = plugin.getConfig().getString("web-editor.token", "").trim();
        if (token.length() < 16 || token.equals("CHANGE_ME")) {
            token = generateToken();
            plugin.getConfig().set("web-editor.token", token);
            plugin.saveConfig();
            plugin.getLogger().warning("內建網頁編輯器已自動建立首次登入 Token：" + token);
            plugin.getLogger().warning("請勿將此 Token 分享給他人；登入後可在網頁後台更換。");
        }
        String host = plugin.getConfig().getString("web-editor.bind-address", "127.0.0.1").trim();
        int port = plugin.getConfig().getInt("web-editor.port", 8765);
        if (port < 1 || port > 65535) {
            statusMessage = "連接埠必須介於 1 至 65535";
            plugin.getLogger().severe("內建網頁編輯器未啟動：web-editor.port 必須介於 1 至 65535。");
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 32);
            server.createContext("/", this::handle);
            executor = Executors.newVirtualThreadPerTaskExecutor();
            server.setExecutor(executor);
            server.start();
            statusMessage = "正在執行";
            plugin.getLogger().info("內建網頁編輯器已啟動：http://" + host + ":" + port + "/");
            if (!isLoopback(host)) {
                plugin.getLogger().warning("網頁編輯器已開放於非本機位址；請使用防火牆與 HTTPS 反向代理保護它。");
            }
        } catch (IOException exception) {
            statusMessage = "啟動失敗：" + exception.getMessage();
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "無法啟動內建網頁編輯器", exception);
        }
    }

    public synchronized void stop() {
        if (server != null) server.stop(1);
        if (executor != null) executor.close();
        server = null;
        executor = null;
        statusMessage = "已停止";
    }

    public synchronized boolean isRunning() { return server != null; }
    public synchronized String getStatusMessage() { return statusMessage; }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            addSecurityHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                if (!authorized(exchange)) {
                    sendJson(exchange, 401, Map.of("error", "管理 Token 無效"));
                    return;
                }
                handleApi(exchange, path);
                return;
            }
            serveAsset(exchange, path);
        } catch (RequestException exception) {
            sendJson(exchange, exception.status, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "網頁編輯器請求處理失敗", exception);
            sendJson(exchange, 500, Map.of("error", "伺服器內部錯誤"));
        } finally {
            exchange.close();
        }
    }

    private void handleApi(HttpExchange exchange, String path) throws IOException {
        String method = exchange.getRequestMethod();
        Map<String, String> query = parseQuery(exchange.getRequestURI());
        if (path.equals("/api/session") && method.equals("GET")) {
            sendJson(exchange, 200, Map.of("authenticated", true, "version", plugin.getPluginMeta().getVersion()));
            return;
        }
        if (path.equals("/api/kinds") && method.equals("GET")) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String kind : orderedKinds()) counts.put(kind, drafts.getDrafts(kind).size());
            sendJson(exchange, 200, Map.of("kinds", orderedKinds(), "counts", counts));
            return;
        }
        if (path.equals("/api/assets") && method.equals("GET")) {
            sendJson(exchange, 200, Map.of("assets", resourcePacks.list()));
            return;
        }
        if (path.equals("/api/minecraft-assets") && method.equals("GET")) {
            sendJson(exchange, 200, Map.of("ready", minecraftAssets.isReady(), "icons", minecraftAssets.list()));
            return;
        }
        if (path.equals("/api/minecraft-assets/sync") && method.equals("POST")) {
            try {
                MinecraftAssetManager.SyncResult synced = minecraftAssets.sync();
                sendJson(exchange, 200, Map.of("synced", true, "version", synced.version(), "count", synced.count()));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RequestException(503, "Minecraft 圖示同步已中斷");
            } catch (IOException exception) {
                throw new RequestException(502, "無法從 Mojang 同步：" + exception.getMessage());
            }
            return;
        }
        if (path.equals("/api/minecraft-assets/icon") && method.equals("GET")) {
            try {
                File file = minecraftAssets.icon(query.getOrDefault("id", ""));
                sendFile(exchange, file, "image/png", null);
            } catch (IllegalArgumentException exception) {
                throw new RequestException(404, exception.getMessage());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RequestException(503, "Minecraft 圖示下載已中斷");
            } catch (IOException exception) {
                throw new RequestException(502, "Minecraft 圖示暫時無法使用：" + exception.getMessage());
            }
            return;
        }
        if (path.equals("/api/asset") && method.equals("POST")) {
            String type = query.getOrDefault("type", "");
            String name = query.getOrDefault("name", "");
            sendJson(exchange, 201, resourcePacks.save(type, name, readBytes(exchange, ResourcePackManager.MAX_ASSET_BYTES)));
            return;
        }
        if (path.equals("/api/asset") && method.equals("DELETE")) {
            boolean deleted = resourcePacks.delete(query.getOrDefault("type", ""), query.getOrDefault("name", ""));
            if (!deleted) throw new RequestException(404, "找不到素材");
            sendJson(exchange, 200, Map.of("deleted", true));
            return;
        }
        if (path.equals("/api/asset/content") && method.equals("GET")) {
            sendAsset(exchange, query.getOrDefault("type", ""), query.getOrDefault("name", ""));
            return;
        }
        if (path.equals("/api/resource-pack/build") && method.equals("POST")) {
            assignMissingCustomModelData();
            ResourcePackManager.BuildResult built = resourcePacks.build(drafts.getAllDrafts());
            sendJson(exchange, 200, Map.of("built", true, "filename", built.file().getName(), "sha1", built.sha1(), "size", built.size()));
            return;
        }
        if (path.equals("/api/resource-pack") && method.equals("GET")) {
            File file = resourcePacks.getOutput();
            if (file == null) throw new RequestException(404, "尚未建立資源包");
            sendFile(exchange, file, "application/zip", "CrestRPG-ResourcePack.zip");
            return;
        }
        if (path.equals("/api/drafts") && method.equals("GET")) {
            String kind = requireKind(query);
            sendJson(exchange, 200, Map.of("kind", kind, "drafts", drafts.getDrafts(kind)));
            return;
        }
        if (path.equals("/api/draft")) {
            String kind = requireKind(query);
            String key = requireKey(query);
            if (method.equals("GET")) {
                JsonObject draft = drafts.getDraft(kind, key);
                if (draft == null) throw new RequestException(404, "找不到草稿");
                sendJson(exchange, 200, draft);
                return;
            }
            if (method.equals("PUT")) {
                JsonObject payload = readObject(exchange);
                drafts.saveDraft(kind, key, payload);
                sendJson(exchange, 200, Map.of("saved", true, "kind", kind, "key", key));
                return;
            }
            if (method.equals("DELETE")) {
                if (!drafts.deleteDraft(kind, key)) throw new RequestException(404, "找不到草稿");
                sendJson(exchange, 200, Map.of("deleted", true));
                return;
            }
        }
        if (path.equals("/api/validate") && method.equals("POST")) {
            RpgDraftValidator.ValidationResult result = RpgDraftValidator.validate(drafts);
            sendJson(exchange, result.isValid() ? 200 : 422,
                    Map.of("valid", result.isValid(), "errors", result.getErrors()));
            return;
        }
        if (path.equals("/api/apply") && method.equals("POST")) {
            if (!(plugin instanceof CrestRpgPlugin crestRpg)) throw new RequestException(503, "插件熱載入不可用");
            try { sendJson(exchange, 200, Map.of("applied", true, "count", crestRpg.applyEditorDrafts())); }
            catch (RuntimeException exception) { throw new RequestException(422, "草稿已儲存，但無法套用：" + exception.getMessage()); }
            return;
        }
        if (path.equals("/api/token") && method.equals("PUT")) {
            JsonObject payload = readObject(exchange);
            String replacement = payload.has("token") ? payload.get("token").getAsString().trim() : "";
            if (replacement.length() < 16 || replacement.length() > 256) {
                throw new RequestException(400, "新 Token 必須介於 16 至 256 字元");
            }
            plugin.getConfig().set("web-editor.token", replacement);
            plugin.saveConfig();
            token = replacement;
            sendJson(exchange, 200, Map.of("updated", true));
            return;
        }
        throw new RequestException(404, "找不到 API");
    }

    private void serveAsset(HttpExchange exchange, String path) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) throw new RequestException(405, "不支援的方法");
        String resource = ASSETS.get(path);
        if (resource == null) throw new RequestException(404, "找不到頁面");
        try (InputStream input = plugin.getResource(resource)) {
            if (input == null) throw new RequestException(404, "找不到網頁資源");
            byte[] bytes = input.readAllBytes();
            String contentType = resource.endsWith(".css") ? "text/css; charset=utf-8"
                    : resource.endsWith(".js") ? "text/javascript; charset=utf-8" : "text/html; charset=utf-8";
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private boolean authorized(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return false;
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                header.substring(7).getBytes(StandardCharsets.UTF_8));
    }

    private static JsonObject readObject(HttpExchange exchange) throws IOException {
        int declared = parseContentLength(exchange);
        if (declared > MAX_REQUEST_BYTES) throw new RequestException(413, "請求內容過大");
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (bytes.length > MAX_REQUEST_BYTES) throw new RequestException(413, "請求內容過大");
        try {
            var parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IllegalArgumentException();
            return parsed.getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new RequestException(400, "請提供有效的 JSON 物件");
        }
    }

    private static byte[] readBytes(HttpExchange exchange, int limit) throws IOException {
        int declared = parseContentLength(exchange);
        if (declared > limit) throw new RequestException(413, "上傳檔案過大");
        byte[] bytes = exchange.getRequestBody().readNBytes(limit + 1);
        if (bytes.length > limit) throw new RequestException(413, "上傳檔案過大");
        return bytes;
    }

    private void sendAsset(HttpExchange exchange, String type, String name) throws IOException {
        String safe = new File(name).getName();
        if (!safe.matches("[A-Za-z0-9_.-]{1,128}") || safe.contains("..")) throw new RequestException(400, "檔名無效");
        File root = new File(plugin.getDataFolder(), type.equals("texture") ? "web-assets/textures/item" : "web-assets/models/item");
        File file = new File(root, safe + (type.equals("texture") ? ".png" : ".json"));
        if (!file.isFile()) throw new RequestException(404, "找不到素材");
        sendFile(exchange, file, type.equals("texture") ? "image/png" : "application/json; charset=utf-8", null);
    }

    private static void sendFile(HttpExchange exchange, File file, String contentType, String downloadName) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (downloadName != null) exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + downloadName + "\"");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static int parseContentLength(HttpExchange exchange) {
        try { return Integer.parseInt(exchange.getRequestHeaders().getFirst("Content-Length")); }
        catch (Exception ignored) { return -1; }
    }

    private static String requireKind(Map<String, String> query) {
        String kind = query.getOrDefault("kind", "");
        if (!KINDS.contains(kind)) throw new RequestException(400, "不支援的內容分類");
        return kind;
    }

    private static String requireKey(Map<String, String> query) {
        String key = query.getOrDefault("key", "").toLowerCase();
        if (!key.matches("[a-z0-9_-]{1,64}")) throw new RequestException(400, "代碼格式不正確");
        return key;
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> result = new LinkedHashMap<>();
        if (uri.getRawQuery() == null) return result;
        for (String pair : uri.getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(decode(parts[0]), parts.length == 2 ? decode(parts[1]) : "");
        }
        return result;
    }

    private static String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private static String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void assignMissingCustomModelData() {
        int next = 100_000;
        for (String kind : List.of("items", "weapons", "equipments", "gems")) {
            Map<String, JsonObject> values = drafts.getDrafts(kind);
            for (JsonObject draft : values.values()) {
                if (draft.has("custom_model_data")) next = Math.max(next, draft.get("custom_model_data").getAsInt() + 1);
            }
        }
        for (String kind : List.of("items", "weapons", "equipments", "gems")) {
            for (Map.Entry<String, JsonObject> entry : drafts.getDrafts(kind).entrySet()) {
                JsonObject draft = entry.getValue();
                if (draft.has("texture") && !draft.get("texture").getAsString().isBlank() && !draft.has("custom_model_data")) {
                    draft.addProperty("custom_model_data", next++);
                    drafts.saveDraft(kind, entry.getKey(), draft);
                }
            }
        }
    }
    private static List<String> orderedKinds() { return List.of("gems", "items", "equipments", "weapons", "affixes", "rarities", "sets", "classes", "skills", "skill-trees", "quests", "monsters", "drop-tables", "crafting-stations", "npcs", "gui-layouts", "guild-settings"); }
    private static boolean isLoopback(String host) { return host.equals("127.0.0.1") || host.equals("localhost") || host.equals("::1"); }

    private static void addSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: blob:; connect-src 'self'; frame-ancestors 'none'");
    }

    private static void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static final class RequestException extends RuntimeException {
        private final int status;
        private RequestException(int status, String message) { super(message); this.status = status; }
    }
}
