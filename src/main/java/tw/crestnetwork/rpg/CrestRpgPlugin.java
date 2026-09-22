package tw.crestnetwork.rpg;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.example.crestrpg.command.SkillsCommand;
import com.example.crestrpg.database.RPGDatabase;
import com.example.crestrpg.equipment.EquipmentListener;
import com.example.crestrpg.equipment.EquipmentManager;
import com.example.crestrpg.gui.SkillsGUI;
import com.example.crestrpg.skills.PlayerProfile;
import com.example.crestrpg.skills.ProfileManager;
import com.example.crestrpg.skills.SkillsListener;
import com.example.crestrpg.command.QuestsCommand;
import com.example.crestrpg.gui.QuestsGUI;
import com.example.crestrpg.quests.QuestManager;
import com.example.crestrpg.quests.QuestsListener;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.example.crestrpg.lifeskills.SortingMenu;
import com.example.crestrpg.lifeskills.listener.SortListener;
import com.example.crestrpg.lifeskills.manager.PlayerCooldownManager;
import com.example.crestrpg.lifeskills.manager.PlayerToggleManager;
import tw.crestnetwork.rpg.editor.RpgDraftManager;
import tw.crestnetwork.rpg.editor.EditorSessionManager;
import tw.crestnetwork.rpg.editor.RpgEditorMenu;
import tw.crestnetwork.rpg.editor.AffixEditorMenu;
import tw.crestnetwork.rpg.editor.ClassEditorMenu;
import tw.crestnetwork.rpg.editor.CraftingStationEditorMenu;
import tw.crestnetwork.rpg.editor.DropTableEditorMenu;
import tw.crestnetwork.rpg.editor.EquipmentEditorMenu;
import tw.crestnetwork.rpg.editor.GemEditorMenu;
import tw.crestnetwork.rpg.editor.ItemEditorMenu;
import tw.crestnetwork.rpg.editor.MonsterEditorMenu;
import tw.crestnetwork.rpg.editor.NpcEditorMenu;
import tw.crestnetwork.rpg.editor.SetEditorMenu;
import tw.crestnetwork.rpg.editor.SkillEditorMenu;
import tw.crestnetwork.rpg.editor.SkillTreeEditorMenu;
import tw.crestnetwork.rpg.editor.WeaponEditorMenu;
import tw.crestnetwork.rpg.editor.UniversalDraftEditorMenu;
import tw.crestnetwork.rpg.editor.VanillaMaterialPickerMenu;
import tw.crestnetwork.rpg.editor.EmbeddedWebEditorServer;
import tw.crestnetwork.rpg.npc.RpgNpcEngine;
import tw.crestnetwork.rpg.party.RpgPartyManager;
import tw.crestnetwork.rpg.party.PartyCommand;
import tw.crestnetwork.rpg.dungeon.RpgDungeonEngine;
import tw.crestnetwork.rpg.title.RpgTitleManager;

public final class CrestRpgPlugin extends JavaPlugin implements Listener {
    private static CrestRpgPlugin instance;
    private final AtomicReference<Map<String, RpgMonsterDefinition>> definitions = new AtomicReference<>(Map.of());
    private final AtomicReference<Map<String, RpgWeaponDefinition>> weaponDefinitions = new AtomicReference<>(Map.of());
    private final AtomicReference<Map<String, RpgEquipmentDefinition>> equipmentDefinitions = new AtomicReference<>(Map.of());
    private final AtomicReference<Map<String, RpgItemDefinition>> itemDefinitions = new AtomicReference<>(Map.of());
    private final AtomicReference<RpgEngineRegistry> engineRegistry = new AtomicReference<>(RpgEngineRegistry.empty());
    private final Map<UUID, Long> abilityCooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<UUID> chainedBreakPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicBoolean syncing = new AtomicBoolean();
    private NamespacedKey monsterKey;
    private NamespacedKey weaponKey;
    private NamespacedKey contentKey;
    private RpgContentClient client;
    private RPGDatabase legacyDatabase;
    private ProfileManager profileManager;
    private EquipmentManager equipmentManager;
    private SkillsGUI skillsGUI;
    private QuestManager questManager;
    private QuestsGUI questsGUI;
    private PlayerCooldownManager cooldownManager;
    private PlayerToggleManager toggleManager;
    private SortListener sortListener;
    private AdvancedItemService advancedItems;
    private RpgSkillEngine skillEngine;
    private SkillBarMenu skillBarMenu;
    private SkillTreeMenu skillTreeMenu;
    private RpgCraftingMenu craftingMenu;
    private CrestRpgCommandMenu commandMenu;
    private PlayerCenterMenu playerCenterMenu;
    private RolledItemAbilityService rolledItemAbilities;
    private RpgDraftManager draftManager;
    private EditorSessionManager editorSessionManager;
    private RpgEditorMenu editorMenu;
    private EmbeddedWebEditorServer webEditorServer;
    private RpgNpcEngine npcEngine;
    private RpgPartyManager partyManager;
    private RpgDungeonEngine dungeonEngine;
    private RpgTitleManager titleManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveBundledResourceIfMissing("items.yml");
        saveBundledResourceIfMissing("quests.yml");
        monsterKey = new NamespacedKey(this, "monster_key");
        weaponKey = new NamespacedKey(this, "weapon_key");
        contentKey = new NamespacedKey(this, "content_key");
        advancedItems = new AdvancedItemService(this, engineRegistry::get);
        String apiBase = getConfig().getString("api-base-url", "");
        String token = getConfig().getString("api-token", "");
        String serverKey = getConfig().getString("server-key", "example-server");
        client = new RpgContentClient(apiBase, token, serverKey);

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new RpgGuiSecurityListener(this), this);
        initializePlayerProgression();
        long ticks = Math.max(10, getConfig().getLong("poll-seconds", 15)) * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> synchronize(null), 1L, ticks);
        if (token.isBlank() || token.equals("CHANGE_ME")) {
            getLogger().severe("api-token 尚未設定，CrestRPG 無法同步網站內容。");
        }
    }

    private void saveBundledResourceIfMissing(String name) {
        if (!new File(getDataFolder(), name).isFile()) {
            saveResource(name, false);
        }
    }

    @Override
    public void onDisable() {
        if (webEditorServer != null) webEditorServer.stop();
        if (profileManager != null) profileManager.saveAll();
        if (legacyDatabase != null) legacyDatabase.close();
    }

    private void initializePlayerProgression() {
        legacyDatabase = new RPGDatabase(this);
        equipmentManager = new EquipmentManager(this);
        profileManager = new ProfileManager(this);
        skillsGUI = new SkillsGUI(this);
        setupEconomy();
        questManager = new QuestManager(this);
        questsGUI = new QuestsGUI(this);
        cooldownManager = new PlayerCooldownManager();
        toggleManager = new PlayerToggleManager();
        sortListener = new SortListener(this);
        SortingMenu sortingMenu = new SortingMenu(this);
        Bukkit.getPluginManager().registerEvents(new SkillsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EquipmentListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuestsListener(this), this);
        Bukkit.getPluginManager().registerEvents(sortListener, this);
        Bukkit.getPluginManager().registerEvents(sortingMenu, this);
        skillEngine = new RpgSkillEngine(this, engineRegistry::get);
        Bukkit.getPluginManager().registerEvents(skillEngine, this);
        skillBarMenu = new SkillBarMenu(this);
        Bukkit.getPluginManager().registerEvents(skillBarMenu, this);
        Bukkit.getScheduler().runTaskTimer(this, skillEngine::tickPassives, 20L, 20L);
        skillTreeMenu = new SkillTreeMenu(this, engineRegistry::get);
        Bukkit.getPluginManager().registerEvents(skillTreeMenu, this);
        craftingMenu = new RpgCraftingMenu(this);
        Bukkit.getPluginManager().registerEvents(craftingMenu, this);
        commandMenu = new CrestRpgCommandMenu();
        Bukkit.getPluginManager().registerEvents(commandMenu, this);
        playerCenterMenu = new PlayerCenterMenu(this);
        Bukkit.getPluginManager().registerEvents(playerCenterMenu, this);
        rolledItemAbilities = new RolledItemAbilityService(this);
        Bukkit.getPluginManager().registerEvents(rolledItemAbilities, this);
        draftManager = new RpgDraftManager(this);
        editorSessionManager = new EditorSessionManager(this, draftManager);
        editorMenu = new RpgEditorMenu(this, editorSessionManager);
        webEditorServer = new EmbeddedWebEditorServer(this, draftManager);
        webEditorServer.start();
        for (Listener listener : List.of(
                editorMenu,
                new ItemEditorMenu(this, editorSessionManager),
                new WeaponEditorMenu(this, editorSessionManager),
                new EquipmentEditorMenu(this, editorSessionManager),
                new GemEditorMenu(this, editorSessionManager),
                new AffixEditorMenu(this, editorSessionManager),
                new SetEditorMenu(this, editorSessionManager),
                new ClassEditorMenu(this, editorSessionManager),
                new SkillEditorMenu(this, editorSessionManager),
                new SkillTreeEditorMenu(this, editorSessionManager),
                new MonsterEditorMenu(this, editorSessionManager),
                new DropTableEditorMenu(this, editorSessionManager),
                new CraftingStationEditorMenu(this, editorSessionManager),
                new NpcEditorMenu(this, editorSessionManager),
                new VanillaMaterialPickerMenu(this, editorSessionManager),
                new UniversalDraftEditorMenu(this, editorSessionManager))) {
            Bukkit.getPluginManager().registerEvents(listener, this);
        }
        npcEngine = new RpgNpcEngine(this);
        npcEngine.loadNpcsFromDrafts(draftManager);
        partyManager = new RpgPartyManager(this);
        dungeonEngine = new RpgDungeonEngine(this);
        titleManager = new RpgTitleManager(this);
        if (getCommand("skills") != null) getCommand("skills").setExecutor(new SkillsCommand(this));
        if (getCommand("quests") != null) getCommand("quests").setExecutor(new QuestsCommand(this));
        if (getCommand("party") != null) getCommand("party").setExecutor(new PartyCommand(this, partyManager));
        if (getCommand("titles") != null) getCommand("titles").setExecutor(titleManager);
        if (getCommand("cls") != null) {
            getCommand("cls").setExecutor(sortingMenu);
            getCommand("cls").setTabCompleter(sortingMenu);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            profileManager.loadPlayer(player);
            questManager.loadPlayer(player);
        }
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
                if (profile != null) profile.updatePlayerAttributes(player);
            }
        }, 20L, 20L);
        getLogger().info("已合併啟用舊版玩家技能、屬性、裝備限制與進度資料庫。");
    }

    public static CrestRpgPlugin getInstance() { return instance; }
    public RPGDatabase getDatabaseManager() { return legacyDatabase; }
    public ProfileManager getProfileManager() { return profileManager; }
    public EquipmentManager getEquipmentManager() { return equipmentManager; }
    public SkillsGUI getSkillsGUI() { return skillsGUI; }
    public QuestManager getQuestManager() { return questManager; }
    public QuestsGUI getQuestsGUI() { return questsGUI; }
    public boolean isVaultEnabled() { return VaultEconomyBridge.isAvailable(); }
    public RpgNpcEngine getNpcEngine() { return npcEngine; }
    public RpgDraftManager getDraftManager() { return draftManager; }
    public void openPlayerCenter(Player player) { playerCenterMenu.open(player); }
    RpgEngineRegistry registry() { return engineRegistry.get(); }
    Map<String, RpgWeaponDefinition> weaponDefinitions() { return weaponDefinitions.get(); }
    Map<String, RpgEquipmentDefinition> equipmentDefinitions() { return equipmentDefinitions.get(); }
    Map<String, RpgItemDefinition> itemDefinitions() { return itemDefinitions.get(); }
    public int applyEditorDrafts() {
        Map<String,RpgWeaponDefinition> weapons=new LinkedHashMap<>();
        for(Map.Entry<String,JsonObject> entry:draftManager.getDrafts("weapons").entrySet()){JsonObject v=entry.getValue();String k=entry.getKey();weapons.put(k,new RpgWeaponDefinition(1,1,k,jt(v,"name",k),jt(v,"base_item","minecraft:diamond_sword"),jn(v,"oraxen_id"),jt(v,"rarity","common"),ji(v,"level",1),jd(v,"attack_damage",1),jd(v,"attack_speed",1.6),jd(v,"critical_chance",0),v.has("max_durability")?ji(v,"max_durability",0):null,jn(v,"tool_ability"),v.has("ability_unlock_level")?ji(v,"ability_unlock_level",1):null,ji(v,"ability_max_blocks",32),ji(v,"ability_radius",1),ji(v,"ability_cooldown_seconds",3),js(v,"lore"),null,null,jb(v,"enabled",true)));}
        Map<String,RpgEquipmentDefinition> equipments=new LinkedHashMap<>();
        for(Map.Entry<String,JsonObject> entry:draftManager.getDrafts("equipments").entrySet()){JsonObject v=entry.getValue();String k=entry.getKey();equipments.put(k,new RpgEquipmentDefinition(1,1,k,jt(v,"name",k),jt(v,"base_item","minecraft:iron_chestplate"),jn(v,"oraxen_id"),jt(v,"rarity","common"),ji(v,"level",1),jt(v,"equipment_slot","chest"),jd(v,"armor",0),jd(v,"armor_toughness",0),jd(v,"knockback_resistance",0),jd(v,"health_bonus",0),js(v,"lore"),null,null,jb(v,"enabled",true)));}
        Map<String,RpgItemDefinition> items=new LinkedHashMap<>();
        for(Map.Entry<String,JsonObject> entry:draftManager.getDrafts("items").entrySet()){JsonObject v=entry.getValue();String k=entry.getKey();items.put(k,new RpgItemDefinition(1,1,k,jt(v,"name",k),jt(v,"base_item","minecraft:paper"),jn(v,"oraxen_id"),jt(v,"rarity","common"),ji(v,"level",1),ji(v,"max_stack_size",64),js(v,"lore"),null,null,jb(v,"enabled",true)));}
        Map<String,RpgMonsterDefinition> monsters=new LinkedHashMap<>();
        for(Map.Entry<String,JsonObject> entry:draftManager.getDrafts("monsters").entrySet()){JsonObject v=entry.getValue();String k=entry.getKey();List<RpgMonsterDefinition.DropDefinition> drops=new ArrayList<>();if(v.has("drops")&&v.get("drops").isJsonArray())v.getAsJsonArray("drops").forEach(e->{JsonObject d=e.getAsJsonObject();drops.add(new RpgMonsterDefinition.DropDefinition(jt(d,"item_key","minecraft:rotten_flesh"),jd(d,"chance",1),ji(d,"min",1),ji(d,"max",1)));});List<RpgMonsterDefinition.EquipmentDefinition> gear=new ArrayList<>();if(v.has("equipment")&&v.get("equipment").isJsonArray())v.getAsJsonArray("equipment").forEach(e->{JsonObject g=e.getAsJsonObject();gear.add(new RpgMonsterDefinition.EquipmentDefinition(jt(g,"slot","main_hand"),jt(g,"item_key","minecraft:iron_sword"),jd(g,"chance",1)));});monsters.put(k,new RpgMonsterDefinition(1,1,k,jt(v,"name",k),jt(v,"entity_type","minecraft:zombie"),jn(v,"model_engine_id"),ji(v,"level",1),jd(v,"max_health",20),jd(v,"damage",2),jd(v,"movement_speed",.23),jd(v,"follow_range",32),jd(v,"armor",0),jd(v,"knockback_resistance",0),List.copyOf(gear),List.copyOf(drops),null,null,jb(v,"enabled",true)));}
        List<RpgGameplayDefinition> gameplay=new ArrayList<>();addDraftSection(gameplay,"classes");addDraftSection(gameplay,"skills");addDraftSection(gameplay,"skill-trees");addDraftSection(gameplay,"crafting-stations");addDraftSection(gameplay,"drop-tables");RpgEngineRegistry next=RpgEngineRegistry.from(gameplay);weaponDefinitions.set(Map.copyOf(weapons));equipmentDefinitions.set(Map.copyOf(equipments));itemDefinitions.set(Map.copyOf(items));definitions.set(Map.copyOf(monsters));engineRegistry.set(next);Bukkit.getScheduler().runTask(this,()->{refreshPlayerProgressionRules();if(npcEngine!=null)npcEngine.loadNpcsFromDrafts(draftManager);});return weapons.size()+equipments.size()+items.size()+monsters.size()+next.classes().size()+next.skills().size();
    }
    private void addDraftSection(List<RpgGameplayDefinition> out,String kind){JsonArray values=new JsonArray();draftManager.getDrafts(kind).values().forEach(v->values.add(v.deepCopy()));JsonObject data=new JsonObject();data.add("definitions",values);out.add(new RpgGameplayDefinition(1,1,kind,kind,data,true));}
    private static String jt(JsonObject v,String k,String f){return v.has(k)&&!v.get(k).isJsonNull()?v.get(k).getAsString():f;}private static String jn(JsonObject v,String k){String s=jt(v,k,"");return s.isBlank()?null:s;}private static int ji(JsonObject v,String k,int f){return v.has(k)?v.get(k).getAsInt():f;}private static double jd(JsonObject v,String k,double f){return v.has(k)?v.get(k).getAsDouble():f;}private static boolean jb(JsonObject v,String k,boolean f){return v.has(k)?v.get(k).getAsBoolean():f;}private static List<String> js(JsonObject v,String k){if(!v.has(k)||!v.get(k).isJsonArray())return List.of();List<String> r=new ArrayList<>();v.getAsJsonArray(k).forEach(e->r.add(e.getAsString()));return List.copyOf(r);}
    public RpgPartyManager getPartyManager() { return partyManager; }
    public RpgDungeonEngine getDungeonEngine() { return dungeonEngine; }
    public RpgTitleManager getTitleManager() { return titleManager; }
    public RpgEngineRegistry getEngineRegistry() { return engineRegistry.get(); }
    public AdvancedItemService getAdvancedItems() { return advancedItems; }
    public PlayerCooldownManager getCooldownManager() { return cooldownManager; }
    public PlayerToggleManager getToggleManager() { return toggleManager; }
    public SortListener getSortListener() { return sortListener; }
    public int getAdvancedStatBonus(Player player, String stat) {
        if (advancedItems == null || player == null) return 0;
        double total = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) total += advancedItems.stats(item).getOrDefault(stat, 0.0);
        total += advancedItems.stats(player.getInventory().getItemInMainHand()).getOrDefault(stat, 0.0);
        total += advancedItems.stats(player.getInventory().getItemInOffHand()).getOrDefault(stat, 0.0);
        total += advancedItems.activeSetBonuses(player).getOrDefault(stat, 0.0);
        return (int) Math.round(total);
    }
    public int getClassStatBonus(UUID playerId, String stat) {
        PlayerProfile profile = profileManager == null ? null : profileManager.getProfile(playerId);
        RpgEngineRegistry.ClassDefinition definition = profile == null ? null : engineRegistry.get().classes().get(profile.getClassId());
        return definition == null ? 0 : (int) Math.round(definition.baseStats().getOrDefault(stat, 0.0));
    }
    public double getClassBaseMana(UUID playerId, double fallback) {
        PlayerProfile profile = profileManager == null ? null : profileManager.getProfile(playerId);
        RpgEngineRegistry.ClassDefinition definition = profile == null ? null : engineRegistry.get().classes().get(profile.getClassId());
        return definition == null ? fallback : definition.baseMana();
    }
    public double getLifeSkillMana(Player player) {
        PlayerProfile profile = profileManager == null ? null : profileManager.getProfile(player.getUniqueId());
        return profile == null ? 0 : profile.getCurrentMana();
    }
    public boolean hasLifeSkillMana(Player player, double amount) { return getLifeSkillMana(player) >= amount; }
    public boolean consumeLifeSkillMana(Player player, double amount) {
        PlayerProfile profile = profileManager == null ? null : profileManager.getProfile(player.getUniqueId());
        if (profile == null || profile.getCurrentMana() < amount) return false;
        profile.setCurrentMana(profile.getCurrentMana() - amount);
        return true;
    }

    private void setupEconomy() {
        VaultEconomyBridge.isAvailable();
    }

    void refreshPlayerProgressionRules() {
        if (equipmentManager != null) equipmentManager.reload();
        if (questManager != null) questManager.loadQuests();
        if (profileManager == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
            if (profile != null) profile.updatePlayerAttributes(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rpgitem")) return playerItemCommand(sender, args);
        if (command.getName().equalsIgnoreCase("rpgclass")) return playerClassCommand(sender, args);
        if (command.getName().equalsIgnoreCase("skilltree")) {
            if (sender instanceof Player player) skillTreeMenu.open(player);
            else sender.sendMessage(Component.text("此操作必須由玩家執行。", NamedTextColor.RED));
            return true;
        }
        if (command.getName().equalsIgnoreCase("rpgcraft")) return craftingCommand(sender, args);
        if (command.getName().equalsIgnoreCase("rpgskill")) {
            return skillCommand(sender, args);
        }
        if (!sender.hasPermission("crestrpg.admin")) {
            sender.sendMessage(Component.text("你沒有 CrestRPG 管理權限。", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player) commandMenu.open(player); else usage(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "editor" -> {
                if (sender instanceof Player player) {
                    editorMenu.open(player);
                } else {
                    sender.sendMessage(Component.text("此操作必須由玩家執行。", NamedTextColor.RED));
                }
                yield true;
            }
            case "web" -> {
                if (webEditorServer == null || !webEditorServer.isRunning()) {
                    String reason = webEditorServer == null ? "尚未初始化" : webEditorServer.getStatusMessage();
                    sender.sendMessage(Component.text("內建網頁編輯器未執行：" + reason + "。修正 config.yml 後請完整重啟伺服器。", NamedTextColor.RED));
                } else {
                    String host = getConfig().getString("web-editor.bind-address", "127.0.0.1");
                    int port = getConfig().getInt("web-editor.port", 8765);
                    String displayHost = host.equals("0.0.0.0") ? "<伺服器 IP 或網域>" : host;
                    sender.sendMessage(Component.text("內建網頁編輯器正在執行：http://" + displayHost + ":" + port + "/", NamedTextColor.AQUA));
                }
                yield true;
            }
            case "reload" -> {
                sender.sendMessage(Component.text("正在從網站同步 RPG 內容…", NamedTextColor.GRAY));
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> synchronize(sender));
                yield true;
            }
            case "list" -> {
                List<String> keys = definitions.get().values().stream().filter(RpgMonsterDefinition::enabled)
                        .map(definition -> definition.key() + " (v" + definition.version() + ")").toList();
                sender.sendMessage(Component.text(keys.isEmpty() ? "目前沒有已發布且啟用的怪物。" : "CrestRPG 怪物：" + String.join(", ", keys), NamedTextColor.AQUA));
                yield true;
            }
            case "weapons" -> {
                List<String> keys = weaponDefinitions.get().values().stream().filter(RpgWeaponDefinition::enabled)
                        .map(definition -> definition.key() + " (v" + definition.version() + ")").toList();
                sender.sendMessage(Component.text(keys.isEmpty() ? "目前沒有已發布且啟用的武器。" : "CrestRPG 武器：" + String.join(", ", keys), NamedTextColor.AQUA));
                yield true;
            }
            case "equipments" -> listContent(sender, "裝備", equipmentDefinitions.get());
            case "items" -> listContent(sender, "物品", itemDefinitions.get());
            case "spawn" -> spawnCommand(sender, args);
            case "give" -> giveContentCommand(sender, args);
            case "identify" -> advancedItemCommand(sender, "identify", null);
            case "upgrade" -> advancedItemCommand(sender, "upgrade", null);
            case "socket" -> args.length == 2 ? advancedItemCommand(sender, "socket", args[1]) : usage(sender);
            case "salvage" -> advancedItemCommand(sender, "salvage", null);
            default -> usage(sender);
        };
    }

    private boolean skillCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("此指令僅供玩家使用。"); return true; }
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()); if (profile == null) return true;
        if (args.length == 0 || args[0].equalsIgnoreCase("bar")) { skillBarMenu.open(player); return true; }
        if (args[0].equalsIgnoreCase("list")) {
            RpgEngineRegistry.ClassDefinition role = engineRegistry.get().classes().get(profile.getClassId());
            player.sendMessage(Component.text(role == null ? "目前職業沒有技能。" : "職業技能：" + String.join("、", role.skills()), NamedTextColor.AQUA)); return true;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("cast") || args[0].equalsIgnoreCase("use"))) {
            String skill = args[0].equalsIgnoreCase("use") ? profile.getSkillBar().get(parseBarSlot(args[1])) : args[1];
            if (skill == null || !castSkill(player, skill)) player.sendMessage(Component.text("無法施放：技能、解鎖、目標、法力或冷卻條件不符。", NamedTextColor.RED)); return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("bind")) {
            int slot = parseBarSlot(args[1]); String skill = args[2].toLowerCase(Locale.ROOT); RpgEngineRegistry.ClassDefinition role = engineRegistry.get().classes().get(profile.getClassId());
            if (slot < 1 || role == null || !role.skills().contains(skill) || !engineRegistry.get().skills().containsKey(skill)) player.sendMessage(Component.text("技能槽或職業技能無效。", NamedTextColor.RED));
            else { profile.bindSkill(slot, skill); saveProfileAsync(profile); player.sendMessage(Component.text("已將 " + skill + " 綁定至技能槽 " + slot + "。", NamedTextColor.GREEN)); }
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("unbind")) { int slot = parseBarSlot(args[1]); if (slot > 0) { profile.unbindSkill(slot); saveProfileAsync(profile); } return true; }
        player.sendMessage(Component.text("用法：/rpgskill bar | list | cast <技能> | bind <1-9> <技能> | unbind <1-9> | use <1-9>", NamedTextColor.YELLOW)); return true;
    }

    private int parseBarSlot(String raw) { try { int value = Integer.parseInt(raw); return value >= 1 && value <= 9 ? value : -1; } catch (NumberFormatException ignored) { return -1; } }
    boolean castSkill(Player player, String skill) { return skillEngine.castManual(player, skill); }
    void saveProfileAsync(PlayerProfile profile) { Bukkit.getScheduler().runTaskAsynchronously(this, () -> legacyDatabase.saveProfile(profile)); }


    private boolean craftingCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此操作必須由玩家執行。", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            String stations = String.join("、", engineRegistry.get().craftingStations().keySet());
            player.sendMessage(Component.text(stations.isBlank() ? "目前沒有已發布製作站。" : "製作站：" + stations, NamedTextColor.AQUA));
            return true;
        }
        craftingMenu.open(player, args[0].toLowerCase(Locale.ROOT));
        return true;
    }

    private boolean playerClassCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此操作必須由玩家執行。", NamedTextColor.RED));
            return true;
        }
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(Component.text("玩家資料仍在載入，請稍後再試。", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            RpgEngineRegistry.ClassDefinition current = engineRegistry.get().classes().get(profile.getClassId());
            player.sendMessage(Component.text("目前職業：" + (current == null ? profile.getClassId() : current.name()), NamedTextColor.AQUA));
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            String values = engineRegistry.get().classes().values().stream()
                    .map(value -> value.key() + "（" + value.name() + "）").collect(java.util.stream.Collectors.joining("、"));
            player.sendMessage(Component.text(values.isBlank() ? "目前沒有已發布職業。" : "可選職業：" + values, NamedTextColor.AQUA));
            return true;
        }
        if (args[0].equalsIgnoreCase("choose") && args.length == 2) {
            String key = args[1].toLowerCase(Locale.ROOT);
            RpgEngineRegistry.ClassDefinition selected = engineRegistry.get().classes().get(key);
            if (selected == null) {
                player.sendMessage(Component.text("找不到職業：" + key, NamedTextColor.RED));
                return true;
            }
            profile.setClassId(key);
            profile.setClassSelected(true);
            profile.getSkillBar().entrySet().removeIf(entry -> !selected.skills().contains(entry.getValue()));
            profile.setCurrentMana(Math.min(profile.getCurrentMana(), profile.getMaxMana()));
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> legacyDatabase.saveProfile(profile));
            player.sendMessage(Component.text("已選擇職業：" + selected.name(), NamedTextColor.GREEN));
            return true;
        }
        player.sendMessage(Component.text("用法：/rpgclass list | info | choose <職業代碼>", NamedTextColor.YELLOW));
        return true;
    }

    private boolean playerItemCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此操作必須由玩家執行。", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1 || args.length > 2) return playerItemUsage(player);
        ItemStack held = player.getInventory().getItemInMainHand();
        String action = args[0].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "identify" -> {
                if (!advancedItems.isUnidentified(held)) {
                    player.sendMessage(Component.text("手持物品不需要鑑定。", NamedTextColor.RED));
                    yield true;
                }
                Material material = configuredMaterial("item-operations.identification.material", Material.AMETHYST_SHARD);
                int amount = Math.max(1, getConfig().getInt("item-operations.identification.amount", 1));
                if (!consume(player, Map.of(material, amount))) yield true;
                advancedItems.identify(held);
                player.sendMessage(Component.text("鑑定完成。", NamedTextColor.GREEN));
                yield true;
            }
            case "upgrade" -> {
                int level = advancedItems.upgradeLevel(held);
                int maximum = advancedItems.maxUpgrade(held);
                if (level < 0 || level >= maximum) {
                    player.sendMessage(Component.text("物品尚未鑑定、不可強化或已達上限。", NamedTextColor.RED));
                    yield true;
                }
                Material material = configuredMaterial("item-operations.upgrade.material", Material.NETHERITE_SCRAP);
                int amount = Math.max(1, getConfig().getInt("item-operations.upgrade.base-amount", 1) + level);
                if (!consume(player, Map.of(material, amount))) yield true;
                double base = getConfig().getDouble("item-operations.upgrade.success-base", 0.90);
                double penalty = getConfig().getDouble("item-operations.upgrade.success-penalty-per-level", 0.04);
                double minimum = getConfig().getDouble("item-operations.upgrade.minimum-success", 0.20);
                double chance = Math.max(minimum, Math.min(1.0, base - level * penalty));
                if (ThreadLocalRandom.current().nextDouble() <= chance && advancedItems.upgrade(held)) {
                    player.sendMessage(Component.text("強化成功：+" + (level + 1) + "。", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("強化失敗，材料已消耗；裝備不會降級或損壞。", NamedTextColor.RED));
                }
                yield true;
            }
            case "socket" -> {
                if (args.length != 2 || !advancedItems.canSocket(held, args[1])) {
                    player.sendMessage(Component.text("寶石代碼無效、物品未鑑定或沒有空插槽。", NamedTextColor.RED));
                    yield true;
                }
                Material gemMaterial = Material.matchMaterial(advancedItems.gemMaterial(args[1]));
                if (gemMaterial == null || !gemMaterial.isItem()) {
                    player.sendMessage(Component.text("寶石材料設定無效。", NamedTextColor.RED));
                    yield true;
                }
                Material catalyst = configuredMaterial("item-operations.socket.additional-material", Material.LAPIS_LAZULI);
                int catalystAmount = Math.max(0, getConfig().getInt("item-operations.socket.additional-amount", 8));
                Map<Material, Integer> costs = new LinkedHashMap<>();
                costs.merge(gemMaterial, 1, Integer::sum);
                if (catalystAmount > 0) costs.merge(catalyst, catalystAmount, Integer::sum);
                if (!consume(player, costs)) yield true;
                advancedItems.socket(held, args[1]);
                player.sendMessage(Component.text("寶石鑲嵌完成。", NamedTextColor.GREEN));
                yield true;
            }
            case "salvage" -> {
                double value = advancedItems.salvageValue(held);
                if (value <= 0) {
                    player.sendMessage(Component.text("此物品不可分解。", NamedTextColor.RED));
                    yield true;
                }
                Material output = configuredMaterial("item-operations.salvage.material", Material.EMERALD);
                double unit = Math.max(0.01, getConfig().getDouble("item-operations.salvage.value-per-item", 10.0));
                int amount = Math.max(1, (int) Math.ceil(value / unit));
                held.setAmount(0);
                while (amount > 0) {
                    int stackAmount = Math.min(output.getMaxStackSize(), amount);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(output, stackAmount));
                    overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                    amount -= stackAmount;
                }
                player.sendMessage(Component.text("分解完成。", NamedTextColor.GREEN));
                yield true;
            }
            default -> playerItemUsage(player);
        };
    }

    private boolean consume(Player player, Map<Material, Integer> costs) {
        for (Map.Entry<Material, Integer> entry : costs.entrySet()) {
            if (!player.getInventory().containsAtLeast(new ItemStack(entry.getKey()), entry.getValue())) {
                player.sendMessage(Component.text("材料不足：" + entry.getKey() + " x" + entry.getValue(), NamedTextColor.RED));
                return false;
            }
        }
        costs.forEach((material, amount) -> player.getInventory().removeItem(new ItemStack(material, amount)));
        return true;
    }

    private Material configuredMaterial(String path, Material fallback) {
        Material material = Material.matchMaterial(getConfig().getString(path, fallback.name()));
        return material == null || !material.isItem() || material.isAir() ? fallback : material;
    }

    private boolean playerItemUsage(Player player) {
        player.sendMessage(Component.text("用法：/rpgitem identify | upgrade | socket <寶石代碼> | salvage", NamedTextColor.YELLOW));
        return true;
    }

    private boolean advancedItemCommand(CommandSender sender, String action, String argument) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此操作必須由玩家手持物品執行。", NamedTextColor.RED));
            return true;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        boolean success;
        switch (action) {
            case "identify" -> success = advancedItems.identify(held);
            case "upgrade" -> success = advancedItems.upgrade(held);
            case "socket" -> success = advancedItems.socket(held, argument);
            case "salvage" -> {
                double value = advancedItems.salvageValue(held);
                if (value <= 0) success = false;
                else {
                    held.setAmount(0);
                    if (VaultEconomyBridge.isAvailable()) VaultEconomyBridge.deposit(player, value);
                    else player.getInventory().addItem(new ItemStack(Material.EMERALD, Math.max(1, Math.min(64, (int) Math.round(value)))));
                    player.sendMessage(Component.text("分解完成，獲得價值 " + Math.round(value * 100.0) / 100.0 + "。", NamedTextColor.GREEN));
                    success = true;
                }
            }
            default -> success = false;
        }
        if (!success) player.sendMessage(Component.text("操作失敗：物品狀態、上限或參數不符合。", NamedTextColor.RED));
        else if (!"salvage".equals(action)) player.sendMessage(Component.text("物品操作完成。", NamedTextColor.GREEN));
        return true;
    }

    private boolean listContent(CommandSender sender, String label, Map<String, ?> values) {
        sender.sendMessage(Component.text(values.isEmpty() ? "目前沒有已發布且啟用的" + label + "。" : "CrestRPG " + label + "：" + String.join(", ", values.keySet()), NamedTextColor.AQUA));
        return true;
    }

    private boolean giveContentCommand(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) return usage(sender);
        Player target;
        if (args.length == 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("找不到線上玩家：" + args[2], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("主控台使用時必須指定玩家。", NamedTextColor.RED));
            return true;
        }

        String key = args[1].toLowerCase(Locale.ROOT);
        RpgWeaponDefinition weaponDefinition = weaponDefinitions.get().get(key);
        RpgEquipmentDefinition equipmentDefinition = equipmentDefinitions.get().get(key);
        RpgItemDefinition itemDefinition = itemDefinitions.get().get(key);
        RpgOraxenAsset definition = weaponDefinition != null ? weaponDefinition : (equipmentDefinition != null ? equipmentDefinition : itemDefinition);
        if (definition == null || !definition.enabled()) {
            sender.sendMessage(Component.text("找不到已發布且啟用的內容：" + args[1], NamedTextColor.RED));
            return true;
        }

        try {
            ItemStack content = weaponDefinition != null ? createWeapon(weaponDefinition)
                    : equipmentDefinition != null ? createEquipment(equipmentDefinition) : createItem(itemDefinition);
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(content);
            overflow.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            sender.sendMessage(Component.text("已給予 " + target.getName() + "：" + definition.name(), NamedTextColor.GREEN));
        } catch (Exception exception) {
            sender.sendMessage(Component.text("建立武器失敗：" + exception.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private ItemStack createWeapon(RpgWeaponDefinition definition) {
        ItemStack item = createOraxenItem(definition);
        if (item == null) {
            Material material = Material.matchMaterial(definition.baseItem());
            if (material == null || !material.isItem() || material.isAir()) {
                throw new IllegalArgumentException("無效的基礎物品：" + definition.baseItem());
            }
            item = new ItemStack(material);
        }

        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.name(), rarityColor(definition.rarity())));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("稀有度：" + definition.rarity().toUpperCase(Locale.ROOT), NamedTextColor.GRAY));
        lore.add(Component.text("需求等級：" + definition.level(), NamedTextColor.GRAY));
        lore.add(Component.text("攻擊傷害：" + definition.attackDamage(), NamedTextColor.RED));
        lore.add(Component.text("攻擊速度：" + definition.attackSpeed(), NamedTextColor.AQUA));
        lore.add(Component.text("暴擊率：" + Math.round(definition.criticalChance() * 100) + "%", NamedTextColor.YELLOW));
        if (definition.maxDurability() != null) lore.add(Component.text("耐久度：" + definition.maxDurability(), NamedTextColor.GRAY));
        if (!definition.lore().isEmpty()) {
            lore.add(Component.empty());
            definition.lore().forEach(line -> lore.add(Component.text(line, NamedTextColor.DARK_GRAY)));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, definition.key());
        meta.getPersistentDataContainer().set(contentKey, PersistentDataType.STRING, definition.key());
        item.setItemMeta(meta);
        return rolledItemAbilities.roll(advancedItems.decorate(item, definition.key()), definition.key(), definition.level());
    }

    private ItemStack createEquipment(RpgEquipmentDefinition definition) {
        ItemStack item = createContentBase(definition);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.name(), rarityColor(definition.rarity())));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("需求等級：" + definition.level(), NamedTextColor.GRAY));
        lore.add(Component.text("護甲：" + definition.armor(), NamedTextColor.AQUA));
        lore.add(Component.text("護甲韌性：" + definition.armorToughness(), NamedTextColor.AQUA));
        if (definition.healthBonus() != 0) lore.add(Component.text("額外生命：" + definition.healthBonus(), NamedTextColor.RED));
        appendLore(lore, definition.lore());
        meta.lore(lore);
        EquipmentSlotGroup slot = equipmentSlot(definition.equipmentSlot());
        addAttribute(meta, Attribute.ARMOR, definition.armor(), definition.key() + "_armor", slot);
        addAttribute(meta, Attribute.ARMOR_TOUGHNESS, definition.armorToughness(), definition.key() + "_toughness", slot);
        addAttribute(meta, Attribute.KNOCKBACK_RESISTANCE, definition.knockbackResistance(), definition.key() + "_knockback", slot);
        addAttribute(meta, Attribute.MAX_HEALTH, definition.healthBonus(), definition.key() + "_health", slot);
        meta.getPersistentDataContainer().set(contentKey, PersistentDataType.STRING, definition.key());
        item.setItemMeta(meta);
        return rolledItemAbilities.roll(advancedItems.decorate(item, definition.key()), definition.key(), definition.level());
    }

    private ItemStack createItem(RpgItemDefinition definition) {
        ItemStack item = createContentBase(definition);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.name(), rarityColor(definition.rarity())));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("需求等級：" + definition.level(), NamedTextColor.GRAY));
        appendLore(lore, definition.lore());
        meta.lore(lore);
        meta.setMaxStackSize(definition.maxStackSize());
        meta.getPersistentDataContainer().set(contentKey, PersistentDataType.STRING, definition.key());
        item.setItemMeta(meta);
        return advancedItems.decorate(item, definition.key());
    }

    private EquipmentSlotGroup equipmentSlot(String slot) {
        return switch (slot) {
            case "head" -> EquipmentSlotGroup.HEAD;
            case "chest" -> EquipmentSlotGroup.CHEST;
            case "legs" -> EquipmentSlotGroup.LEGS;
            case "feet" -> EquipmentSlotGroup.FEET;
            case "off_hand" -> EquipmentSlotGroup.OFFHAND;
            default -> EquipmentSlotGroup.ARMOR;
        };
    }

    private void addAttribute(ItemMeta meta, Attribute attribute, double amount, String key, EquipmentSlotGroup slot) {
        if (amount == 0) return;
        meta.addAttributeModifier(attribute, new AttributeModifier(new NamespacedKey(this, key), amount, AttributeModifier.Operation.ADD_NUMBER, slot));
    }

    private void appendLore(List<Component> output, List<String> lines) {
        if (lines.isEmpty()) return;
        output.add(Component.empty());
        lines.forEach(line -> output.add(Component.text(line, NamedTextColor.DARK_GRAY)));
    }

    private ItemStack createContentBase(RpgOraxenAsset definition) {
        ItemStack item = createOraxenItem(definition);
        if (item != null) return item;
        Material material = Material.matchMaterial(definition.baseItem());
        if (material == null || !material.isItem() || material.isAir()) throw new IllegalArgumentException("無效的基礎物品：" + definition.baseItem());
        return new ItemStack(material);
    }

    private ItemStack createOraxenItem(RpgOraxenAsset definition) {
        Plugin oraxen = Bukkit.getPluginManager().getPlugin("Oraxen");
        if (oraxen == null || !oraxen.isEnabled()) return null;
        try {
            Class<?> itemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            Method getItemById = itemsClass.getMethod("getItemById", String.class);
            Object builder = getItemById.invoke(null, definition.oraxenId());
            if (builder == null) {
                getLogger().warning("Oraxen 找不到物品 " + definition.oraxenId() + "，改用基礎物品。");
                return null;
            }
            Object built = builder.getClass().getMethod("build").invoke(builder);
            return built instanceof ItemStack stack ? stack.clone() : null;
        } catch (ReflectiveOperationException exception) {
            getLogger().warning("無法透過 Oraxen 建立 " + definition.oraxenId() + "，改用基礎物品：" + exception.getMessage());
            return null;
        }
    }

    private NamedTextColor rarityColor(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "uncommon" -> NamedTextColor.GREEN;
            case "rare" -> NamedTextColor.BLUE;
            case "epic" -> NamedTextColor.DARK_PURPLE;
            case "legendary" -> NamedTextColor.GOLD;
            case "mythic" -> NamedTextColor.LIGHT_PURPLE;
            default -> NamedTextColor.WHITE;
        };
    }

    ItemStack createGeneratedContent(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        RpgWeaponDefinition weapon = weaponDefinitions.get().get(normalized);
        if (weapon != null && weapon.enabled()) return createWeapon(weapon);
        RpgEquipmentDefinition equipment = equipmentDefinitions.get().get(normalized);
        if (equipment != null && equipment.enabled()) return createEquipment(equipment);
        RpgItemDefinition item = itemDefinitions.get().get(normalized);
        if (item != null && item.enabled()) return createItem(item);
        RpgEngineRegistry.ItemTemplate template = engineRegistry.get().items().get(normalized);
        if (template != null) {
            Material templateMaterial = Material.matchMaterial(template.material());
            if (templateMaterial == null || !templateMaterial.isItem() || templateMaterial.isAir()) templateMaterial = Material.STONE;
            ItemStack generated = new ItemStack(templateMaterial);
            ItemMeta meta = generated.getItemMeta();
            meta.displayName(Component.text(template.name(), rarityColor(template.rarity())));
            List<Component> lore = new ArrayList<>();
            template.baseStats().forEach((stat, value) -> lore.add(Component.text(stat + " +" + value, NamedTextColor.GRAY)));
            meta.lore(lore); meta.getPersistentDataContainer().set(contentKey, PersistentDataType.STRING, normalized);
            generated.setItemMeta(meta);
            return advancedItems.decorate(generated, normalized);
        }
        Material material = Material.matchMaterial(normalized);
        return material == null || !material.isItem() || material.isAir() ? null : new ItemStack(material);
    }

    public ItemStack createContentItem(String key) {
        return createGeneratedContent(key);
    }

    String contentId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        return item.getPersistentDataContainer().get(contentKey, PersistentDataType.STRING);
    }

    RpgEngineRegistry engineSnapshot() { return engineRegistry.get(); }
    int characterLevel(Player player) { return playerLevel(player); }

    private boolean spawnCommand(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) return usage(sender);
        Player target;
        if (args.length == 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("找不到線上玩家：" + args[2], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("主控台使用時必須指定玩家。", NamedTextColor.RED));
            return true;
        }

        RpgMonsterDefinition definition = definitions.get().get(args[1].toLowerCase(Locale.ROOT));
        if (definition == null || !definition.enabled()) {
            sender.sendMessage(Component.text("找不到已發布且啟用的怪物：" + args[1], NamedTextColor.RED));
            return true;
        }

        try {
            LivingEntity entity = spawn(definition, target);
            sender.sendMessage(Component.text("已在 " + target.getName() + " 附近生成 " + definition.name(), NamedTextColor.GREEN));
            entity.setRotation(target.getYaw(), 0);
        } catch (Exception exception) {
            sender.sendMessage(Component.text("生成失敗：" + exception.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private LivingEntity spawn(RpgMonsterDefinition definition, Player target) {
        NamespacedKey typeKey = NamespacedKey.fromString(definition.entityType());
        EntityType entityType = typeKey == null ? null : Registry.ENTITY_TYPE.get(typeKey);
        if (entityType == null || !entityType.isAlive() || !entityType.isSpawnable()) {
            throw new IllegalArgumentException("無效或不可生成的實體：" + definition.entityType());
        }
        Entity spawned = target.getWorld().spawnEntity(target.getLocation(), entityType);
        if (!(spawned instanceof LivingEntity living)) {
            spawned.remove();
            throw new IllegalArgumentException("實體不是生物：" + definition.entityType());
        }

        AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            living.remove();
            throw new IllegalStateException("實體不支援生命值屬性");
        }
        maxHealth.setBaseValue(definition.maxHealth());
        living.setHealth(definition.maxHealth());
        setMobAttribute(living, Attribute.MOVEMENT_SPEED, definition.movementSpeed());
        setMobAttribute(living, Attribute.FOLLOW_RANGE, definition.followRange());
        setMobAttribute(living, Attribute.ARMOR, definition.armor());
        setMobAttribute(living, Attribute.KNOCKBACK_RESISTANCE, definition.knockbackResistance());
        for (RpgMonsterDefinition.EquipmentDefinition equipped : definition.equipment()) {
            if (ThreadLocalRandom.current().nextDouble() > equipped.chance()) continue;
            ItemStack equipmentItem = createGeneratedContent(equipped.itemKey()); if (equipmentItem == null) continue;
            switch (equipped.slot()) { case "head" -> living.getEquipment().setHelmet(equipmentItem); case "chest" -> living.getEquipment().setChestplate(equipmentItem); case "legs" -> living.getEquipment().setLeggings(equipmentItem); case "feet" -> living.getEquipment().setBoots(equipmentItem); case "off_hand" -> living.getEquipment().setItemInOffHand(equipmentItem); default -> living.getEquipment().setItemInMainHand(equipmentItem); }
        }
        living.customName(Component.text("[Lv." + definition.level() + "] " + definition.name(), NamedTextColor.GOLD));
        living.setCustomNameVisible(true);
        living.getPersistentDataContainer().set(monsterKey, PersistentDataType.STRING, definition.key());
        if (living instanceof Mob mob) mob.setRemoveWhenFarAway(false);
        new ModelEngineBridge(this).apply(living, definition.modelEngineId());
        return living;
    }

    private static void setMobAttribute(LivingEntity living, Attribute attribute, double value) { AttributeInstance instance=living.getAttribute(attribute); if(instance!=null&&Double.isFinite(value)&&value>=0)instance.setBaseValue(value); }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        RpgMonsterDefinition definition = definitionFor(event.getDamager());
        if (definition != null && definition.enabled()) event.setDamage(definition.damage());

        if (event.getDamager() instanceof Player player) {
            String key = player.getInventory().getItemInMainHand().getPersistentDataContainer()
                    .get(weaponKey, PersistentDataType.STRING);
            RpgWeaponDefinition weapon = key == null ? null : weaponDefinitions.get().get(key);
            if (weapon != null && weapon.enabled()) {
                if (!levelRequirementMet(player, weapon.level())) {
                    event.setCancelled(true);
                    return;
                }
                double damage = weapon.attackDamage();
                if (ThreadLocalRandom.current().nextDouble() < weapon.criticalChance()) damage *= 2;
                event.setDamage(damage);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (chainedBreakPlayers.contains(player.getUniqueId())) return;
        RpgWeaponDefinition heldWeapon = heldWeapon(player);
        if (heldWeapon != null && !levelRequirementMet(player, heldWeapon.level())) {
            event.setCancelled(true);
            return;
        }
        ToolAbilityDefinition ability = heldToolAbility(player);
        if (ability == null || !abilityReady(player, ability)) return;

        String materialName = player.getInventory().getItemInMainHand().getType().name();
        if (ability.ability().equals("vein_mining") && materialName.endsWith("_PICKAXE")) {
            breakConnected(player, event.getBlock(), ability, false);
        } else if (ability.ability().equals("tree_felling") && materialName.endsWith("_AXE") && !materialName.endsWith("_PICKAXE")
                && Tag.LOGS.isTagged(event.getBlock().getType())) {
            breakConnected(player, event.getBlock(), ability, true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCropInteract(PlayerInteractEvent event) {
        RpgWeaponDefinition heldWeapon = heldWeapon(event.getPlayer());
        if (heldWeapon != null && !levelRequirementMet(event.getPlayer(), heldWeapon.level())) {
            event.setCancelled(true);
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        ToolAbilityDefinition ability = heldToolAbility(player);
        if (ability == null || !"right_click_harvest".equals(ability.ability()) || !abilityReady(player, ability)) return;
        if (!player.getInventory().getItemInMainHand().getType().name().endsWith("_HOE")) return;

        Block origin = event.getClickedBlock();
        if (!(origin.getBlockData() instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge() || !isHarvestable(origin.getType())) return;
        int harvested = 0;
        int radius = ability.radius();
        outer: for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) for (int y = -1; y <= 1; y++) {
            Block crop = origin.getRelative(x, y, z);
            if (!(crop.getBlockData() instanceof Ageable cropAge) || cropAge.getAge() < cropAge.getMaximumAge() || !isHarvestable(crop.getType())) continue;
            Material type = crop.getType();
            if (!allowedToBreak(player, crop)) continue;
            crop.breakNaturally(player.getInventory().getItemInMainHand());
            crop.setType(type, false);
            Ageable replanted = (Ageable) crop.getBlockData();
            replanted.setAge(0);
            crop.setBlockData(replanted, false);
            if (++harvested >= ability.maxBlocks()) break outer;
        }
        if (harvested > 0) startCooldown(player, ability);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEquipmentClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack candidate = event.isShiftClick() ? event.getCurrentItem()
                : event.getSlotType() == InventoryType.SlotType.ARMOR ? event.getCursor() : null;
        RpgEquipmentDefinition equipment = equipmentDefinition(candidate);
        if (equipment != null && !levelRequirementMet(player, equipment.level())) {
            event.setCancelled(true);
            return;
        }
    }

    private RpgEquipmentDefinition equipmentDefinition(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        String key = item.getPersistentDataContainer().get(contentKey, PersistentDataType.STRING);
        RpgEquipmentDefinition definition = key == null ? null : equipmentDefinitions.get().get(key);
        return definition != null && definition.enabled() ? definition : null;
    }

    private boolean levelRequirementMet(Player player, int required) {
        int current = playerLevel(player);
        if (current < required) {
            player.sendActionBar(Component.text("此物品需要角色等級 " + required, NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private boolean isHarvestable(Material material) {
        return material == Material.WHEAT || material == Material.CARROTS || material == Material.POTATOES
                || material == Material.BEETROOTS || material == Material.NETHER_WART;
    }

    private RpgWeaponDefinition heldWeapon(Player player) {
        String key = player.getInventory().getItemInMainHand().getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
        RpgWeaponDefinition definition = key == null ? null : weaponDefinitions.get().get(key);
        return definition != null && definition.enabled() ? definition : null;
    }

    private ToolAbilityDefinition heldToolAbility(Player player) {
        RpgWeaponDefinition weapon = heldWeapon(player);
        if (weapon != null && weapon.toolAbility() != null) {
            return new ToolAbilityDefinition(weapon.key(), weapon.toolAbility(),
                    weapon.abilityUnlockLevel() == null ? weapon.level() : weapon.abilityUnlockLevel(),
                    weapon.abilityMaxBlocks(), weapon.abilityRadius(), weapon.abilityCooldownSeconds());
        }
        return null;
    }

    private boolean abilityReady(Player player, ToolAbilityDefinition ability) {
        int required = ability.unlockLevel();
        int currentLevel = playerLevel(player);
        if (currentLevel < required) {
            player.sendActionBar(Component.text("此功能需要等級 " + required, NamedTextColor.RED));
            return false;
        }
        long remaining = abilityCooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
        if (remaining > 0) {
            player.sendActionBar(Component.text("能力冷卻中：" + ((remaining + 999) / 1000) + " 秒", NamedTextColor.GRAY));
            return false;
        }
        return true;
    }

    int playerLevel(Player player) {
        PlayerProfile profile = profileManager == null ? null : profileManager.getProfile(player.getUniqueId());
        return profile == null ? 1 : profile.getCharacterLevel();
    }

    private void startCooldown(Player player, ToolAbilityDefinition ability) {
        abilityCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + ability.cooldownSeconds() * 1000L);
    }

    private void breakConnected(Player player, Block origin, ToolAbilityDefinition ability, boolean logs) {
        Material targetType = origin.getType();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(origin);
        int broken = 0;
        while (!queue.isEmpty() && broken < ability.maxBlocks()) {
            Block current = queue.removeFirst();
            String position = current.getX() + ":" + current.getY() + ":" + current.getZ();
            if (!visited.add(position)) continue;
            if (Math.abs(current.getX() - origin.getX()) > ability.radius()
                    || Math.abs(current.getY() - origin.getY()) > ability.radius()
                    || Math.abs(current.getZ() - origin.getZ()) > ability.radius()) continue;
            boolean matches = logs ? Tag.LOGS.isTagged(current.getType()) : current.getType() == targetType;
            if (!matches) continue;
            if (!current.equals(origin)) {
                if (!allowedToBreak(player, current)) continue;
                current.breakNaturally(player.getInventory().getItemInMainHand());
                broken++;
            }
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                if (x != 0 || y != 0 || z != 0) queue.add(current.getRelative(x, y, z));
            }
        }
        if (broken > 0) startCooldown(player, ability);
    }

    private boolean allowedToBreak(Player player, Block block) {
        chainedBreakPlayers.add(player.getUniqueId());
        try {
            BlockBreakEvent check = new BlockBreakEvent(block, player);
            Bukkit.getPluginManager().callEvent(check);
            return !check.isCancelled();
        } finally {
            chainedBreakPlayers.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        RpgMonsterDefinition definition = definitionFor(event.getEntity());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (definition != null) {
            event.getDrops().clear();
            for (RpgMonsterDefinition.DropDefinition drop : definition.drops()) {
                if (random.nextDouble() > drop.chance()) continue;
                int amount = drop.min() == drop.max() ? drop.min() : random.nextInt(drop.min(), drop.max() + 1);
                if (amount <= 0) continue;
                ItemStack generated = createGeneratedContent(drop.itemKey());
                if (generated == null) {
                    getLogger().warning("忽略無效掉落物：" + drop.itemKey() + "（怪物 " + definition.key() + "）");
                    continue;
                }
                generated.setAmount(Math.min(generated.getMaxStackSize(), amount)); event.getDrops().add(generated);
            }
        }
        String customSource = definition == null ? null : definition.key();
        String vanillaSource = "monsters_" + event.getEntityType().name().toLowerCase(Locale.ROOT);
        for (RpgEngineRegistry.DropTableDefinition table : engineRegistry.get().dropTables().values()) {
            if (!table.sources().contains(vanillaSource) && (customSource == null || !table.sources().contains(customSource))) continue;
            for (int roll = 0; roll < table.rolls(); roll++) {
                RpgEngineRegistry.DropEntry entry = rollDrop(table, random);
                int amount = entry.min() == entry.max() ? entry.min() : random.nextInt(entry.min(), entry.max() + 1);
                for (int index = 0; index < amount; index++) {
                    ItemStack generated = createGeneratedContent(entry.key());
                    if (generated != null) event.getDrops().add(generated);
                    else getLogger().warning("掉落表 " + table.key() + " 引用不存在的內容：" + entry.key());
                }
            }
        }
    }

    private RpgEngineRegistry.DropEntry rollDrop(RpgEngineRegistry.DropTableDefinition table, ThreadLocalRandom random) {
        double cursor = random.nextDouble(table.totalWeight());
        for (RpgEngineRegistry.DropEntry entry : table.entries()) {
            cursor -= entry.weight();
            if (cursor <= 0) return entry;
        }
        return table.entries().getLast();
    }

    private RpgMonsterDefinition definitionFor(Entity entity) {
        String key = entity.getPersistentDataContainer().get(monsterKey, PersistentDataType.STRING);
        return key == null ? null : definitions.get().get(key);
    }

    private void synchronize(CommandSender requester) {
        String token = getConfig().getString("api-token", "");
        if (token.isBlank() || token.equals("CHANGE_ME") || !syncing.compareAndSet(false, true)) return;
        try {
            RpgManifest manifest = client.fetchManifest();
            Map<String, RpgMonsterDefinition> next = new LinkedHashMap<>();
            for (RpgMonsterDefinition definition : manifest.monsters()) next.put(definition.key(), definition);
            Map<String, RpgWeaponDefinition> nextWeapons = new LinkedHashMap<>();
            for (RpgWeaponDefinition definition : manifest.weapons()) nextWeapons.put(definition.key(), definition);
            Map<String, RpgEquipmentDefinition> nextEquipments = new LinkedHashMap<>();
            for (RpgEquipmentDefinition definition : manifest.equipments()) nextEquipments.put(definition.key(), definition);
            Map<String, RpgItemDefinition> nextItems = new LinkedHashMap<>();
            for (RpgItemDefinition definition : manifest.items()) nextItems.put(definition.key(), definition);
            new RpgGameplayCache(this).save(manifest);
            Bukkit.getScheduler().runTask(this, () -> new RpgGameplayApplier(this).apply(manifest));
            definitions.set(Map.copyOf(next));
            weaponDefinitions.set(Map.copyOf(nextWeapons));
            equipmentDefinitions.set(Map.copyOf(nextEquipments));
            itemDefinitions.set(Map.copyOf(nextItems));
            RpgEngineRegistry nextRegistry = RpgEngineRegistry.from(manifest.gameplay());
            engineRegistry.set(nextRegistry);
            new OraxenAssetSynchronizer(this).synchronize(manifest);
            new ModelEngineAssetSynchronizer(this).synchronize(manifest);
            client.acknowledge(manifest, true, null);
            getLogger().info("已同步 " + next.size() + " 個怪物、" + nextWeapons.size() + " 把武器、" + nextEquipments.size() + " 件裝備、" + nextItems.size() + " 個物品，manifest " + manifest.checksum().substring(0, 12) + "…");
            getLogger().info("獨立 RPG 引擎已載入：" + nextRegistry.affixes().size() + " 詞綴、" + nextRegistry.gems().size()
                    + " 寶石、" + nextRegistry.sets().size() + " 套裝、" + nextRegistry.classes().size() + " 職業、"
                    + nextRegistry.skills().size() + " 技能、" + nextRegistry.craftingStations().size() + " 製作站。" );
            if (requester != null) Bukkit.getScheduler().runTask(this, () -> requester.sendMessage(Component.text("RPG 內容同步完成：" + next.size() + " 怪物、" + nextWeapons.size() + " 武器、" + nextEquipments.size() + " 裝備、" + nextItems.size() + " 物品。", NamedTextColor.GREEN)));
        } catch (Exception exception) {
            getLogger().warning("RPG 內容同步失敗：" + exception.getMessage());
            if (requester != null) Bukkit.getScheduler().runTask(this, () -> requester.sendMessage(Component.text("RPG 同步失敗：" + exception.getMessage(), NamedTextColor.RED)));
        } finally {
            syncing.set(false);
        }
    }

    private boolean usage(CommandSender sender) {
        sender.sendMessage(Component.text("── CrestRPG 管理指令 ──", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/crestrpg editor  開啟遊戲內圖形編輯器", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/crestrpg web     查看網頁編輯器狀態", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/crestrpg reload  重新同步 RPG 內容", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/crestrpg items | weapons | equipments | list", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/crestrpg spawn <怪物代碼> [玩家]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/crestrpg give <內容代碼> [玩家]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/crestrpg identify | upgrade | socket <寶石代碼> | salvage", NamedTextColor.YELLOW));
        return true;
    }
}
