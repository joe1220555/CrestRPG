package com.example.crestrpg.command;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QuestsCommand implements CommandExecutor {

    private final CrestRpgPlugin plugin;

    public QuestsCommand(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此指令只能由玩家在遊戲內執行。");
            return true;
        }

        if (!player.hasPermission("crestrpg.use")) {
            player.sendMessage("§c您沒有權限使用此指令。");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§7======= §a📜 CrestRPG 任務與冒險系統 §7=======");
            player.sendMessage("§a/quests §7- 開啟主線與支線任務 GUI 進度面板");
            player.sendMessage("§7- 支援多種任務目標：擊殺怪物 (包含自訂怪)、收集物品、NPC對話。");
            player.sendMessage("§7- 靠近頭頂有對應名稱的村民 NPC 右鍵互動，即可接取或完成對話任務。");
            player.sendMessage("§7- 管理員指令請使用：§f/crestrpg help");
            return true;
        }

        plugin.getQuestsGUI().openMainMenu(player);
        return true;
    }
}
