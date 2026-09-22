package tw.crestnetwork.rpg.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tw.crestnetwork.rpg.CrestRpgPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Shared field editor used by both the in-game GUI and, later, the embedded web editor.
 * All mutations pass through RpgDraftManager so there is only one content source of truth.
 */
public final class UniversalDraftEditorMenu implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Map<String, String> LIST_TITLES = Map.of(
            "RPG 物品草稿管理", "items",
            "RPG 武器草稿管理", "weapons",
            "RPG 裝備草稿管理", "equipments",
            "RPG 寶石草稿管理", "gems",
            "RPG 詞綴草稿管理", "affixes",
            "RPG 掉落表草稿管理", "drop-tables",
            "RPG 稀有度草稿管理", "rarities"
    );

    private enum ValueType { TEXT, INTEGER, DECIMAL, BOOLEAN, MATERIAL, TEXT_LIST, DROP_ENTRIES }
    private record Field(String key, String label, ValueType type, Material icon, String help) {}
    private record DetailHolder(String kind, String key) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private final CrestRpgPlugin plugin;
    private final EditorSessionManager sessions;

    public UniversalDraftEditorMenu(CrestRpgPlugin plugin, EditorSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
    }

    public void openList(Player player, String kind) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("RPG " + kindName(kind) + "草稿管理", NamedTextColor.DARK_AQUA));
        int slot = 0;
        for (Map.Entry<String, JsonObject> entry : sessions.getDraftManager().getDrafts(kind).entrySet()) {
            if (slot >= 45) break;
            String name = stringValue(entry.getValue(), "name", entry.getKey());
            inv.setItem(slot++, icon(iconFor(kind), name + " (" + entry.getKey() + ")",
                    List.of("左鍵：開啟完整欄位編輯", "可編輯、複製、預覽與刪除")));
        }
        inv.setItem(48, icon(Material.LIME_DYE, "新增" + kindName(kind), List.of("建立新草稿")));
        inv.setItem(49, icon(Material.ARROW, "返回主選單", List.of()));
        player.openInventory(inv);
    }

    void openDetail(Player player, String kind, String key) {
        JsonObject draft = sessions.getDraftManager().getDraft(kind, key);
        if (draft == null) {
            player.sendMessage(Component.text("[RPG 編輯器] 找不到草稿：" + key, NamedTextColor.RED));
            openList(player, kind);
            return;
        }
        Inventory inv = Bukkit.createInventory(new DetailHolder(kind, key), 54,
                Component.text("編輯 " + kindName(kind) + " · " + key, NamedTextColor.DARK_AQUA));
        List<Field> fields = fieldsFor(kind);
        for (int i = 0; i < fields.size() && i < 36; i++) {
            Field field = fields.get(i);
            String value = displayValue(draft, field);
            List<String> lore = new ArrayList<>();
            lore.add("目前：" + value);
            lore.add(field.help());
            lore.add(field.type() == ValueType.BOOLEAN ? "點擊切換" : "點擊後於聊天欄輸入");
            if (field.type() == ValueType.MATERIAL) {
                lore.set(lore.size() - 1, "左鍵：開啟 Minecraft 圖示選擇器");
                lore.add("Shift+左鍵：使用主手物品");
            }
            inv.setItem(fieldSlot(i), icon(field.icon(), field.label(), lore));
        }
        inv.setItem(45, icon(Material.SPYGLASS, "預覽資料", previewLore(draft)));
        inv.setItem(47, icon(Material.WRITABLE_BOOK, "複製草稿", List.of("點擊後輸入新代碼")));
        inv.setItem(49, icon(Material.ARROW, "返回列表", List.of()));
        inv.setItem(53, icon(Material.RED_DYE, "刪除草稿", List.of("需在聊天欄輸入 DELETE " + key + " 確認")));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof DetailHolder holder) {
            event.setCancelled(true);
            handleDetailClick(player, holder, event.getRawSlot(), event.getClick());
            return;
        }
        String title = PLAIN.serialize(event.getView().title());
        String kind = LIST_TITLES.get(title);
        if (kind == null) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (kind.equals("rarities") && slot == 48) {
            requestInput(player, "請輸入新稀有度代碼（例如 epic）", input -> createRarity(player, input));
            return;
        }
        if (kind.equals("rarities") && slot == 49) {
            new RpgEditorMenu(plugin, sessions).open(player);
            return;
        }
        if (slot < 0 || slot >= 45) return;
        List<String> keys = new ArrayList<>(sessions.getDraftManager().getDrafts(kind).keySet());
        if (slot < keys.size()) openDetail(player, kind, keys.get(slot));
    }

    private void createRarity(Player player, String input) {
        String key = normalizeKey(input);
        if (!validKey(key) || sessions.getDraftManager().getDraft("rarities", key) != null) {
            player.sendMessage(Component.text("[RPG 編輯器] 代碼無效或已存在。", NamedTextColor.RED));
            openList(player, "rarities");
            return;
        }
        JsonObject rarity = new JsonObject();
        rarity.addProperty("name", key);
        rarity.addProperty("color", "<white>");
        rarity.addProperty("weight", 10.0);
        rarity.addProperty("enabled", true);
        sessions.getDraftManager().saveDraft("rarities", key, rarity);
        openDetail(player, "rarities", key);
    }

    private void handleDetailClick(Player player, DetailHolder holder, int slot, ClickType click) {
        if (slot == 49) {
            openList(player, holder.kind());
            return;
        }
        if (slot == 47) {
            requestInput(player, "請輸入複製後的新代碼", input -> duplicate(player, holder, input));
            return;
        }
        if (slot == 53) {
            requestInput(player, "請輸入 DELETE " + holder.key() + " 確認刪除", input -> delete(player, holder, input));
            return;
        }
        int index = fieldIndex(slot);
        List<Field> fields = fieldsFor(holder.kind());
        if (index < 0 || index >= fields.size()) return;
        Field field = fields.get(index);
        JsonObject draft = sessions.getDraftManager().getDraft(holder.kind(), holder.key());
        if (draft == null) return;
        if (field.type() == ValueType.BOOLEAN) {
            boolean current = draft.has(field.key()) && draft.get(field.key()).getAsBoolean();
            draft.addProperty(field.key(), !current);
            saveAndReopen(player, holder, draft, field.label() + " 已切換為 " + (!current ? "開啟" : "關閉"));
            return;
        }
        if (field.type() == ValueType.MATERIAL && click.isShiftClick()) {
            Material material = player.getInventory().getItemInMainHand().getType();
            if (material.isAir()) {
                player.sendMessage(Component.text("[RPG 編輯器] 請先在主手拿著要使用的物品。", NamedTextColor.RED));
                return;
            }
            draft.addProperty(field.key(), material.getKey().toString());
            saveAndReopen(player, holder, draft, "已從主手讀取材質 " + material.getKey());
            return;
        }
        if (field.type() == ValueType.MATERIAL) {
            new VanillaMaterialPickerMenu(plugin, sessions).open(player, holder.kind(), holder.key(), field.key(), 0);
            return;
        }
        requestInput(player, inputHelp(field), input -> updateField(player, holder, field, input));
    }

    private void updateField(Player player, DetailHolder holder, Field field, String input) {
        JsonObject draft = sessions.getDraftManager().getDraft(holder.kind(), holder.key());
        if (draft == null) return;
        try {
            switch (field.type()) {
                case TEXT, MATERIAL -> {
                    if (field.type() == ValueType.TEXT && input.equals("-")) {
                        draft.remove(field.key());
                        break;
                    }
                    if (field.type() == ValueType.MATERIAL && Material.matchMaterial(input) == null) {
                        throw new IllegalArgumentException("找不到 Minecraft 材質：" + input);
                    }
                    draft.addProperty(field.key(), input);
                }
                case INTEGER -> draft.addProperty(field.key(), Integer.parseInt(input));
                case DECIMAL -> draft.addProperty(field.key(), Double.parseDouble(input));
                case TEXT_LIST -> {
                    JsonArray values = new JsonArray();
                    if (!input.equals("-") && !input.isBlank()) {
                        for (String value : input.split(",")) if (!value.isBlank()) values.add(value.trim());
                    }
                    draft.add(field.key(), values);
                }
                case DROP_ENTRIES -> draft.add(field.key(), parseDropEntries(input));
                case BOOLEAN -> throw new IllegalStateException("布林值不使用聊天輸入");
            }
            saveAndReopen(player, holder, draft, field.label() + " 已更新");
        } catch (RuntimeException exception) {
            player.sendMessage(Component.text("[RPG 編輯器] 輸入無效：" + exception.getMessage(), NamedTextColor.RED));
            openDetail(player, holder.kind(), holder.key());
        }
    }

    private JsonArray parseDropEntries(String input) {
        JsonArray entries = new JsonArray();
        if (input.equals("-") || input.isBlank()) return entries;
        for (String raw : input.split(",")) {
            String[] parts = raw.trim().split(":");
            if (parts.length < 2 || parts.length > 4) throw new IllegalArgumentException("掉落項目格式應為 代碼:權重:最小:最大");
            JsonObject entry = new JsonObject();
            entry.addProperty("key", parts[0]);
            entry.addProperty("weight", Double.parseDouble(parts[1]));
            entry.addProperty("min", parts.length >= 3 ? Integer.parseInt(parts[2]) : 1);
            entry.addProperty("max", parts.length >= 4 ? Integer.parseInt(parts[3]) : 1);
            entries.add(entry);
        }
        return entries;
    }

    private void duplicate(Player player, DetailHolder holder, String input) {
        String newKey = normalizeKey(input);
        if (!validKey(newKey)) {
            player.sendMessage(Component.text("[RPG 編輯器] 代碼只能使用小寫英數、_ 或 -。", NamedTextColor.RED));
            openDetail(player, holder.kind(), holder.key());
            return;
        }
        if (sessions.getDraftManager().getDraft(holder.kind(), newKey) != null) {
            player.sendMessage(Component.text("[RPG 編輯器] 這個代碼已存在。", NamedTextColor.RED));
            openDetail(player, holder.kind(), holder.key());
            return;
        }
        JsonObject source = sessions.getDraftManager().getDraft(holder.kind(), holder.key());
        if (source != null) sessions.getDraftManager().saveDraft(holder.kind(), newKey, source.deepCopy());
        player.sendMessage(Component.text("[RPG 編輯器] 已複製為 " + newKey, NamedTextColor.GREEN));
        openDetail(player, holder.kind(), newKey);
    }

    private void delete(Player player, DetailHolder holder, String input) {
        if (!("DELETE " + holder.key()).equals(input)) {
            player.sendMessage(Component.text("[RPG 編輯器] 確認文字不符，未刪除任何資料。", NamedTextColor.YELLOW));
            openDetail(player, holder.kind(), holder.key());
            return;
        }
        sessions.getDraftManager().deleteDraft(holder.kind(), holder.key());
        player.sendMessage(Component.text("[RPG 編輯器] 已刪除 " + holder.key(), NamedTextColor.GREEN));
        openList(player, holder.kind());
    }

    private void requestInput(Player player, String prompt, Consumer<String> action) {
        player.closeInventory();
        player.sendMessage(Component.text("[RPG 編輯器] " + prompt + "（輸入 取消 可中止）", NamedTextColor.YELLOW));
        sessions.getOrCreateSession(player).awaitChatInput(prompt, action);
    }

    private void saveAndReopen(Player player, DetailHolder holder, JsonObject draft, String message) {
        sessions.getDraftManager().saveDraft(holder.kind(), holder.key(), draft);
        player.sendMessage(Component.text("[RPG 編輯器] " + message, NamedTextColor.GREEN));
        openDetail(player, holder.kind(), holder.key());
    }

    private static List<Field> fieldsFor(String kind) {
        List<Field> common = new ArrayList<>(List.of(
                new Field("name", "顯示名稱", ValueType.TEXT, Material.NAME_TAG, "玩家看到的名稱"),
                new Field("base_item", "Minecraft 材質", ValueType.MATERIAL, Material.GRASS_BLOCK, "如 minecraft:paper"),
                new Field("lore", "物品說明", ValueType.TEXT_LIST, Material.BOOK, "以逗號分隔多行，- 清空"),
                new Field("rarity", "稀有度", ValueType.TEXT, Material.NETHER_STAR, "稀有度代碼"),
                new Field("level", "等級", ValueType.INTEGER, Material.EXPERIENCE_BOTTLE, "需求或物品等級"),
                new Field("enabled", "是否啟用", ValueType.BOOLEAN, Material.LEVER, "未啟用的內容不會進入遊戲")
        ));
        return switch (kind) {
            case "items" -> append(common,
                    new Field("category", "物品分類", ValueType.TEXT, Material.CHEST, "material、consumable 等"),
                    new Field("max_stack_size", "堆疊上限", ValueType.INTEGER, Material.PAPER, "1 至 99"),
                    new Field("affix_pool", "詞綴池", ValueType.TEXT_LIST, Material.ENCHANTED_BOOK, "以逗號分隔詞綴代碼"));
            case "weapons" -> append(common,
                    new Field("attack_damage", "攻擊傷害", ValueType.DECIMAL, Material.IRON_SWORD, "基礎攻擊傷害"),
                    new Field("attack_speed", "攻擊速度", ValueType.DECIMAL, Material.FEATHER, "每秒攻擊速度"),
                    new Field("critical_chance", "暴擊機率", ValueType.DECIMAL, Material.FIRE_CHARGE, "0.15 代表 15%"),
                    new Field("affix_pool", "詞綴池", ValueType.TEXT_LIST, Material.ENCHANTED_BOOK, "以逗號分隔詞綴代碼"),
                    new Field("set_id", "套裝代碼", ValueType.TEXT, Material.GOLD_BLOCK, "- 可表示無套裝"));
            case "equipments" -> append(common,
                    new Field("equipment_slot", "裝備部位", ValueType.TEXT, Material.ARMOR_STAND, "head、chest、legs、feet、offhand"),
                    new Field("armor", "護甲", ValueType.DECIMAL, Material.IRON_CHESTPLATE, "基礎護甲值"),
                    new Field("armor_toughness", "護甲韌性", ValueType.DECIMAL, Material.NETHERITE_CHESTPLATE, "護甲韌性"),
                    new Field("health_bonus", "生命加成", ValueType.DECIMAL, Material.GOLDEN_APPLE, "額外生命值"),
                    new Field("affix_pool", "詞綴池", ValueType.TEXT_LIST, Material.ENCHANTED_BOOK, "以逗號分隔詞綴代碼"),
                    new Field("set_id", "套裝代碼", ValueType.TEXT, Material.GOLD_BLOCK, "所屬套裝"));
            case "gems" -> List.of(
                    new Field("name", "顯示名稱", ValueType.TEXT, Material.NAME_TAG, "寶石名稱"),
                    new Field("material", "Minecraft 材質", ValueType.MATERIAL, Material.AMETHYST_SHARD, "可從主手取得"),
                    new Field("stat", "屬性", ValueType.TEXT, Material.PAPER, "health、damage 等"),
                    new Field("value", "屬性值", ValueType.DECIMAL, Material.EMERALD, "嵌入後的加成"),
                    new Field("enabled", "是否啟用", ValueType.BOOLEAN, Material.LEVER, "點擊切換"));
            case "affixes" -> List.of(
                    new Field("name", "顯示名稱", ValueType.TEXT, Material.NAME_TAG, "詞綴名稱"),
                    new Field("stat", "屬性", ValueType.TEXT, Material.PAPER, "damage、health 等"),
                    new Field("min", "最小值", ValueType.DECIMAL, Material.REDSTONE, "隨機範圍下限"),
                    new Field("max", "最大值", ValueType.DECIMAL, Material.GLOWSTONE_DUST, "隨機範圍上限"),
                    new Field("weight", "抽選權重", ValueType.DECIMAL, Material.SCAFFOLDING, "權重越高越常出現"),
                    new Field("enabled", "是否啟用", ValueType.BOOLEAN, Material.LEVER, "點擊切換"));
            case "drop-tables" -> List.of(
                    new Field("name", "顯示名稱", ValueType.TEXT, Material.NAME_TAG, "掉落表名稱"),
                    new Field("rolls", "抽選次數", ValueType.INTEGER, Material.HOPPER, "每次觸發抽幾次"),
                    new Field("entries", "掉落項目", ValueType.DROP_ENTRIES, Material.CHEST, "代碼:權重:最小:最大，多項用逗號分隔"),
                    new Field("enabled", "是否啟用", ValueType.BOOLEAN, Material.LEVER, "點擊切換"));
            case "rarities" -> List.of(
                    new Field("name", "顯示名稱", ValueType.TEXT, Material.NAME_TAG, "例如：史詩"),
                    new Field("color", "MiniMessage 顏色", ValueType.TEXT, Material.PURPLE_DYE, "例如：<light_purple>"),
                    new Field("weight", "抽選權重", ValueType.DECIMAL, Material.SCAFFOLDING, "權重越高越常出現"),
                    new Field("enabled", "是否啟用", ValueType.BOOLEAN, Material.LEVER, "點擊切換"));
            default -> common;
        };
    }

    private static List<Field> append(List<Field> base, Field... additions) {
        List<Field> result = new ArrayList<>(base);
        result.addAll(List.of(additions));
        return result;
    }

    private static int fieldSlot(int index) { return (index / 7) * 9 + 10 + (index % 7); }
    private static int fieldIndex(int slot) {
        int row = slot / 9;
        int column = slot % 9;
        if (row < 1 || row > 4 || column < 1 || column > 7) return -1;
        return (row - 1) * 7 + (column - 1);
    }

    private static String displayValue(JsonObject draft, Field field) {
        if (!draft.has(field.key()) || draft.get(field.key()).isJsonNull()) return "未設定";
        if (draft.get(field.key()).isJsonArray()) return draft.getAsJsonArray(field.key()).toString();
        return draft.get(field.key()).getAsString();
    }

    private static List<String> previewLore(JsonObject draft) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : draft.entrySet()) {
            String value = entry.getValue().toString();
            if (value.length() > 38) value = value.substring(0, 35) + "...";
            lines.add(entry.getKey() + ": " + value);
            if (lines.size() == 12) break;
        }
        return lines;
    }

    private static String inputHelp(Field field) {
        return switch (field.type()) {
            case INTEGER -> "請輸入整數：" + field.label();
            case DECIMAL -> "請輸入數值：" + field.label();
            case TEXT_LIST -> "請以逗號分隔多個值（- 清空）：" + field.label();
            case DROP_ENTRIES -> "請輸入 代碼:權重:最小:最大，多項以逗號分隔";
            default -> "請輸入：" + field.label();
        };
    }

    private static String stringValue(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        if (!lore.isEmpty()) meta.lore(lore.stream().map(line -> Component.text(line, NamedTextColor.GRAY)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static Material iconFor(String kind) {
        return switch (kind) {
            case "weapons" -> Material.DIAMOND_SWORD;
            case "equipments" -> Material.NETHERITE_CHESTPLATE;
            case "gems" -> Material.EMERALD;
            case "affixes" -> Material.ENCHANTED_BOOK;
            case "drop-tables" -> Material.CHEST;
            case "rarities" -> Material.NETHER_STAR;
            default -> Material.PAPER;
        };
    }

    private static String kindName(String kind) {
        return switch (kind) {
            case "items" -> "物品";
            case "weapons" -> "武器";
            case "equipments" -> "裝備";
            case "gems" -> "寶石";
            case "affixes" -> "詞綴";
            case "drop-tables" -> "掉落表";
            case "rarities" -> "稀有度";
            default -> kind;
        };
    }

    private static String normalizeKey(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private static boolean validKey(String key) { return key.matches("[a-z0-9_-]{1,64}"); }
}
