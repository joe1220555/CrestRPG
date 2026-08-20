package tw.crestnetwork.rpg;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

final class ModelEngineAssetSynchronizer {
    private static final int MAX_ASSET_BYTES = 20 * 1024 * 1024;
    private final CrestRpgPlugin plugin;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ModelEnginePackMerger packMerger = new ModelEnginePackMerger();

    ModelEngineAssetSynchronizer(CrestRpgPlugin plugin) { this.plugin = plugin; }

    void synchronize(RpgManifest manifest) {
        Plugin modelEngine = Bukkit.getPluginManager().getPlugin("ModelEngine");
        if (modelEngine == null || !modelEngine.isEnabled()) return;
        try {
            Path state = plugin.getDataFolder().toPath().resolve("modelengine-manifest.txt");
            Path modelEngineFolder = plugin.getDataFolder().toPath().getParent().resolve("ModelEngine");
            boolean manifestChanged = !Files.exists(state) || !Files.readString(state).trim().equals(manifest.checksum());
            boolean blueprintsChanged = false;
            if (manifestChanged) {
                for (RpgMonsterDefinition monster : manifest.monsters()) {
                    if (!monster.enabled() || monster.modelEngineId() == null) continue;
                    if (monster.modelUrl() != null) {
                        download(monster.modelUrl(), modelEngineFolder.resolve("blueprints/" + monster.modelEngineId() + ".bbmodel"));
                        blueprintsChanged = true;
                    }
                    if (monster.textureUrl() != null) {
                        download(monster.textureUrl(), modelEngineFolder.resolve("blueprints/textures/" + monster.modelEngineId() + ".png"));
                        blueprintsChanged = true;
                    }
                }
                Files.createDirectories(state.getParent());
                atomicWrite(state, manifest.checksum().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            if (blueprintsChanged) {
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "meg reload models"));
                schedulePackMerge(modelEngineFolder, 60L);
            } else {
                schedulePackMerge(modelEngineFolder, 1L);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("ModelEngine 怪物模型同步失敗，將顯示 Minecraft 基礎怪物：" + exception.getMessage());
        }
    }

    private void schedulePackMerge(Path modelEngineFolder, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Plugin oraxen = Bukkit.getPluginManager().getPlugin("Oraxen");
            if (oraxen == null || !oraxen.isEnabled()) {
                plugin.getLogger().warning("ModelEngine 已載入，但 Oraxen 尚未啟用，無法發送合併後的怪物資源包。");
                return;
            }
            try {
                Path oraxenPack = plugin.getDataFolder().toPath().getParent().resolve("Oraxen/pack");
                boolean changed = packMerger.merge(
                        modelEngineFolder.resolve("resource pack"),
                        oraxenPack,
                        Bukkit.getMinecraftVersion()
                );
                if (!changed) return;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "oraxen reload all");
                plugin.getLogger().info("ModelEngine 怪物模型已合併至 Oraxen 資源包並重新發送。");
            } catch (Exception exception) {
                plugin.getLogger().warning("ModelEngine 與 Oraxen 資源包合併失敗：" + exception.getMessage());
            }
        }, delayTicks);
    }

    private void download(String url, Path target) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) throw new IOException("不允許的素材網址協定");
        HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("素材下載 HTTP " + response.statusCode());
        if (response.body().length == 0 || response.body().length > MAX_ASSET_BYTES) throw new IOException("素材大小不合法");
        Files.createDirectories(target.getParent());
        atomicWrite(target, response.body());
    }

    private void atomicWrite(Path target, byte[] content) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temporary, content);
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
