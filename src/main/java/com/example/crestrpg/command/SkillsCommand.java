package com.example.crestrpg.command;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkillsCommand implements CommandExecutor {

    private final CrestRpgPlugin plugin;

    public SkillsCommand(CrestRpgPlugin plugin) {
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
            player.sendMessage("§7======= §e⚔ CrestRPG 技能與屬性系統 §7=======");
            player.sendMessage("§e/skills §7- 開啟角色技能狀態與屬性點配點 GUI 介面");
            player.sendMessage("§7- 升級任何技能，即可獲得 2 點自由屬性點 (AP)。");
            player.sendMessage("§7- 可分配屬性：力量 (💪 增傷)、敏捷 (⚡ 加速與閃避)、智慧 (🧠 加法力與法傷)、體質 (❤️ 增血)。");
            player.sendMessage("§7- 管理員指令請使用：§f/crestrpg help");
            return true;
        }

        plugin.getSkillsGUI().openGUI(player);
        return true;
    }
}
