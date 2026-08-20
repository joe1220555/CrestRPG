package tw.crestnetwork.rpg;

import com.example.crestrpg.skills.PlayerProfile;
import java.util.ArrayList;
import java.util.List;
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

final class SkillBarMenu implements Listener {
    static final String TITLE = "CrestRPG 技能快捷列";
    private final CrestRpgPlugin plugin;
    SkillBarMenu(CrestRpgPlugin plugin) { this.plugin = plugin; }

    void open(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId()); if (profile == null) return;
        Inventory inventory = Bukkit.createInventory(new Holder(), 27, Component.text(TITLE, NamedTextColor.DARK_PURPLE));
        for (int barSlot = 1; barSlot <= 9; barSlot++) {
            String skillKey = profile.getSkillBar().get(barSlot); int inventorySlot = 8 + barSlot;
            if (skillKey == null) inventory.setItem(inventorySlot, icon(Material.GRAY_STAINED_GLASS_PANE, "空白技能槽 " + barSlot, List.of("使用 /rpgskill bind " + barSlot + " <技能>")));
            else {
                RpgEngineRegistry.SkillDefinition skill = plugin.engineSnapshot().skills().get(skillKey);
                String name = skill == null ? skillKey : skill.name(); int level = profile.getClassSkillLevel(skillKey);
                inventory.setItem(inventorySlot, icon(Material.ENCHANTED_BOOK, name, List.of("技能等級：" + level, "經驗：" + Math.round(profile.getClassSkillXp(skillKey)) + "/" + Math.round(profile.getClassSkillXpRequired(skillKey)), "左鍵：施放", "右鍵：解除綁定")));
            }
        }
        player.openInventory(inventory);
    }

    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return; event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 9 || event.getRawSlot() > 17) return;
        int slot = event.getRawSlot() - 8; PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId()); if (profile == null) return;
        String skill = profile.getSkillBar().get(slot); if (skill == null) return;
        if (event.isRightClick()) { profile.unbindSkill(slot); plugin.saveProfileAsync(profile); open(player); }
        else { player.closeInventory(); if (!plugin.castSkill(player, skill)) player.sendMessage(Component.text("技能目前無法施放。", NamedTextColor.RED)); }
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.displayName(Component.text(name, NamedTextColor.GOLD));
        List<Component> lines = new ArrayList<>(); lore.forEach(line -> lines.add(Component.text(line, NamedTextColor.GRAY))); meta.lore(lines); item.setItemMeta(meta); return item;
    }
    static final class Holder implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
}
