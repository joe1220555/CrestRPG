package tw.crestnetwork.rpg;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class AdvancedItemService {
    private static final String LORE_PREFIX = "◆ ";
    private final Gson gson = new Gson();
    private final Supplier<RpgEngineRegistry> registry;
    private final NamespacedKey templateKey;
    private final NamespacedKey instanceKey;
    private final NamespacedKey stateKey;

    AdvancedItemService(CrestRpgPlugin plugin, Supplier<RpgEngineRegistry> registry) {
        this.registry = registry;
        templateKey = new NamespacedKey(plugin, "advanced_template");
        instanceKey = new NamespacedKey(plugin, "item_instance");
        stateKey = new NamespacedKey(plugin, "advanced_state");
    }

    public ItemStack decorate(ItemStack item, String contentKey) {
        RpgItemRoller.RolledItem rolled = RpgItemRoller.roll(registry.get(), contentKey, ThreadLocalRandom.current());
        if (rolled == null) return item;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(templateKey, PersistentDataType.STRING, contentKey);
        meta.getPersistentDataContainer().set(instanceKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        write(meta, rolled);
        render(meta, rolled);
        item.setItemMeta(meta);
        return item;
    }

    boolean identify(ItemStack item) {
        RpgItemRoller.RolledItem state = read(item);
        if (state == null || !state.unidentified()) return false;
        RpgItemRoller.RolledItem next = new RpgItemRoller.RolledItem(state.templateKey(), state.quality(), false,
                state.upgradeLevel(), state.affixes(), state.gems());
        update(item, next);
        return true;
    }

    boolean isUnidentified(ItemStack item) {
        RpgItemRoller.RolledItem state = read(item);
        return state != null && state.unidentified();
    }

    int upgradeLevel(ItemStack item) {
        RpgItemRoller.RolledItem state = read(item);
        return state == null || state.unidentified() ? -1 : state.upgradeLevel();
    }

    int maxUpgrade(ItemStack item) {
        RpgItemRoller.RolledItem state = read(item);
        if (state == null) return -1;
        RpgEngineRegistry.ItemTemplate template = registry.get().items().get(state.templateKey());
        return template == null ? -1 : template.maxUpgrade();
    }

    String gemMaterial(String gemKey) {
        RpgEngineRegistry.GemDefinition gem = registry.get().gems().get(gemKey.toLowerCase(Locale.ROOT));
        return gem == null ? null : gem.material();
    }

    boolean canSocket(ItemStack item, String gemKey) {
        RpgItemRoller.RolledItem state = read(item);
        return state != null && !state.unidentified() && state.gems().contains("")
                && registry.get().gems().containsKey(gemKey.toLowerCase(Locale.ROOT));
    }

    boolean upgrade(ItemStack item) {
        RpgItemRoller.RolledItem state = read(item);
        if (state == null || state.unidentified()) return false;
        RpgEngineRegistry.ItemTemplate template = registry.get().items().get(state.templateKey());
        if (template == null || state.upgradeLevel() >= template.maxUpgrade()) return false;
        RpgItemRoller.RolledItem next = new RpgItemRoller.RolledItem(state.templateKey(), state.quality(), false,
                state.upgradeLevel() + 1, state.affixes(), state.gems());
        update(item, next);
        return true;
    }

    boolean socket(ItemStack item, String gemKey) {
        RpgItemRoller.RolledItem state = read(item);
        RpgEngineRegistry.GemDefinition gem = registry.get().gems().get(gemKey.toLowerCase(Locale.ROOT));
        if (state == null || state.unidentified() || gem == null) return false;
        List<String> gems = new ArrayList<>(state.gems());
        int slot = gems.indexOf("");
        if (slot < 0) return false;
        gems.set(slot, gem.key());
        update(item, new RpgItemRoller.RolledItem(state.templateKey(), state.quality(), false,
                state.upgradeLevel(), state.affixes(), gems));
        return true;
    }

    Map<String, Double> stats(ItemStack item) {
        RpgItemRoller.RolledItem state = read(item);
        if (state == null || state.unidentified()) return Map.of();
        Map<String, Double> result = new LinkedHashMap<>();
        RpgEngineRegistry snapshot = registry.get();
        state.affixes().forEach((key, value) -> {
            RpgEngineRegistry.AffixDefinition affix = snapshot.affixes().get(key);
            if (affix != null) result.merge(affix.stat(), value, Double::sum);
        });
        for (String key : state.gems()) {
            RpgEngineRegistry.GemDefinition gem = snapshot.gems().get(key);
            if (gem != null) gem.stats().forEach((stat,value)->result.merge(stat,value,Double::sum));
        }
        double upgradeMultiplier = 1.0 + state.upgradeLevel() * 0.05;
        result.replaceAll((key, value) -> Math.round(value * upgradeMultiplier * 100.0) / 100.0);
        return Map.copyOf(result);
    }

    double salvageValue(ItemStack item) {
        RpgItemRoller.RolledItem state = read(item);
        if (state == null) return 0;
        RpgEngineRegistry.ItemTemplate template = registry.get().items().get(state.templateKey());
        return template == null ? 0 : template.salvageValue() * (0.5 + state.quality() / 100.0)
                * (1.0 + state.upgradeLevel() * 0.1);
    }

    Map<String, Double> activeSetBonuses(Player player) {
        Map<String, Integer> pieces = new LinkedHashMap<>();
        for (ItemStack item : player.getInventory().getArmorContents()) {
            RpgItemRoller.RolledItem state = read(item);
            if (state == null || state.unidentified()) continue;
            RpgEngineRegistry.ItemTemplate template = registry.get().items().get(state.templateKey());
            if (template != null && template.setId() != null) pieces.merge(template.setId(), 1, Integer::sum);
        }
        Map<String, Double> result = new LinkedHashMap<>();
        pieces.forEach((setKey, count) -> {
            RpgEngineRegistry.SetDefinition set = registry.get().sets().get(setKey);
            if (set == null) return;
            set.bonuses().entrySet().stream().filter(entry -> count >= entry.getKey()).forEach(entry ->
                    entry.getValue().forEach((stat, value) -> result.merge(stat, value, Double::sum)));
        });
        return Map.copyOf(result);
    }

    private void update(ItemStack item, RpgItemRoller.RolledItem state) {
        ItemMeta meta = item.getItemMeta();
        write(meta, state);
        render(meta, state);
        item.setItemMeta(meta);
    }

    private RpgItemRoller.RolledItem read(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        String json = item.getItemMeta().getPersistentDataContainer().get(stateKey, PersistentDataType.STRING);
        if (json == null) return null;
        try { return gson.fromJson(json, RpgItemRoller.RolledItem.class); }
        catch (RuntimeException ignored) { return null; }
    }

    private void write(ItemMeta meta, RpgItemRoller.RolledItem state) {
        meta.getPersistentDataContainer().set(stateKey, PersistentDataType.STRING, gson.toJson(state));
    }

    private void render(ItemMeta meta, RpgItemRoller.RolledItem state) {
        List<Component> lore = new ArrayList<>();
        if (meta.lore() != null) for (Component line : meta.lore()) {
            if (!PlainTextComponentSerializer.plainText().serialize(line).startsWith(LORE_PREFIX)) lore.add(line);
        }
        lore.add(Component.text(LORE_PREFIX + "品質：" + state.quality() + "%", NamedTextColor.GOLD));
        lore.add(Component.text(LORE_PREFIX + "強化：+" + state.upgradeLevel(), NamedTextColor.AQUA));
        if (state.unidentified()) {
            lore.add(Component.text(LORE_PREFIX + "尚未鑑定", NamedTextColor.DARK_PURPLE));
        } else {
            Map<String, Double> stats = statsFromState(state);
            stats.forEach((stat, value) -> lore.add(Component.text(LORE_PREFIX + stat + " +" + value, NamedTextColor.GREEN)));
            long empty = state.gems().stream().filter(String::isEmpty).count();
            lore.add(Component.text(LORE_PREFIX + "寶石槽：" + (state.gems().size() - empty) + "/" + state.gems().size(), NamedTextColor.BLUE));
        }
        meta.lore(lore);
    }

    private Map<String, Double> statsFromState(RpgItemRoller.RolledItem state) {
        Map<String, Double> result = new LinkedHashMap<>();
        RpgEngineRegistry snapshot = registry.get();
        state.affixes().forEach((key, value) -> {
            RpgEngineRegistry.AffixDefinition definition = snapshot.affixes().get(key);
            if (definition != null) result.merge(definition.stat(), value, Double::sum);
        });
        for (String key : state.gems()) {
            RpgEngineRegistry.GemDefinition gem = snapshot.gems().get(key);
            if (gem != null) gem.stats().forEach((stat,value)->result.merge(stat,value,Double::sum));
        }
        double multiplier = 1.0 + state.upgradeLevel() * 0.05;
        result.replaceAll((key, value) -> Math.round(value * multiplier * 100.0) / 100.0);
        return result;
    }
}
