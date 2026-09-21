package tw.crestnetwork.rpg.editor;

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

public final class RpgEditorMenu implements Listener {
    private final CrestRpgPlugin plugin;
    private final EditorSessionManager sessionManager;

    public RpgEditorMenu(CrestRpgPlugin plugin, EditorSessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("CrestRPG 管理編輯器", NamedTextColor.DARK_AQUA));

        inv.setItem(10, createIcon(Material.PAPER, "物品 (Items)", List.of("編輯一般基礎物品、材料", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("items").size())));
        inv.setItem(11, createIcon(Material.DIAMOND_SWORD, "武器 (Weapons)", List.of("編輯 RPG 攻擊武器與工具能力", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("weapons").size())));
        inv.setItem(12, createIcon(Material.NETHERITE_CHESTPLATE, "裝備 (Equipments)", List.of("編輯頭盔、胸甲、護腿、靴子、副手", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("equipments").size())));
        inv.setItem(13, createIcon(Material.EMERALD, "寶石 (Gems)", List.of("編輯寶石與插槽屬性", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("gems").size())));
        inv.setItem(14, createIcon(Material.ENCHANTED_BOOK, "詞綴 (Affixes)", List.of("編輯隨機詞綴池與屬性範圍", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("affixes").size())));
        inv.setItem(15, createIcon(Material.GOLD_BLOCK, "套裝 (Sets)", List.of("編輯裝備套裝件數效果", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("sets").size())));
        inv.setItem(16, createIcon(Material.NETHER_STAR, "稀有度 (Rarities)", List.of("編輯名稱、顏色與抽選權重", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("rarities").size())));

        inv.setItem(19, createIcon(Material.ARMOR_STAND, "職業 (Classes)", List.of("編輯 RPG 職業與基礎屬性", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("classes").size())));
        inv.setItem(20, createIcon(Material.BLAZE_POWDER, "技能 (Skills)", List.of("編輯觸發器、條件與效果", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("skills").size())));
        inv.setItem(21, createIcon(Material.OAK_SAPLING, "技能樹 (Skill Trees)", List.of("編輯技能點、解鎖前置與 GUI 佈局", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("skill-trees").size())));
        inv.setItem(22, createIcon(Material.ZOMBIE_HEAD, "怪物 (Monsters)", List.of("編輯 RPG 自訂怪物與血量傷害", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("monsters").size())));
        inv.setItem(23, createIcon(Material.CHEST, "掉落表 (Drop Tables)", List.of("編輯怪物加權抽選掉落表", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("drop-tables").size())));
        inv.setItem(24, createIcon(Material.CRAFTING_TABLE, "製作站 (Crafting Stations)", List.of("編輯自訂配方與製作站 GUI", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("crafting-stations").size())));
        inv.setItem(25, createIcon(Material.PLAYER_HEAD, "NPC 實體 (NPCs)", List.of("編輯全息NPC、座標與對話互動", "當前草稿數量：" + sessionManager.getDraftManager().getDrafts("npcs").size())));

        inv.setItem(40, createIcon(Material.COMPASS, "進行草稿全量驗證", List.of("點擊驗證所有本機草稿關聯與格式")));
        inv.setItem(49, createIcon(Material.BARRIER, "關閉編輯器", List.of()));

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
        if (!title.contains("CrestRPG 管理編輯器")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        switch (slot) {
            case 10 -> new ItemEditorMenu(plugin, sessionManager).open(player);
            case 11 -> new WeaponEditorMenu(plugin, sessionManager).open(player);
            case 12 -> new EquipmentEditorMenu(plugin, sessionManager).open(player);
            case 13 -> new GemEditorMenu(plugin, sessionManager).open(player);
            case 14 -> new AffixEditorMenu(plugin, sessionManager).open(player);
            case 15 -> new SetEditorMenu(plugin, sessionManager).open(player);
            case 16 -> new UniversalDraftEditorMenu(plugin, sessionManager).openList(player, "rarities");
            case 19 -> new ClassEditorMenu(plugin, sessionManager).open(player);
            case 20 -> new SkillEditorMenu(plugin, sessionManager).open(player);
            case 21 -> new SkillTreeEditorMenu(plugin, sessionManager).open(player);
            case 22 -> new MonsterEditorMenu(plugin, sessionManager).open(player);
            case 23 -> new DropTableEditorMenu(plugin, sessionManager).open(player);
            case 24 -> new CraftingStationEditorMenu(plugin, sessionManager).open(player);
            case 25 -> new NpcEditorMenu(plugin, sessionManager).open(player);
            case 40 -> {
                RpgDraftValidator.ValidationResult result = RpgDraftValidator.validate(sessionManager.getDraftManager());
                if (result.isValid()) {
                    player.sendMessage(Component.text("[RPG 編輯器] 驗證成功！所有草稿資料格式與關聯皆完全正確。", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("[RPG 編輯器] 驗證失敗，發現 " + result.getErrors().size() + " 個錯誤：", NamedTextColor.RED));
                    for (String err : result.getErrors()) {
                        player.sendMessage(Component.text(" - " + err, NamedTextColor.RED));
                    }
                }
            }
            case 49 -> player.closeInventory();
        }
    }
}
