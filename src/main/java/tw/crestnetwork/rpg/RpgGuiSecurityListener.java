package tw.crestnetwork.rpg;

import com.example.crestrpg.gui.QuestsGUI;
import com.example.crestrpg.gui.RPGInventoryHolder;
import com.example.crestrpg.skills.PlayerProfile;
import java.util.Set;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Locks every CrestRPG menu while allowing its registered button handlers to run. */
public final class RpgGuiSecurityListener implements Listener {
    private static final Set<String> EXACT_TITLES = Set.of(
            "CrestRPG 管理編輯器", "RPG 寶石草稿管理", "RPG 詞綴草稿管理",
            "RPG 職業草稿管理", "RPG 製作站草稿管理", "RPG 掉落表草稿管理",
            "RPG 裝備草稿管理", "RPG 物品草稿管理", "RPG 怪物草稿管理",
            "RPG NPC 草稿管理", "RPG 套裝草稿管理", "RPG 技能草稿管理",
            "RPG 技能樹草稿管理", "RPG 武器草稿管理", "CrestRPG 隊伍管理系統",
            "CrestRPG 稱號與成就系統", "CrestRPG 整理工具", "⛔ 管理員模式異常"
            , SkillBarMenu.TITLE
    );

    private final CrestRpgPlugin plugin;
    private final NamespacedKey questIdKey;

    public RpgGuiSecurityListener(CrestRpgPlugin plugin) {
        this.plugin = plugin;
        this.questIdKey = new NamespacedKey(plugin, "quest_id");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!isCrestMenu(event.getView().getTopInventory().getHolder(), title(event))) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof RPGInventoryHolder.SkillsGUIHolder) handleSkills(player, event.getRawSlot());
        else if (holder instanceof RPGInventoryHolder.QuestsMainHolder) handleQuestMain(player, event.getRawSlot());
        else if (holder instanceof RPGInventoryHolder.QuestCategoryHolder category) handleQuest(player, category, event.getCurrentItem(), event.getRawSlot());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (!isCrestMenu(event.getView().getTopInventory().getHolder(), title(event))) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) event.setCancelled(true);
    }

    private void handleSkills(Player player, int slot) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null || slot < 29 || slot > 32) return;
        if (profile.getUnusedAp() <= 0) {
            player.sendMessage("§c沒有可用的屬性點。");
            return;
        }
        switch (slot) {
            case 29 -> profile.setStrength(profile.getStrength() + 1);
            case 30 -> profile.setDexterity(profile.getDexterity() + 1);
            case 31 -> profile.setIntelligence(profile.getIntelligence() + 1);
            case 32 -> profile.setVitality(profile.getVitality() + 1);
            default -> { return; }
        }
        profile.setUnusedAp(profile.getUnusedAp() - 1);
        profile.updatePlayerAttributes(player);
        plugin.getSkillsGUI().openGUI(player);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().saveProfile(profile));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
    }

    private void handleQuestMain(Player player, int slot) {
        if (slot == 11) plugin.getQuestsGUI().openQuestCategory(player, "MAIN", QuestsGUI.TITLE_MAIN);
        else if (slot == 13) plugin.getQuestsGUI().openQuestCategory(player, "SIDE", QuestsGUI.TITLE_SIDE);
        else if (slot == 15) plugin.getQuestsGUI().openQuestCategory(player, "COMPLETED", QuestsGUI.TITLE_DONE);
    }

    private void handleQuest(Player player, RPGInventoryHolder.QuestCategoryHolder holder, ItemStack item, int slot) {
        if (slot == 49) {
            plugin.getQuestsGUI().openMainMenu(player);
            return;
        }
        if (item == null || item.getType().isAir() || !holder.getCategory().equals("SIDE")) return;
        String questId = item.getItemMeta().getPersistentDataContainer().get(questIdKey, PersistentDataType.STRING);
        if (questId != null) plugin.getQuestManager().acceptQuest(player, questId);
        plugin.getQuestsGUI().openQuestCategory(player, "SIDE", QuestsGUI.TITLE_SIDE);
    }

    private boolean isCrestMenu(InventoryHolder holder, String title) {
        if (holder instanceof RPGInventoryHolder.SkillsGUIHolder
                || holder instanceof RPGInventoryHolder.QuestsMainHolder
                || holder instanceof RPGInventoryHolder.QuestCategoryHolder) return true;
        if (holder != null && holder.getClass().getName().startsWith("tw.crestnetwork.rpg.")
                && (holder.getClass().getSimpleName().equals("StationHolder")
                || holder.getClass().getSimpleName().equals("TreeHolder"))) return true;
        return EXACT_TITLES.contains(title) || title.startsWith("技能樹 · 點數 ");
    }

    private String title(InventoryClickEvent event) {
        return PlainTextComponentSerializer.plainText().serialize(event.getView().title()).replace("§", "");
    }

    private String title(InventoryDragEvent event) {
        return PlainTextComponentSerializer.plainText().serialize(event.getView().title()).replace("§", "");
    }
}
