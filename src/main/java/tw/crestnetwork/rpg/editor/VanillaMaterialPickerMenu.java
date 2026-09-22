package tw.crestnetwork.rpg.editor;

import com.google.gson.JsonObject;
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
import tw.crestnetwork.rpg.CrestRpgPlugin;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Native Minecraft item browser used by material fields in the in-game draft editor. */
public final class VanillaMaterialPickerMenu implements Listener {
    private static final int PAGE_SIZE = 45;
    private static final List<Material> MATERIALS = Arrays.stream(Material.values())
            .filter(material -> material.isItem() && !material.isAir())
            .sorted(Comparator.comparing(material -> material.getKey().toString()))
            .toList();

    private record PickerHolder(String kind, String key, String field, int page) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    private record CallbackHolder(Consumer<Material> selected, Runnable cancelled, int page) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private final CrestRpgPlugin plugin;
    private final EditorSessionManager sessions;

    public VanillaMaterialPickerMenu(CrestRpgPlugin plugin, EditorSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
    }

    public void open(Player player, String kind, String key, String field, int requestedPage) {
        int lastPage = Math.max(0, (MATERIALS.size() - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, lastPage));
        Inventory inventory = Bukkit.createInventory(new PickerHolder(kind, key, field, page), 54,
                Component.text("Minecraft 圖示 · " + (page + 1) + "/" + (lastPage + 1), NamedTextColor.DARK_AQUA));
        int start = page * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE && start + slot < MATERIALS.size(); slot++) {
            Material material = MATERIALS.get(start + slot);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(material.getKey().toString(), NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("點擊選擇這個材質", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
        if (page > 0) inventory.setItem(45, button(Material.ARROW, "上一頁"));
        inventory.setItem(49, button(Material.BARRIER, "取消並返回"));
        if (page < lastPage) inventory.setItem(53, button(Material.ARROW, "下一頁"));
        player.openInventory(inventory);
    }

    public void open(Player player, Consumer<Material> selected, Runnable cancelled) {
        openCallback(player, selected, cancelled, 0);
    }

    private void openCallback(Player player, Consumer<Material> selected, Runnable cancelled, int requestedPage) {
        int lastPage = Math.max(0, (MATERIALS.size() - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, lastPage));
        Inventory inventory = Bukkit.createInventory(new CallbackHolder(selected, cancelled, page), 54,
                Component.text("Minecraft 材質 · " + (page + 1) + "/" + (lastPage + 1), NamedTextColor.DARK_AQUA));
        fillMaterials(inventory, page);
        if (page > 0) inventory.setItem(45, button(Material.ARROW, "上一頁"));
        inventory.setItem(49, button(Material.BARRIER, "取消並返回"));
        if (page < lastPage) inventory.setItem(53, button(Material.ARROW, "下一頁"));
        player.openInventory(inventory);
    }

    private static void fillMaterials(Inventory inventory, int page) {
        int start = page * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE && start + slot < MATERIALS.size(); slot++) {
            Material material = MATERIALS.get(start + slot);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(material.getKey().toString(), NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("點擊選擇這個材質", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CallbackHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            int slot = event.getRawSlot();
            if (slot == 45 && holder.page() > 0) openCallback(player, holder.selected(), holder.cancelled(), holder.page() - 1);
            else if (slot == 49) holder.cancelled().run();
            else if (slot == 53) openCallback(player, holder.selected(), holder.cancelled(), holder.page() + 1);
            else { int index = holder.page() * PAGE_SIZE + slot; if (slot >= 0 && slot < PAGE_SIZE && index < MATERIALS.size()) holder.selected().accept(MATERIALS.get(index)); }
            return;
        }
        if (!(event.getInventory().getHolder() instanceof PickerHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot == 45 && holder.page() > 0) {
            open(player, holder.kind(), holder.key(), holder.field(), holder.page() - 1);
            return;
        }
        if (slot == 49) {
            new UniversalDraftEditorMenu(plugin, sessions).openDetail(player, holder.kind(), holder.key());
            return;
        }
        if (slot == 53) {
            open(player, holder.kind(), holder.key(), holder.field(), holder.page() + 1);
            return;
        }
        int index = holder.page() * PAGE_SIZE + slot;
        if (slot < 0 || slot >= PAGE_SIZE || index >= MATERIALS.size()) return;
        Material material = MATERIALS.get(index);
        JsonObject draft = sessions.getDraftManager().getDraft(holder.kind(), holder.key());
        if (draft == null) return;
        draft.addProperty(holder.field(), material.getKey().toString());
        sessions.getDraftManager().saveDraft(holder.kind(), holder.key(), draft);
        player.sendMessage(Component.text("[RPG 編輯器] 已選擇 " + material.getKey(), NamedTextColor.GREEN));
        new UniversalDraftEditorMenu(plugin, sessions).openDetail(player, holder.kind(), holder.key());
    }

    private static ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
    }
}
