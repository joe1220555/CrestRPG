package tw.crestnetwork.rpg.title;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RpgTitleManager implements CommandExecutor, Listener {
    private final CrestRpgPlugin plugin;
    private final Map<String, RpgTitleDefinition> registeredTitles = new LinkedHashMap<>();
    private final Map<UUID, String> equippedTitles = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> unlockedTitles = new ConcurrentHashMap<>();

    public RpgTitleManager(CrestRpgPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Pre-register titles with stat bonuses
        registerTitle(new RpgTitleDefinition("dragon_slayer", "&c[龍之殺手]", "擊敗赤龍解鎖，物理攻擊 +15%", Map.of("physical_damage", 15.0)));
        registerTitle(new RpgTitleDefinition("archmage", "&b[大魔導士]", "精通頂級魔法，法力上限 +50", Map.of("max_mana", 50.0)));
        registerTitle(new RpgTitleDefinition("first_adventurer", "&e[初出茅廬]", "踏入 CrestRPG 的勇士", Map.of("health_bonus", 20.0)));
    }

    public void registerTitle(RpgTitleDefinition title) {
        registeredTitles.put(title.key(), title);
    }

    public void equipTitle(Player player, String titleKey) {
        if (titleKey == null) {
            equippedTitles.remove(player.getUniqueId());
            player.sendMessage(Component.text("[稱號系統] 已解除配戴稱號。", NamedTextColor.YELLOW));
            return;
        }
        RpgTitleDefinition def = registeredTitles.get(titleKey);
        if (def == null) return;

        equippedTitles.put(player.getUniqueId(), titleKey);
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a[稱號系統] 成功配戴稱號: " + def.displayName()));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    public String getEquippedTitleFormatted(Player player) {
        String key = equippedTitles.get(player.getUniqueId());
        if (key == null) return "";
        RpgTitleDefinition def = registeredTitles.get(key);
        return def != null ? def.displayName() + " " : "";
    }

    public double getTitleStatBonus(Player player, String stat) {
        String key = equippedTitles.get(player.getUniqueId());
        if (key == null) return 0.0;
        RpgTitleDefinition def = registeredTitles.get(key);
        return def != null ? def.statBonuses().getOrDefault(stat, 0.0) : 0.0;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        openTitleGui(player);
        return true;
    }

    public void openTitleGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("CrestRPG 稱號與成就系統", NamedTextColor.DARK_AQUA));
        String current = equippedTitles.get(player.getUniqueId());

        int slot = 10;
        for (RpgTitleDefinition def : registeredTitles.values()) {
            if (slot >= 17) break;
            boolean isEquipped = def.key().equals(current);
            inv.setItem(slot++, createIcon(isEquipped ? Material.GOLDEN_HELMET : Material.NAME_TAG,
                    def.displayName() + (isEquipped ? " (已裝備)" : ""),
                    List.of(def.description(), "點擊進行佩戴/切換")));
        }

        inv.setItem(22, createIcon(Material.BARRIER, "解除佩戴稱號", List.of("點擊取消當前稱號")));
        player.openInventory(inv);
    }

    private ItemStack createIcon(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        if (!loreLines.isEmpty()) {
            meta.lore(loreLines.stream().map(line -> Component.text(line, NamedTextColor.GRAY)).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.contains("CrestRPG 稱號與成就系統")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot >= 10 && slot < 10 + registeredTitles.size()) {
            List<RpgTitleDefinition> list = new ArrayList<>(registeredTitles.values());
            int idx = slot - 10;
            if (idx < list.size()) {
                equipTitle(player, list.get(idx).key());
                openTitleGui(player);
            }
        } else if (slot == 22) {
            equipTitle(player, null);
            openTitleGui(player);
        }
    }
}
