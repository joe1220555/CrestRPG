package tw.crestnetwork.rpg.party;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tw.crestnetwork.rpg.CrestRpgPlugin;

import java.util.List;
import java.util.UUID;

public final class PartyCommand implements CommandExecutor, Listener {
    private final CrestRpgPlugin plugin;
    private final RpgPartyManager partyManager;

    public PartyCommand(CrestRpgPlugin plugin, RpgPartyManager partyManager) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以使用此指令。");
            return true;
        }

        if (args.length == 0) {
            openPartyGui(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> partyManager.createParty(player);
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("用法: /party invite <玩家名稱>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(Component.text("玩家不線上。", NamedTextColor.RED));
                    return true;
                }
                partyManager.invitePlayer(player, target);
            }
            case "accept" -> partyManager.acceptInvite(player);
            case "leave" -> partyManager.leaveParty(player);
            case "gui" -> openPartyGui(player);
            default -> {
                player.sendMessage(Component.text("=== CrestRPG 隊伍指令說明 ===", NamedTextColor.GOLD));
                player.sendMessage(Component.text("/party - 開啟隊伍 GUI 介面", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("/party invite <玩家> - 邀請玩家加入隊伍", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("/party accept - 接受隊伍邀請", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("/party leave - 離開當前隊伍", NamedTextColor.YELLOW));
            }
        }
        return true;
    }

    public void openPartyGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("CrestRPG 隊伍管理系統", NamedTextColor.DARK_AQUA));
        RpgParty party = partyManager.getParty(player);

        if (party == null) {
            inv.setItem(13, createIcon(Material.NETHER_STAR, "建立全新隊伍", List.of("點擊立即建立屬於您的冒險隊伍")));
        } else {
            int slot = 10;
            for (UUID memberUuid : party.getMembers()) {
                if (slot >= 17) break;
                Player m = Bukkit.getPlayer(memberUuid);
                String name = m != null ? m.getName() : "離線玩家";
                boolean isLeader = party.isLeader(memberUuid);
                inv.setItem(slot++, createIcon(Material.PLAYER_HEAD, (isLeader ? "👑 " : "👤 ") + name,
                        List.of(isLeader ? "隊長" : "隊員", "血量: " + (m != null ? (int) m.getHealth() : 0) + " / " + (m != null ? (int) m.getMaxHealth() : 0))));
            }
            inv.setItem(22, createIcon(Material.BARRIER, "離開隊伍", List.of("點擊退出當前隊伍")));
        }

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
        if (!title.contains("CrestRPG 隊伍管理系統")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot == 13) {
            partyManager.createParty(player);
            openPartyGui(player);
        } else if (slot == 22) {
            partyManager.leaveParty(player);
            player.closeInventory();
        }
    }
}
