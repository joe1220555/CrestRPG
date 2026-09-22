package tw.crestnetwork.rpg;

import com.example.crestrpg.skills.PlayerProfile;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Player-facing RPG hub, including mandatory first class selection. */
final class PlayerCenterMenu implements Listener {
    private record CenterHolder() implements InventoryHolder { public Inventory getInventory(){return null;} }
    private record ClassHolder(List<String> keys,boolean required) implements InventoryHolder { public Inventory getInventory(){return null;} }
    private record ContentHolder() implements InventoryHolder { public Inventory getInventory(){return null;} }
    private final CrestRpgPlugin plugin;
    PlayerCenterMenu(CrestRpgPlugin plugin){this.plugin=plugin;}

    void open(Player player){PlayerProfile profile=plugin.getProfileManager().getProfile(player.getUniqueId());if(profile==null)return;if(!profile.isClassSelected()&&!classes().isEmpty()){openClasses(player,true);return;}Inventory inv=Bukkit.createInventory(new CenterHolder(),54,Component.text("CrestRPG 玩家中心",NamedTextColor.DARK_AQUA));ItemStack head=new ItemStack(Material.PLAYER_HEAD);SkullMeta meta=(SkullMeta)head.getItemMeta();meta.setOwningPlayer(player);meta.displayName(Component.text(player.getName(),NamedTextColor.GOLD));meta.lore(List.of(Component.text("角色等級："+profile.getCharacterLevel(),NamedTextColor.GRAY),Component.text("目前職業："+className(profile.getClassId()),NamedTextColor.AQUA)));head.setItemMeta(meta);inv.setItem(13,head);inv.setItem(20,item(Material.ARMOR_STAND,"轉換職業",List.of("查看可選職業並進行轉換")));inv.setItem(22,item(Material.NETHER_STAR,"技能與屬性",List.of("查看屬性與分配 AP")));inv.setItem(24,item(Material.CHEST,"可使用的內容",List.of("查看等級符合的武器、裝備與物品")));inv.setItem(31,item(Material.BLAZE_POWDER,"技能快捷列",List.of()));inv.setItem(32,item(Material.OAK_SAPLING,"職業技能樹",List.of()));inv.setItem(49,item(Material.BARRIER,"關閉",List.of()));player.openInventory(inv);}

    void openClasses(Player player,boolean required){Map<String,String> classes=classes();List<String> keys=new ArrayList<>(classes.keySet());Inventory inv=Bukkit.createInventory(new ClassHolder(keys,required),54,Component.text(required?"請先選擇職業":"轉換職業",NamedTextColor.DARK_AQUA));for(int i=0;i<keys.size()&&i<45;i++){String key=keys.get(i);inv.setItem(i,item(classIcon(i),classes.get(key),List.of("代碼："+key,required?"必須選擇後才能進入玩家中心":"點擊轉換為此職業")));}if(!required)inv.setItem(49,item(Material.ARROW,"返回玩家中心",List.of()));player.openInventory(inv);}

    private void openContent(Player player){int level=plugin.playerLevel(player);Inventory inv=Bukkit.createInventory(new ContentHolder(),54,Component.text("可使用的 RPG 內容",NamedTextColor.DARK_AQUA));int slot=0;for(RpgWeaponDefinition value:plugin.weaponDefinitions().values())if(value.enabled()&&value.level()<=level&&slot<45)inv.setItem(slot++,item(Material.DIAMOND_SWORD,value.name(),List.of("武器 · 需求等級 "+value.level())));for(RpgEquipmentDefinition value:plugin.equipmentDefinitions().values())if(value.enabled()&&value.level()<=level&&slot<45)inv.setItem(slot++,item(Material.IRON_CHESTPLATE,value.name(),List.of("裝備 · 需求等級 "+value.level())));for(RpgItemDefinition value:plugin.itemDefinitions().values())if(value.enabled()&&value.level()<=level&&slot<45)inv.setItem(slot++,item(Material.PAPER,value.name(),List.of("物品 · 需求等級 "+value.level())));if(slot==0)inv.setItem(22,item(Material.BARRIER,"目前沒有可使用內容",List.of("提升角色等級或請管理員發布內容")));inv.setItem(49,item(Material.ARROW,"返回",List.of()));player.openInventory(inv);}

    @EventHandler public void onClick(InventoryClickEvent event){if(!(event.getWhoClicked() instanceof Player player))return;InventoryHolder holder=event.getInventory().getHolder();if(holder instanceof CenterHolder){event.setCancelled(true);switch(event.getRawSlot()){case 20->openClasses(player,false);case 22->plugin.getSkillsGUI().openStatsGUI(player);case 24->openContent(player);case 31->player.performCommand("rpgskill bar");case 32->player.performCommand("skilltree");case 49->player.closeInventory();}return;}if(holder instanceof ClassHolder classes){event.setCancelled(true);int slot=event.getRawSlot();if(slot==49&&!classes.required()){open(player);return;}if(slot<0||slot>=classes.keys().size())return;PlayerProfile profile=plugin.getProfileManager().getProfile(player.getUniqueId());if(profile==null)return;String key=classes.keys().get(slot);profile.setClassId(key);profile.setClassSelected(true);RpgEngineRegistry.ClassDefinition selected=plugin.registry().classes().get(key);if(selected!=null)profile.getSkillBar().entrySet().removeIf(entry->!selected.skills().contains(entry.getValue()));plugin.saveProfileAsync(profile);player.sendMessage(Component.text("已選擇職業："+className(key),NamedTextColor.GREEN));open(player);return;}if(holder instanceof ContentHolder){event.setCancelled(true);if(event.getRawSlot()==49)open(player);}}

    private Map<String,String> classes(){Map<String,String> result=new LinkedHashMap<>();plugin.registry().classes().forEach((key,value)->result.put(key,value.name()));for(Map.Entry<String,JsonObject> entry:plugin.getDraftManager().getDrafts("classes").entrySet())result.putIfAbsent(entry.getKey(),entry.getValue().has("name")?entry.getValue().get("name").getAsString():entry.getKey());return result;}
    private String className(String key){return classes().getOrDefault(key,key);}private static Material classIcon(int index){Material[] icons={Material.IRON_SWORD,Material.BOW,Material.BLAZE_ROD,Material.SHIELD,Material.GOLDEN_AXE,Material.BOOK};return icons[index%icons.length];}
    private static ItemStack item(Material material,String name,List<String> lore){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.displayName(Component.text(name,NamedTextColor.GOLD));meta.lore(lore.stream().map(line->Component.text(line,NamedTextColor.GRAY)).toList());item.setItemMeta(meta);return item;}
}
