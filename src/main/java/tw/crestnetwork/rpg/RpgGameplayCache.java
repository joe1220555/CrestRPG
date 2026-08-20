package tw.crestnetwork.rpg;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class RpgGameplayCache {
    private final CrestRpgPlugin plugin;

    RpgGameplayCache(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    void save(RpgManifest manifest) throws IOException {
        JsonObject root = new JsonObject();
        for (RpgGameplayDefinition definition : manifest.gameplay()) {
            if (definition.enabled()) root.add(definition.section(), definition.data());
        }
        Path directory = plugin.getDataFolder().toPath();
        Files.createDirectories(directory);
        Path target = directory.resolve("gameplay-cache.json");
        Path temporary = directory.resolve("gameplay-cache.json.tmp");
        Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
