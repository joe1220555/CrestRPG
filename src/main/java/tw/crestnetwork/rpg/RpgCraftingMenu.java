package tw.crestnetwork.rpg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class RpgCraftingMenu implements Listener {
    private final CrestRpgPlugin plugin;

    RpgCraftingMenu(CrestRpgPlugin plugin) { this.plugin = plugin; }

    void open(Player player, String stationKey) {
        RpgEngineRegistry.CraftingStationDefinition station = plugin.engineSnapshot().craftingStations().get(stationKey);
        if (station == null) {
            player.sendMessage(Component.text("找不到製作站：" + stationKey, NamedTextColor.RED));
            return;
        }
        StationHolder holder = new StationHolder(station);
        int size = Math.max(9, Math.min(54, ((station.recipes().size() + 8) / 9) * 9));
        Inventory inventory = Bukkit.createInventory(holder, size, Component.text(station.name(), NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        for (int index = 0; index < station.recipes().size() && index < size; index++) {
            RpgEngineRegistry.RecipeDefinition recipe = station.recipes().get(index);
            holder.recipes.put(index, recipe);
            inventory.setItem(index, recipeIcon(recipe));
        }
        player.openInventory(inventory);
    }

    private ItemStack recipeIcon(RpgEngineRegistry.RecipeDefinition recipe) {
        ItemStack icon = plugin.createGeneratedContent(recipe.output());
        if (icon == null) icon = new ItemStack(Material.BARRIER);
        ItemMeta meta = icon.getItemMeta();
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Component.empty());
        lore.add(Component.text("製作數量：" + recipe.amount(), NamedTextColor.AQUA));
        lore.add(Component.text("需求等級：" + recipe.requiredLevel(), NamedTextColor.GRAY));
        lore.add(Component.text("所需材料：", NamedTextColor.YELLOW));
        for (RpgEngineRegistry.IngredientDefinition ingredient : recipe.ingredients()) {
            lore.add(Component.text("  " + ingredient.key() + " x" + ingredient.amount(), NamedTextColor.GRAY));
        }
        lore.add(Component.text("點擊製作", NamedTextColor.GREEN));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StationHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        RpgEngineRegistry.RecipeDefinition recipe = holder.recipes.get(event.getRawSlot());
        if (recipe == null) return;
        if (plugin.characterLevel(player) < recipe.requiredLevel()) {
            player.sendMessage(Component.text("角色等級不足。", NamedTextColor.RED));
            return;
        }
        if (!recipe.permission().isBlank() && !player.hasPermission(recipe.permission())) {
            player.sendMessage(Component.text("你沒有此配方的權限。", NamedTextColor.RED));
            return;
        }
        Map<IngredientKey, Integer> totalCosts = new LinkedHashMap<>();
        for (RpgEngineRegistry.IngredientDefinition ingredient : recipe.ingredients()) {
            totalCosts.merge(new IngredientKey(ingredient.key(), ingredient.content()), ingredient.amount(), Integer::sum);
        }
        for (Map.Entry<IngredientKey, Integer> cost : totalCosts.entrySet()) {
            RpgEngineRegistry.IngredientDefinition ingredient = cost.getKey().definition(cost.getValue());
            if (count(player, ingredient) < cost.getValue()) {
                player.sendMessage(Component.text("材料不足：" + ingredient.key(), NamedTextColor.RED));
                return;
            }
        }
        List<ItemStack> outputs = new ArrayList<>();
        for (int index = 0; index < recipe.amount(); index++) {
            ItemStack output = plugin.createGeneratedContent(recipe.output());
            if (output == null) {
                player.sendMessage(Component.text("配方產物設定無效，請聯絡管理員。", NamedTextColor.RED));
                return;
            }
            outputs.add(output);
        }
        for (Map.Entry<IngredientKey, Integer> cost : totalCosts.entrySet()) {
            RpgEngineRegistry.IngredientDefinition ingredient = cost.getKey().definition(cost.getValue());
            if (!remove(player, ingredient, cost.getValue())) {
                player.sendMessage(Component.text("材料狀態已變更，製作已取消。", NamedTextColor.RED));
                return;
            }
        }
        for (ItemStack output : outputs) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(output);
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.sendMessage(Component.text("製作完成。", NamedTextColor.GREEN));
    }

    private int count(Player player, RpgEngineRegistry.IngredientDefinition ingredient) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (matches(item, ingredient)) count += item.getAmount();
        }
        return count;
    }

    private boolean remove(Player player, RpgEngineRegistry.IngredientDefinition ingredient, int requested) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        int remaining = requested;
        for (int slot = 0; slot < storage.length && remaining > 0; slot++) {
            ItemStack item = storage[slot];
            if (!matches(item, ingredient)) continue;
            int removed = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - removed);
            remaining -= removed;
            storage[slot] = item.getAmount() == 0 ? null : item;
        }
        player.getInventory().setStorageContents(storage);
        return remaining == 0;
    }

    private record IngredientKey(String key, boolean content) {
        RpgEngineRegistry.IngredientDefinition definition(int amount) {
            return new RpgEngineRegistry.IngredientDefinition(key, amount, content);
        }
    }

    private boolean matches(ItemStack item, RpgEngineRegistry.IngredientDefinition ingredient) {
        if (item == null || item.getType().isAir()) return false;
        if (ingredient.content()) return ingredient.key().equals(plugin.contentId(item));
        Material material = Material.matchMaterial(ingredient.key());
        return material != null && item.getType() == material && plugin.contentId(item) == null;
    }

    private static final class StationHolder implements InventoryHolder {
        private final RpgEngineRegistry.CraftingStationDefinition station;
        private final Map<Integer, RpgEngineRegistry.RecipeDefinition> recipes = new LinkedHashMap<>();
        private Inventory inventory;
        private StationHolder(RpgEngineRegistry.CraftingStationDefinition station) { this.station = station; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
