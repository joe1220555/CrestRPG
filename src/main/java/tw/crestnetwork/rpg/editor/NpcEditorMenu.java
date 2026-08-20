package tw.crestnetwork.rpg.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

public final class NpcEditorMenu implements Listener {
    private final CrestRpgPlugin plugin;
    private final EditorSessionManager sessionManager;

    public NpcEditorMenu(CrestRpgPlugin plugin, EditorSessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("RPG NPC 草稿管理", NamedTextColor.DARK_AQUA));
        Map<String, JsonObject> drafts = sessionManager.getDraftManager().getDrafts("npcs");

        int slot = 0;
        for (Map.Entry<String, JsonObject> entry : drafts.entrySet()) {
            if (slot >= 45) break;
            JsonObject obj = entry.getValue();
            String name = obj.has("display_name") ? obj.get("display_name").getAsString() : entry.getKey();
            inv.setItem(slot++, createIcon(Material.ARMOR_STAND, name + " (" + entry.getKey() + ")",
                    List.of("點擊設置當前位置 / 編輯對話")));
        }

        inv.setItem(48, createIcon(Material.NETHER_STAR, "在當前位置建立 NPC", List.of("在此處建立全新 NPC 草稿")));
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
        if (!title.contains("RPG NPC 草稿管理")) return;
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
            player.sendMessage(Component.text("[RPG 編輯器] 請在聊天欄輸入新 NPC 的 Key (代碼，如 blacksmith_jack)：", NamedTextColor.YELLOW));
            session.awaitChatInput("新 NPC Key", input -> {
                String key = input.toLowerCase().trim();
                Location loc = player.getLocation();
                JsonObject obj = new JsonObject();
                obj.addProperty("key", key);
                obj.addProperty("display_name", "&6NPC " + key);
                obj.addProperty("entity_type", "PLAYER");
                obj.addProperty("world", loc.getWorld().getName());
                obj.addProperty("x", Math.round(loc.getX() * 100.0) / 100.0);
                obj.addProperty("y", Math.round(loc.getY() * 100.0) / 100.0);
                obj.addProperty("z", Math.round(loc.getZ() * 100.0) / 100.0);
                obj.addProperty("yaw", Math.round(loc.getYaw() * 10.0) / 10.0);
                obj.addProperty("pitch", Math.round(loc.getPitch() * 10.0) / 10.0);
                obj.addProperty("interaction_type", "DIALOGUE");

                JsonArray holograms = new JsonArray();
                holograms.add("&6[" + key + "]");
                holograms.add("&f右鍵點擊進行對話");
                obj.add("holograms", holograms);

                JsonArray dialogues = new JsonArray();
                dialogues.add("你好，冒險者！歡迎來到 CrestRPG 世界！");
                obj.add("dialogues", dialogues);

                sessionManager.getDraftManager().saveDraft("npcs", key, obj);
                player.sendMessage(Component.text("[RPG 編輯器] 已成功在當前位置建立 NPC 草稿：" + key, NamedTextColor.GREEN));
                open(player);
            });
        }
    }
}
