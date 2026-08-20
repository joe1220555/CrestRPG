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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tw.crestnetwork.rpg.CrestRpgPlugin;
import java.util.List;
import java.util.Map;

public final class WeaponEditorMenu implements Listener {
    private final CrestRpgPlugin plugin;
    private final EditorSessionManager sessionManager;

    public WeaponEditorMenu(CrestRpgPlugin plugin, EditorSessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("RPG 武器草稿管理", NamedTextColor.DARK_AQUA));
        Map<String, JsonObject> drafts = sessionManager.getDraftManager().getDrafts("weapons");

        int slot = 0;
        for (Map.Entry<String, JsonObject> entry : drafts.entrySet()) {
            if (slot >= 45) break;
            JsonObject obj = entry.getValue();
            String name = obj.has("name") ? obj.get("name").getAsString() : entry.getKey();
            inv.setItem(slot++, createIcon(Material.DIAMOND_SWORD, name + " (" + entry.getKey() + ")",
                    List.of("傷害: " + (obj.has("attack_damage") ? obj.get("attack_damage").getAsDouble() : 10))));
        }

        inv.setItem(48, createIcon(Material.NETHER_STAR, "新增武器草稿", List.of("建立全新的武器草稿")));
        inv.setItem(49, createIcon(Material.ARROW, "返回主選單", List.of()));
        player.openInventory(inv);
    }

    private ItemStack createIcon(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        if (!loreLines.isEmpty()) {
            meta.lore(loreLines.stream().map(line -> Component.text(line, NamedTextColor.GRAY)).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.contains("RPG 武器草稿管理")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot == 49) {
            new RpgEditorMenu(plugin, sessionManager).open(player);
            return;
        }
        if (slot == 48) {
            EditorSession session = sessionManager.getOrCreateSession(player);
            player.closeInventory();
            player.sendMessage(Component.text("[RPG 編輯器] 請在聊天欄輸入新武器的 Key (代碼，如 flame_sword)：", NamedTextColor.YELLOW));
            session.awaitChatInput("新武器 Key", input -> {
                String key = input.toLowerCase().trim();
                JsonObject obj = new JsonObject();
                obj.addProperty("key", key);
                obj.addProperty("name", "火焰之劍 " + key);
                obj.addProperty("base_item", "minecraft:diamond_sword");
                obj.addProperty("level", 1);
                obj.addProperty("attack_damage", 25.0);
                obj.addProperty("attack_speed", 1.6);
                obj.addProperty("critical_chance", 0.15);
                obj.addProperty("enabled", true);
                sessionManager.getDraftManager().saveDraft("weapons", key, obj);
                player.sendMessage(Component.text("[RPG 編輯器] 已成功建立武器草稿：" + key, NamedTextColor.GREEN));
                open(player);
            });
        }
    }
}
