package tw.crestnetwork.rpg;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Rolls level-scaled equipment abilities once and executes passive/bound-skill effects. */
final class RolledItemAbilityService implements Listener {
    private static final Gson GSON=new Gson();
    private final CrestRpgPlugin plugin;private final NamespacedKey key;
    RolledItemAbilityService(CrestRpgPlugin plugin){this.plugin=plugin;this.key=new NamespacedKey(plugin,"rolled_abilities");Bukkit.getScheduler().runTaskTimer(plugin,this::tickPassives,20L,20L);}

    ItemStack roll(ItemStack item,String contentKey,int itemLevel){if(item==null||item.getType().isAir())return item;ItemMeta meta=item.getItemMeta();if(meta.getPersistentDataContainer().has(key,PersistentDataType.STRING))return item;JsonObject draft=plugin.getDraftManager().getDraft("weapons",contentKey);if(draft==null)draft=plugin.getDraftManager().getDraft("equipments",contentKey);JsonArray selected=new JsonArray();if(draft!=null&&draft.has("abilities")&&draft.get("abilities").isJsonArray())for(JsonElement element:draft.getAsJsonArray("abilities")){if(!element.isJsonObject())continue;JsonObject ability=element.getAsJsonObject();int min=number(ability,"min_level",1),max=number(ability,"max_level",1000);double chance=decimal(ability,"chance",1);if(itemLevel>=min&&itemLevel<=max&&ThreadLocalRandom.current().nextDouble()<=chance){JsonObject rolled=ability.deepCopy();rolled.addProperty("rolled_value",decimal(ability,"base_value",0)+itemLevel*decimal(ability,"value_per_level",0));selected.add(rolled);}}
        meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,GSON.toJson(selected));if(!selected.isEmpty()){List<Component> lore=meta.lore()==null?new ArrayList<>():new ArrayList<>(meta.lore());lore.add(Component.empty());lore.add(Component.text("隨機能力",NamedTextColor.LIGHT_PURPLE));for(JsonElement element:selected){JsonObject ability=element.getAsJsonObject();lore.add(Component.text("• "+display(ability),NamedTextColor.AQUA));}meta.lore(lore);}item.setItemMeta(meta);return item;}

    @EventHandler(ignoreCancelled=true)public void onUse(PlayerInteractEvent event){if(event.getAction()!=Action.RIGHT_CLICK_AIR&&event.getAction()!=Action.RIGHT_CLICK_BLOCK)return;for(JsonObject ability:abilities(event.getPlayer().getInventory().getItemInMainHand())){if(!"skill".equals(text(ability,"type",""))||!text(ability,"trigger","right_click").contains("right_click"))continue;String skill=text(ability,"skill","");if(!skill.isBlank())plugin.castSkill(event.getPlayer(),skill);}}
    private void tickPassives(){for(Player player:Bukkit.getOnlinePlayers()){List<ItemStack> equipped=new ArrayList<>(List.of(player.getInventory().getArmorContents()));equipped.add(player.getInventory().getItemInMainHand());equipped.add(player.getInventory().getItemInOffHand());for(ItemStack item:equipped)for(JsonObject ability:abilities(item)){if(!"potion".equals(text(ability,"type",""))||!"passive".equals(text(ability,"trigger","passive")))continue;PotionEffectType type=PotionEffectType.getByName(text(ability,"effect","speed").toUpperCase(Locale.ROOT));if(type!=null)player.addPotionEffect(new PotionEffect(type,50,Math.max(0,(int)Math.round(decimal(ability,"rolled_value",0))),false,false,true));}}}
    private List<JsonObject> abilities(ItemStack item){if(item==null||item.getType().isAir())return List.of();String raw=item.getPersistentDataContainer().get(key,PersistentDataType.STRING);if(raw==null)return List.of();try{List<JsonObject> result=new ArrayList<>();JsonParser.parseString(raw).getAsJsonArray().forEach(e->{if(e.isJsonObject())result.add(e.getAsJsonObject());});return result;}catch(Exception ignored){return List.of();}}
    private static String display(JsonObject a){if("skill".equals(text(a,"type","")))return "綁定技能："+text(a,"skill","");return text(a,"effect",text(a,"type","效果"))+" "+decimal(a,"rolled_value",0);}private static String text(JsonObject o,String k,String f){return o.has(k)?o.get(k).getAsString():f;}private static int number(JsonObject o,String k,int f){return o.has(k)?o.get(k).getAsInt():f;}private static double decimal(JsonObject o,String k,double f){return o.has(k)?o.get(k).getAsDouble():f;}
}
