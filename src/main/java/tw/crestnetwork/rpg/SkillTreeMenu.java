package tw.crestnetwork.rpg;

import com.example.crestrpg.skills.PlayerProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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

final class SkillTreeMenu implements Listener {
    private final CrestRpgPlugin plugin;
    private final Supplier<RpgEngineRegistry> registry;

    SkillTreeMenu(CrestRpgPlugin plugin, Supplier<RpgEngineRegistry> registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    void open(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return;
        RpgEngineRegistry.TreeDefinition tree = registry.get().skillTrees().values().stream()
                .filter(value -> value.classId() == null || value.classId().equals(profile.getClassId())).findFirst().orElse(null);
        if (tree == null) {
            player.sendMessage(Component.text("目前職業沒有已發布的技能樹。", NamedTextColor.RED));
            return;
        }
        TreeHolder holder = new TreeHolder(tree);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text("技能樹 · 點數 " + profile.getSkillPoints(), NamedTextColor.DARK_PURPLE));
        holder.inventory = inventory;
        for (RpgEngineRegistry.TreeNode node : tree.nodes()) {
            holder.nodes.put(node.slot(), node);
            inventory.setItem(node.slot(), icon(tree, node, profile));
        }
        player.openInventory(inventory);
    }

    private ItemStack icon(RpgEngineRegistry.TreeDefinition tree, RpgEngineRegistry.TreeNode node, PlayerProfile profile) {
        String persistentKey = tree.key() + ":" + node.key();
        boolean unlocked = profile.getUnlockedSkillNodes().contains(persistentKey);
        boolean requirements = requirementsMet(tree, node, profile);
        ItemStack item = new ItemStack(unlocked ? Material.LIME_DYE : requirements ? Material.YELLOW_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        RpgEngineRegistry.SkillDefinition skill = registry.get().skills().get(node.skill());
        meta.displayName(Component.text(skill == null ? node.skill() : skill.name(), unlocked ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("技能點花費：" + node.cost(), NamedTextColor.AQUA));
        lore.add(Component.text("角色等級：" + node.requiredLevel(), NamedTextColor.GRAY));
        if (!node.requires().isEmpty()) lore.add(Component.text("前置節點：" + String.join("、", node.requires()), NamedTextColor.GRAY));
        lore.add(Component.text(unlocked ? "已解鎖" : requirements ? "點擊解鎖" : "尚未符合條件",
                unlocked ? NamedTextColor.GREEN : requirements ? NamedTextColor.YELLOW : NamedTextColor.RED));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TreeHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        RpgEngineRegistry.TreeNode node = holder.nodes.get(event.getRawSlot());
        if (node == null) return;
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return;
        String key = holder.tree.key() + ":" + node.key();
        if (profile.getUnlockedSkillNodes().contains(key)) {
            player.sendMessage(Component.text("此技能已解鎖。", NamedTextColor.RED));
            return;
        }
        if (!requirementsMet(holder.tree, node, profile)) {
            player.sendMessage(Component.text("等級或前置技能尚未符合。", NamedTextColor.RED));
            return;
        }
        if (!profile.unlockSkillNode(key, node.cost())) {
            player.sendMessage(Component.text("技能點不足。", NamedTextColor.RED));
            return;
        }
        if (!profile.getSkillBar().containsValue(node.skill())) {
            for (int slot = 1; slot <= 9; slot++) if (!profile.getSkillBar().containsKey(slot)) { profile.bindSkill(slot, node.skill()); break; }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().saveProfile(profile));
        player.sendMessage(Component.text("已解鎖技能節點。", NamedTextColor.GREEN));
        open(player);
    }

    private boolean requirementsMet(RpgEngineRegistry.TreeDefinition tree, RpgEngineRegistry.TreeNode node, PlayerProfile profile) {
        if (profile.getCharacterLevel() < node.requiredLevel() || profile.getSkillPoints() < node.cost()) return false;
        return node.requires().stream().allMatch(required -> profile.getUnlockedSkillNodes().contains(tree.key() + ":" + required));
    }

    private static final class TreeHolder implements InventoryHolder {
        private final RpgEngineRegistry.TreeDefinition tree;
        private final Map<Integer, RpgEngineRegistry.TreeNode> nodes = new LinkedHashMap<>();
        private Inventory inventory;
        private TreeHolder(RpgEngineRegistry.TreeDefinition tree) { this.tree = tree; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
