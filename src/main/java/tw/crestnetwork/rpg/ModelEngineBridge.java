package tw.crestnetwork.rpg;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;

final class ModelEngineBridge {
    private final CrestRpgPlugin plugin;

    ModelEngineBridge(CrestRpgPlugin plugin) { this.plugin = plugin; }

    void apply(LivingEntity entity, String modelId) {
        if (modelId == null || modelId.isBlank()) return;
        Plugin modelEngine = Bukkit.getPluginManager().getPlugin("ModelEngine");
        if (modelEngine == null || !modelEngine.isEnabled()) {
            plugin.getLogger().warning("怪物指定了 ModelEngine 模型 " + modelId + "，但 ModelEngine 尚未安裝。");
            return;
        }
        try {
            Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            Object modeledEntity = api.getMethod("createModeledEntity", org.bukkit.entity.Entity.class).invoke(null, entity);
            Object activeModel = api.getMethod("createActiveModel", String.class).invoke(null, modelId);
            Method addModel = null;
            for (Method method : modeledEntity.getClass().getMethods()) {
                if (method.getName().equals("addModel") && method.getParameterCount() >= 1) { addModel = method; break; }
            }
            if (addModel == null) throw new NoSuchMethodException("addModel");
            if (addModel.getParameterCount() == 1) addModel.invoke(modeledEntity, activeModel);
            else addModel.invoke(modeledEntity, activeModel, true);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("套用 ModelEngine 模型 " + modelId + " 失敗：" + exception.getMessage());
        }
    }
}
