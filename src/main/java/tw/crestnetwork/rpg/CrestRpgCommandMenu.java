package tw.crestnetwork.rpg;

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

import java.util.List;
import java.util.Map;

/** Clickable, categorized command browser opened by /crestrpg. */
public final class CrestRpgCommandMenu implements Listener {
    private record Holder() implements InventoryHolder { public Inventory getInventory(){return null;} }
    private static final Map<Integer,String> COMMANDS=Map.ofEntries(
            Map.entry(10,"skills"),Map.entry(11,"quests"),Map.entry(12,"rpgclass list"),Map.entry(13,"skilltree"),Map.entry(14,"rpgskill bar"),Map.entry(15,"rpgcraft"),Map.entry(16,"titles"),
            Map.entry(19,"rpgitem identify"),Map.entry(20,"rpgitem upgrade"),Map.entry(21,"rpgitem salvage"),Map.entry(22,"party gui"),Map.entry(24,"cls"),Map.entry(28,"crestrpg editor"),Map.entry(29,"crestrpg reload"),Map.entry(30,"crestrpg web"),Map.entry(31,"crestrpg items"),Map.entry(32,"crestrpg weapons"),Map.entry(33,"crestrpg equipments"),Map.entry(34,"crestrpg list"));

    public void open(Player player){Inventory inv=Bukkit.createInventory(new Holder(),54,Component.text("CrestRPG 指令選單",NamedTextColor.DARK_AQUA));
        inv.setItem(4,item(Material.COMPASS,"玩家功能",List.of("點擊下方圖示即可執行")));
        put(inv,10,Material.NETHER_STAR,"技能與屬性","/skills");put(inv,11,Material.WRITABLE_BOOK,"任務","/quests");put(inv,12,Material.ARMOR_STAND,"職業列表","/rpgclass list");put(inv,13,Material.OAK_SAPLING,"技能樹","/skilltree");put(inv,14,Material.BLAZE_POWDER,"技能快捷列","/rpgskill bar");put(inv,15,Material.CRAFTING_TABLE,"自訂製作站","/rpgcraft");put(inv,16,Material.NAME_TAG,"稱號與成就","/titles");
        put(inv,19,Material.SPYGLASS,"鑑定手上物品","/rpgitem identify");put(inv,20,Material.ANVIL,"強化手上物品","/rpgitem upgrade");put(inv,21,Material.LAVA_BUCKET,"分解手上物品","/rpgitem salvage");put(inv,22,Material.PLAYER_HEAD,"隊伍選單","/party gui");inv.setItem(23,item(Material.AMETHYST_SHARD,"鑲嵌寶石",List.of("需參數：/rpgitem socket <寶石代碼>")));put(inv,24,Material.CHEST,"背包與箱子整理","/cls");
        inv.setItem(26,item(Material.COMMAND_BLOCK,"管理員功能",List.of("需要 crestrpg.admin 權限")));
        put(inv,28,Material.CHEST,"內建圖形編輯器","/crestrpg editor");put(inv,29,Material.CLOCK,"重新載入內容","/crestrpg reload");put(inv,30,Material.MAP,"網頁編輯器狀態","/crestrpg web");put(inv,31,Material.PAPER,"物品列表","/crestrpg items");put(inv,32,Material.DIAMOND_SWORD,"武器列表","/crestrpg weapons");put(inv,33,Material.NETHERITE_CHESTPLATE,"裝備列表","/crestrpg equipments");put(inv,34,Material.ZOMBIE_HEAD,"怪物列表","/crestrpg list");
        inv.setItem(49,item(Material.BARRIER,"關閉",List.of()));inv.setItem(52,item(Material.SPAWNER,"生成怪物",List.of("需參數：/crestrpg spawn <怪物代碼> [玩家]")));inv.setItem(53,item(Material.GOLD_INGOT,"給予內容",List.of("需參數：/crestrpg give <內容代碼> [玩家]")));player.openInventory(inv);}
    private static void put(Inventory inv,int slot,Material material,String name,String command){inv.setItem(slot,item(material,name,List.of(command,"點擊執行")));}
    @EventHandler public void onClick(InventoryClickEvent event){if(!(event.getInventory().getHolder() instanceof Holder))return;event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player player))return;int slot=event.getRawSlot();if(slot==49){player.closeInventory();return;}String command=COMMANDS.get(slot);if(command!=null){player.closeInventory();player.performCommand(command);return;}if(slot==23||slot==52||slot==53){player.closeInventory();String usage=slot==23?"用法：/rpgitem socket <寶石代碼>":slot==52?"用法：/crestrpg spawn <怪物代碼> [玩家]":"用法：/crestrpg give <內容代碼> [玩家]";player.sendMessage(Component.text(usage,NamedTextColor.YELLOW));}}
    private static ItemStack item(Material material,String name,List<String> lore){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.displayName(Component.text(name,NamedTextColor.GOLD));meta.lore(lore.stream().map(line->Component.text(line,NamedTextColor.GRAY)).toList());item.setItemMeta(meta);return item;}
}
