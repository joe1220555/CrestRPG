package com.example.crestrpg.equipment;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import com.example.crestrpg.skills.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class EquipmentListener implements Listener {

    private final CrestRpgPlugin plugin;

    public EquipmentListener(CrestRpgPlugin plugin) {
        this.plugin = plugin;
    }

    private void updateDelayed(Player player) {
        if (player == null || !player.isOnline()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
            if (profile != null) {
                profile.updatePlayerAttributes(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldChange(PlayerItemHeldEvent event) {
        updateDelayed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryType.SlotType slotType = event.getSlotType();
        if (slotType == InventoryType.SlotType.ARMOR ||
            slotType == InventoryType.SlotType.QUICKBAR ||
            event.isShiftClick()) {
            updateDelayed(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            updateDelayed(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        updateDelayed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        updateDelayed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            updateDelayed(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMeleeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        CustomItem customItem = plugin.getEquipmentManager().getCustomItem(mainHand);
        if (customItem == null) return;

        if (!plugin.getEquipmentManager().meetsRequirements(attacker, customItem)) {
            event.setCancelled(true);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            attacker.sendActionBar("§c⚠️ 您的技能等級不足，無法發揮此武器的力量！");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;

        ItemStack bow = event.getBow();
        CustomItem customItem = plugin.getEquipmentManager().getCustomItem(bow);
        if (customItem == null) return;

        if (!plugin.getEquipmentManager().meetsRequirements(shooter, customItem)) {
            event.setCancelled(true);
            shooter.playSound(shooter.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            shooter.sendActionBar("§c⚠️ 您的技能等級不足，無法使用此弓！");
        }
    }
}
