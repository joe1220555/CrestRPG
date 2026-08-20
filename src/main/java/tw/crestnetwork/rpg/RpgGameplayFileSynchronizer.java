package tw.crestnetwork.rpg;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

final class RpgGameplayFileSynchronizer {
    private final Gson gson = new Gson();
    private final CrestRpgPlugin plugin;

    RpgGameplayFileSynchronizer(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    boolean synchronize(RpgGameplayDefinition definition) {
        if (!definition.section().equals("quests")) return false;
        Map<?, ?> values = gson.fromJson(definition.data(), Map.class);
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            yaml.set(String.valueOf(entry.getKey()), entry.getValue());
        }
        File target = new File(plugin.getDataFolder(), "quests.yml");
        try {
            yaml.save(target);
            return true;
        } catch (IOException exception) {
            throw new IllegalStateException("無法更新 quests.yml：" + exception.getMessage(), exception);
        }
    }
}
