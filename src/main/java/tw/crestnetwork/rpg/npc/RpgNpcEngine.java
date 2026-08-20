package tw.crestnetwork.rpg.npc;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.Material;
import tw.crestnetwork.rpg.CrestRpgPlugin;
import tw.crestnetwork.rpg.editor.RpgDraftManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RpgNpcEngine implements Listener {
    private final CrestRpgPlugin plugin;
    private final Map<String, PacketNpcInstance> npcs = new ConcurrentHashMap<>();

    public RpgNpcEngine(CrestRpgPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickEngine, 20L, 20L);
    }

    public void loadNpcsFromDrafts(RpgDraftManager draftManager) {
        clearAll();
        Map<String, JsonObject> drafts = draftManager.getDrafts("npcs");
        for (Map.Entry<String, JsonObject> entry : drafts.entrySet()) {
            try {
                RpgNpcDefinition def = RpgNpcDefinition.fromJson(entry.getKey(), entry.getValue());
                if (def.enabled()) {
                    PacketNpcInstance instance = new PacketNpcInstance(plugin, def);
                    instance.spawnHolograms();
                    npcs.put(def.id(), instance);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("無法載入 NPC 草稿: " + entry.getKey() + " - " + e.getMessage());
            }
        }
        plugin.getLogger().info("已載入 " + npcs.size() + " 個原生 NPC 實體。");
    }

    public Collection<PacketNpcInstance> getNpcs() {
        return npcs.values();
    }

    public PacketNpcInstance getNpc(String id) {
        return npcs.get(id);
    }

    public void clearAll() {
        for (PacketNpcInstance instance : npcs.values()) {
            instance.destroy();
        }
        npcs.clear();
    }

    private void tickEngine() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (PacketNpcInstance npc : npcs.values()) {
                if (npc.isNearby(player)) {
                    if (!npc.isVisibleTo(player)) {
                        npc.showTo(player);
                    }
                    if (npc.isInGreetingRange(player)) {
                        triggerProactiveGreeting(player, npc);
                    }
                } else {
                    if (npc.isVisibleTo(player)) {
                        npc.hideFrom(player);
                    }
                }
            }
        }
    }

    private void triggerProactiveGreeting(Player player, PacketNpcInstance npc) {
        if (!npc.tryGreet(player)) return;
        RpgNpcDefinition def = npc.getDefinition();

        // Play Sound
        try {
            String soundKey = def.greetingSound() != null ? def.greetingSound() : "entity.villager.yes";
            player.playSound(npc.getLocation(), soundKey, 1.0f, 1.1f);
        } catch (Exception ignored) {
            player.playSound(npc.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.1f);
        }

        // Spawn Visual Particles
        try {
            org.bukkit.Location loc = npc.getLocation().add(0, 1.8, 0);
            if ("HEART_PARTICLE".equalsIgnoreCase(def.greetingAction())) {
                player.spawnParticle(org.bukkit.Particle.HEART, loc, 3, 0.2, 0.2, 0.2, 0.05);
            } else if ("NOTE".equalsIgnoreCase(def.greetingAction())) {
                player.spawnParticle(org.bukkit.Particle.NOTE, loc, 4, 0.2, 0.2, 0.2, 0.05);
            } else {
                player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, loc, 5, 0.3, 0.3, 0.3, 0.05);
            }
        } catch (Exception ignored) {}

        // Send Proactive Greeting Message in Chat
        String greetingText = def.greetingText() != null ? def.greetingText() : "您好，冒險者！請問有什麼需要的嗎？";
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(def.displayName() + " &f: &e" + greetingText));
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked());
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked());
    }

    private void handleInteract(Player player, Entity clicked) {
        if (!(clicked instanceof ArmorStand stand)) return;

        for (PacketNpcInstance npc : npcs.values()) {
            if (npc.isNearby(player) && npc.getLocation().distanceSquared(stand.getLocation()) <= 16) {
                triggerNpcInteraction(player, npc);
                break;
            }
        }
    }

    public void triggerNpcInteraction(Player player, PacketNpcInstance npc) {
        RpgNpcDefinition def = npc.getDefinition();
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.1f);

        // Display Dialogues
        if (!def.dialogues().isEmpty()) {
            player.sendMessage(Component.text("----------------------------------------", NamedTextColor.DARK_GRAY));
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(def.displayName() + " &f:"));
            for (String line : def.dialogues()) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("  &7" + line));
            }
            player.sendMessage(Component.text("----------------------------------------", NamedTextColor.DARK_GRAY));
        }

        // Trigger Interaction Action
        switch (def.interactionType().toUpperCase()) {
            case "CRAFTING_STATION" -> player.sendMessage(Component.text("[NPC] 正在開啟合成台: " + def.targetId(), NamedTextColor.GREEN));
            case "CLASS_TRAINER" -> player.sendMessage(Component.text("[NPC] 正在開啟轉職導師介面: " + def.targetId(), NamedTextColor.AQUA));
            case "QUEST_GIVER" -> player.sendMessage(Component.text("[NPC] 正在開啟任務介面...", NamedTextColor.GOLD));
            case "SHOP", "TRADER" -> openMerchantShop(player, def);
        }
    }

    private void openMerchantShop(Player player, RpgNpcDefinition def) {
        Component title = LegacyComponentSerializer.legacyAmpersand().deserialize(def.displayName() + " &8[RPG 商店]");
        org.bukkit.inventory.Merchant merchant = Bukkit.createMerchant(title);
        List<org.bukkit.inventory.MerchantRecipe> recipes = new ArrayList<>();

        for (RpgNpcDefinition.TradeOffer offer : def.trades()) {
            org.bukkit.inventory.ItemStack resultStack;
            if (offer.resultItemKey() != null && !offer.resultItemKey().isBlank()) {
                Material mat = Material.matchMaterial(offer.resultMaterial() != null ? offer.resultMaterial() : "DIAMOND_SWORD");
                resultStack = new org.bukkit.inventory.ItemStack(mat != null ? mat : Material.DIAMOND_SWORD, Math.max(1, offer.resultAmount()));
                if (plugin.getEngineRegistry().items().containsKey(offer.resultItemKey())) {
                    resultStack = plugin.getAdvancedItems().decorate(resultStack, offer.resultItemKey());
                }
            } else {
                Material mat = Material.matchMaterial(offer.resultMaterial() != null ? offer.resultMaterial() : "DIAMOND");
                resultStack = new org.bukkit.inventory.ItemStack(mat != null ? mat : Material.DIAMOND, Math.max(1, offer.resultAmount()));
            }

            org.bukkit.inventory.MerchantRecipe recipe = new org.bukkit.inventory.MerchantRecipe(resultStack, Math.max(1, offer.maxUses()));

            Material b1Mat = Material.matchMaterial(offer.buy1Material() != null ? offer.buy1Material() : "EMERALD");
            recipe.addIngredient(new org.bukkit.inventory.ItemStack(b1Mat != null ? b1Mat : Material.EMERALD, Math.max(1, offer.buy1Amount())));

            if (offer.buy2Material() != null && !offer.buy2Material().isBlank() && offer.buy2Amount() > 0) {
                Material b2Mat = Material.matchMaterial(offer.buy2Material());
                if (b2Mat != null) {
                    recipe.addIngredient(new org.bukkit.inventory.ItemStack(b2Mat, offer.buy2Amount()));
                }
            }

            recipes.add(recipe);
        }

        if (recipes.isEmpty()) {
            player.sendMessage(Component.text("[RPG 商店] 該 NPC 目前沒有開放交易項目。", NamedTextColor.YELLOW));
            return;
        }

        merchant.setRecipes(recipes);
        player.openMerchant(merchant, true);
    }
}
