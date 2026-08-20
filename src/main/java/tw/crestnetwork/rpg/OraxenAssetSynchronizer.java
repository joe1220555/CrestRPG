package tw.crestnetwork.rpg;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

final class OraxenAssetSynchronizer {
    private static final int MAX_ASSET_BYTES = 12 * 1024 * 1024;
    private final CrestRpgPlugin plugin;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    OraxenAssetSynchronizer(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    void synchronize(RpgManifest manifest) {
        Plugin oraxen = Bukkit.getPluginManager().getPlugin("Oraxen");
        if (oraxen == null || !oraxen.isEnabled()) return;
        try {
            Path oraxenFolder = plugin.getDataFolder().toPath().getParent().resolve("Oraxen");
            Path stateFile = plugin.getDataFolder().toPath().resolve("oraxen-manifest.txt");
            String previous = Files.exists(stateFile) ? Files.readString(stateFile).trim() : "";
            if (manifest.checksum().equals(previous)) return;

            List<RpgOraxenAsset> assets = new ArrayList<>();
            assets.addAll(manifest.weapons());
            assets.addAll(manifest.equipments());
            assets.addAll(manifest.items());
            StringBuilder yaml = new StringBuilder("# 由 CrestRPG 網站自動產生，請勿手動修改。\n");
            for (RpgOraxenAsset asset : assets) {
                if (!asset.enabled()) continue;
                writeAsset(oraxenFolder, asset, yaml);
            }
            Path itemConfig = oraxenFolder.resolve("items/crest_generated.yml");
            Files.createDirectories(itemConfig.getParent());
            atomicWrite(itemConfig, yaml.toString().getBytes(StandardCharsets.UTF_8));
            Files.createDirectories(stateFile.getParent());
            atomicWrite(stateFile, manifest.checksum().getBytes(StandardCharsets.UTF_8));
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "oraxen reload all");
                plugin.getLogger().info("Oraxen 網站材質與模型已更新。若玩家未立即看到，請重新套用資源包。");
            });
        } catch (Exception exception) {
            plugin.getLogger().warning("Oraxen 素材同步失敗，遊戲仍會使用基礎物品：" + exception.getMessage());
        }
    }

    private void writeAsset(Path folder, RpgOraxenAsset asset, StringBuilder yaml) throws IOException, InterruptedException {
        String id = asset.oraxenId();
        yaml.append('\n').append(id).append(":\n")
                .append("  displayname: \"").append(escapeYaml(asset.name())).append("\"\n")
                .append("  material: ").append(asset.baseItem().replace("minecraft:", "").toUpperCase()).append('\n');
        if (asset.modelUrl() != null) {
            download(asset.modelUrl(), folder.resolve("pack/models/crest/" + id + ".json"));
            if (asset.textureUrl() != null) download(asset.textureUrl(), folder.resolve("pack/textures/crest/" + id + ".png"));
            yaml.append("  Pack:\n    generate_model: false\n    model: crest/").append(id).append('\n');
        } else if (asset.textureUrl() != null) {
            download(asset.textureUrl(), folder.resolve("pack/textures/crest/" + id + ".png"));
            yaml.append("  Pack:\n    generate_model: true\n    parent_model: ")
                    .append(isHandheld(asset.baseItem()) ? "item/handheld" : "item/generated")
                    .append("\n    textures:\n      - crest/").append(id).append(".png\n");
        }
    }

    private boolean isHandheld(String material) {
        String upper = material.toUpperCase();
        return upper.matches(".*_(SWORD|PICKAXE|AXE|SHOVEL|HOE)$") || upper.endsWith(":BOW") || upper.endsWith(":CROSSBOW");
    }

    private void download(String url, Path target) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("不允許的素材網址協定");
        }
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
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

    private String escapeYaml(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
