package tw.crestnetwork.rpg.editor;

import com.google.gson.JsonArray;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Chest-style 3x3 recipe editor for crafting station drafts. */
public final class CraftingStationEditorMenu implements Listener {
    private static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private record StationListHolder(List<String> keys) implements InventoryHolder { public Inventory getInventory(){return null;} }
    private record RecipeListHolder(String station) implements InventoryHolder { public Inventory getInventory(){return null;} }
    private record RecipeHolder(String station,int recipe) implements InventoryHolder { public Inventory getInventory(){return null;} }
    private final CrestRpgPlugin plugin;
    private final EditorSessionManager sessions;

    public CraftingStationEditorMenu(CrestRpgPlugin plugin,EditorSessionManager sessions){this.plugin=plugin;this.sessions=sessions;}

    public void open(Player player){
        Map<String,JsonObject> drafts=sessions.getDraftManager().getDrafts("crafting-stations");
        List<String> keys=new ArrayList<>(drafts.keySet());
        Inventory inv=Bukkit.createInventory(new StationListHolder(keys),54,Component.text("RPG 製作站草稿管理",NamedTextColor.DARK_AQUA));
        for(int i=0;i<keys.size()&&i<45;i++){JsonObject value=drafts.get(keys.get(i));inv.setItem(i,icon(Material.CRAFTING_TABLE,text(value,"name",keys.get(i)),List.of("點擊編輯工作台配方",keys.get(i))));}
        inv.setItem(48,icon(Material.NETHER_STAR,"新增製作站",List.of()));inv.setItem(49,icon(Material.ARROW,"返回主選單",List.of()));player.openInventory(inv);
    }

    private void openRecipes(Player player,String station){
        JsonObject draft=draft(station);if(draft==null){open(player);return;}JsonArray recipes=array(draft,"recipes");
        Inventory inv=Bukkit.createInventory(new RecipeListHolder(station),54,Component.text("配方 · "+station,NamedTextColor.DARK_AQUA));
        for(int i=0;i<recipes.size()&&i<45;i++){JsonObject recipe=recipes.get(i).getAsJsonObject();Material mat=Material.matchMaterial(text(recipe,"output","minecraft:stone"));inv.setItem(i,icon(mat!=null&&mat.isItem()?mat:Material.CRAFTING_TABLE,text(recipe,"key","recipe_"+(i+1)),List.of("產物："+text(recipe,"output","minecraft:stone"),"點擊開啟 3×3 工作台編輯")));}
        inv.setItem(48,icon(Material.LIME_DYE,"新增配方",List.of()));inv.setItem(49,icon(Material.ARROW,"返回製作站",List.of()));player.openInventory(inv);
    }

    private void openRecipe(Player player,String station,int index){
        JsonObject draft=draft(station);if(draft==null)return;JsonArray recipes=array(draft,"recipes");if(index<0||index>=recipes.size()){openRecipes(player,station);return;}JsonObject recipe=recipes.get(index).getAsJsonObject();
        Inventory inv=Bukkit.createInventory(new RecipeHolder(station,index),54,Component.text("工作台 · "+text(recipe,"key","recipe"),NamedTextColor.DARK_AQUA));JsonArray ingredients=array(recipe,"ingredients");
        for(int grid=0;grid<9;grid++){JsonObject ingredient=findIngredient(ingredients,grid);if(ingredient==null){inv.setItem(GRID[grid],icon(Material.GRAY_STAINED_GLASS_PANE,"空材料格",List.of("左鍵：選擇原版材料")));continue;}Material material=Material.matchMaterial(text(ingredient,"key","minecraft:stone"));int amount=ingredient.has("amount")?ingredient.get("amount").getAsInt():1;inv.setItem(GRID[grid],icon(material!=null&&material.isItem()?material:Material.PAPER,text(ingredient,"key",""),List.of("數量："+amount,"左鍵：更換　Shift+左鍵：增加數量","右鍵：清除這格")));}
        inv.setItem(23,icon(Material.ARROW,"製作結果",List.of()));String output=text(recipe,"output","minecraft:stone");Material out=Material.matchMaterial(output);int amount=recipe.has("amount")?recipe.get("amount").getAsInt():1;inv.setItem(25,icon(out!=null&&out.isItem()?out:Material.PAPER,output,List.of("產出數量："+amount,"左鍵：更換　Shift+左鍵：增加","右鍵：減少")));inv.setItem(49,icon(Material.ARROW,"儲存並返回",List.of()));inv.setItem(53,icon(Material.RED_DYE,"刪除配方",List.of()));player.openInventory(inv);
    }

    @EventHandler public void onClick(InventoryClickEvent event){
        if(!(event.getWhoClicked() instanceof Player player))return;InventoryHolder raw=event.getInventory().getHolder();
        if(raw instanceof StationListHolder holder){event.setCancelled(true);int slot=event.getRawSlot();if(slot==49)new RpgEditorMenu(plugin,sessions).open(player);else if(slot==48)createStation(player);else if(slot>=0&&slot<holder.keys().size())openRecipes(player,holder.keys().get(slot));return;}
        if(raw instanceof RecipeListHolder holder){event.setCancelled(true);int slot=event.getRawSlot();if(slot==49)open(player);else if(slot==48)addRecipe(player,holder.station());else if(slot>=0&&slot<45)openRecipe(player,holder.station(),slot);return;}
        if(!(raw instanceof RecipeHolder holder))return;event.setCancelled(true);JsonObject draft=draft(holder.station());if(draft==null)return;JsonArray recipes=array(draft,"recipes");if(holder.recipe()>=recipes.size())return;JsonObject recipe=recipes.get(holder.recipe()).getAsJsonObject();int slot=event.getRawSlot();
        if(slot==49){save(holder.station(),draft);openRecipes(player,holder.station());return;}if(slot==53){recipes.remove(holder.recipe());save(holder.station(),draft);openRecipes(player,holder.station());return;}
        if(slot==25){if(event.isShiftClick()){recipe.addProperty("amount",Math.min(99,(recipe.has("amount")?recipe.get("amount").getAsInt():1)+1));save(holder.station(),draft);openRecipe(player,holder.station(),holder.recipe());}else if(event.isRightClick()){recipe.addProperty("amount",Math.max(1,(recipe.has("amount")?recipe.get("amount").getAsInt():1)-1));save(holder.station(),draft);openRecipe(player,holder.station(),holder.recipe());}else chooseMaterial(player,material->{recipe.addProperty("output",material.getKey().toString());save(holder.station(),draft);openRecipe(player,holder.station(),holder.recipe());},()->openRecipe(player,holder.station(),holder.recipe()));return;}
        for(int grid=0;grid<9;grid++)if(slot==GRID[grid]){JsonArray ingredients=array(recipe,"ingredients");JsonObject ingredient=findIngredient(ingredients,grid);if(event.isRightClick()){if(ingredient!=null)ingredients.remove(ingredient);save(holder.station(),draft);openRecipe(player,holder.station(),holder.recipe());}else if(event.isShiftClick()&&ingredient!=null){ingredient.addProperty("amount",Math.min(999,(ingredient.has("amount")?ingredient.get("amount").getAsInt():1)+1));save(holder.station(),draft);openRecipe(player,holder.station(),holder.recipe());}else{int selectedGrid=grid;chooseMaterial(player,material->{JsonObject current=findIngredient(ingredients,selectedGrid);if(current==null){current=new JsonObject();current.addProperty("slot",selectedGrid);current.addProperty("amount",1);ingredients.add(current);}current.addProperty("key",material.getKey().toString());current.addProperty("content",false);save(holder.station(),draft);openRecipe(player,holder.station(),holder.recipe());},()->openRecipe(player,holder.station(),holder.recipe()));}return;}
    }

    private void chooseMaterial(Player player,java.util.function.Consumer<Material> selected,Runnable cancelled){new VanillaMaterialPickerMenu(plugin,sessions).open(player,selected,cancelled);}
    private void createStation(Player player){player.closeInventory();sessions.getOrCreateSession(player).awaitChatInput("新製作站代碼",input->{String key=input.toLowerCase().trim();JsonObject value=new JsonObject();value.addProperty("key",key);value.addProperty("name",key);value.add("recipes",new JsonArray());value.addProperty("enabled",true);save(key,value);openRecipes(player,key);});player.sendMessage(Component.text("[RPG 編輯器] 請在聊天欄輸入新製作站代碼。",NamedTextColor.YELLOW));}
    private void addRecipe(Player player,String station){JsonObject draft=draft(station);if(draft==null)return;JsonArray recipes=array(draft,"recipes");JsonObject recipe=new JsonObject();recipe.addProperty("key","recipe_"+(recipes.size()+1));recipe.addProperty("output","minecraft:stone");recipe.addProperty("amount",1);recipe.addProperty("required_level",1);recipe.addProperty("permission","");recipe.add("ingredients",new JsonArray());recipes.add(recipe);save(station,draft);openRecipe(player,station,recipes.size()-1);}
    private JsonObject draft(String key){return sessions.getDraftManager().getDraft("crafting-stations",key);}private void save(String key,JsonObject value){sessions.getDraftManager().saveDraft("crafting-stations",key,value);}private static JsonArray array(JsonObject value,String key){if(!value.has(key)||!value.get(key).isJsonArray())value.add(key,new JsonArray());return value.getAsJsonArray(key);}private static JsonObject findIngredient(JsonArray values,int slot){for(int i=0;i<values.size();i++){if(!values.get(i).isJsonObject())continue;JsonObject value=values.get(i).getAsJsonObject();int current=value.has("slot")?value.get("slot").getAsInt():i;if(current==slot)return value;}return null;}private static String text(JsonObject value,String key,String fallback){return value.has(key)?value.get(key).getAsString():fallback;}
    private static ItemStack icon(Material material,String name,List<String> lore){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.displayName(Component.text(name,NamedTextColor.GOLD));if(!lore.isEmpty())meta.lore(lore.stream().map(line->Component.text(line,NamedTextColor.GRAY)).toList());item.setItemMeta(meta);return item;}
}
