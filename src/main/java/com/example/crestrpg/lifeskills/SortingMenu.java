package com.example.crestrpg.lifeskills;

import com.example.crestrpg.lifeskills.manager.PlayerToggleManager;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tw.crestnetwork.rpg.CrestRpgPlugin;

public final class SortingMenu implements Listener, CommandExecutor, TabCompleter {
    private static final String TITLE = "§8CrestRPG §0整理工具";
    private final CrestRpgPlugin plugin;

    public SortingMenu(CrestRpgPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);
        boolean inventoryEnabled = plugin.getToggleManager().isEnabled(player.getUniqueId(), PlayerToggleManager.SORT_INVENTORY);
        boolean chestEnabled = plugin.getToggleManager().isEnabled(player.getUniqueId(), PlayerToggleManager.SORT_CHEST);
        inventory.setItem(11, item(Material.CHEST, "§a立即整理背包", "§7點擊後依網站設定排序", "§7也可使用 §f/cls sort"));
        inventory.setItem(14, toggleItem(Material.BUNDLE, "背包整理", inventoryEnabled));
        inventory.setItem(16, toggleItem(Material.BARREL, "箱子整理", chestEnabled));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        switch (event.getRawSlot()) {
            case 11 -> { plugin.getSortListener().sortPlayerInventory(player); open(player); }
            case 14 -> { plugin.getToggleManager().toggle(player.getUniqueId(), PlayerToggleManager.SORT_INVENTORY); open(player); }
            case 16 -> { plugin.getToggleManager().toggle(player.getUniqueId(), PlayerToggleManager.SORT_CHEST); open(player); }
            default -> { }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此指令僅供玩家使用。");
            return true;
        }
        if (args.length == 0) { open(player); return true; }
        if (args[0].equalsIgnoreCase("sort")) return plugin.getSortListener().sortPlayerInventory(player);
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")
                && List.of(PlayerToggleManager.SORT_INVENTORY, PlayerToggleManager.SORT_CHEST).contains(args[1].toLowerCase())) {
            boolean enabled = plugin.getToggleManager().toggle(player.getUniqueId(), args[1].toLowerCase());
            player.sendMessage("§a[CrestRPG] " + args[1].toLowerCase() + " 已" + (enabled ? "啟用" : "停用") + "。");
            return true;
        }
        player.sendMessage("§e用法：/cls | /cls sort | /cls toggle <sort-inventory|sort-chest>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("sort", "toggle");
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) return List.of("sort-inventory", "sort-chest");
        return List.of();
    }

    private ItemStack toggleItem(Material material, String name, boolean enabled) {
        return item(material, (enabled ? "§a" : "§c") + name, enabled ? "§7目前：§a啟用" : "§7目前：§c停用", "§7點擊切換");
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }
}
