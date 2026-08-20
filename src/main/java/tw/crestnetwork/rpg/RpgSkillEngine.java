package tw.crestnetwork.rpg;

import com.example.crestrpg.skills.PlayerProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

final class RpgSkillEngine implements Listener {
    private final CrestRpgPlugin plugin;
    private final Supplier<RpgEngineRegistry> registry;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> executingDamage = ConcurrentHashMap.newKeySet();
    private final Map<UUID, CastingState> casting = new ConcurrentHashMap<>();
    private final Map<UUID, ShieldState> shields = new ConcurrentHashMap<>();
    private final Map<UUID, ProjectileState> skillProjectiles = new ConcurrentHashMap<>();

    RpgSkillEngine(CrestRpgPlugin plugin, Supplier<RpgEngineRegistry> registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    void tickPassives() {
        for (Player player : Bukkit.getOnlinePlayers()) cast(player, null, "passive");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        String trigger = event.getPlayer().isSneaking()
                ? (action.isRightClick() ? "shift_right_click" : "shift_left_click")
                : (action.isRightClick() ? "right_click" : "left_click");
        cast(event.getPlayer(), null, trigger);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        Player player = attacker(event);
        if (player == null || executingDamage.contains(player.getUniqueId())) return;
        cast(player, event.getEntity() instanceof LivingEntity living ? living : null, "damage_dealt");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageTaken(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ShieldState shield = shields.get(player.getUniqueId());
        if (shield != null) {
            if (shield.expiresAt() <= System.currentTimeMillis() || shield.amount() <= 0) shields.remove(player.getUniqueId());
            else {
                double absorbed = Math.min(event.getDamage(), shield.amount());
                event.setDamage(event.getDamage() - absorbed);
                double remaining = shield.amount() - absorbed;
                if (remaining <= 0) shields.remove(player.getUniqueId()); else shields.put(player.getUniqueId(), new ShieldState(remaining, shield.expiresAt()));
            }
        }
        CastingState state = casting.get(player.getUniqueId());
        if (state != null && state.interruptible()) interrupt(player, "施法被攻擊打斷。");
        cast(player, null, "damage_taken");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) cast(killer, event.getEntity(), "kill");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) { cast(event.getPlayer(), null, "block_break"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        LivingEntity hit = event.getHitEntity() instanceof LivingEntity living ? living : null;
        ProjectileState state = skillProjectiles.remove(event.getEntity().getUniqueId());
        if (state != null) {
            PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
            RpgEngineRegistry.SkillDefinition skill = registry.get().skills().get(state.skillKey());
            if (profile != null && skill != null) nestedEffects(state.effect(), "effects").forEach(effect -> applyEffect(player, hit, profile, skill, effect));
        }
        cast(player, hit, "projectile_hit");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        CastingState state = casting.get(event.getPlayer().getUniqueId());
        if (state == null || !state.interruptible() || event.getTo() == null) return;
        if (event.getFrom().distanceSquared(event.getTo()) > 0.01) interrupt(event.getPlayer(), "移動中斷了施法。");
    }

    private Player attacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    private void cast(Player player, LivingEntity target, String trigger) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return;
        RpgEngineRegistry snapshot = registry.get();
        RpgEngineRegistry.ClassDefinition playerClass = snapshot.classes().get(profile.getClassId());
        if (playerClass == null) return;
        for (String skillKey : playerClass.skills()) {
            RpgEngineRegistry.SkillDefinition skill = snapshot.skills().get(skillKey);
            if (skill == null || !skill.trigger().equals(trigger) || !isUnlocked(snapshot, profile, skillKey)
                    || !conditionsMet(player, target, profile, skill)) continue;
            long now = System.currentTimeMillis();
            long readyAt = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>()).getOrDefault(skill.key(), 0L);
            if (readyAt > now || profile.getCurrentMana() < skill.manaCost()) continue;
            profile.setCurrentMana(profile.getCurrentMana() - skill.manaCost());
            cooldowns.get(player.getUniqueId()).put(skill.key(), now + Math.round(skill.cooldownSeconds() * 1000));
            beginCast(player, target, profile, skill);
            awardSkillXp(player, profile, skill);
        }
    }

    boolean castManual(Player player, String skillKey) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return false;
        RpgEngineRegistry snapshot = registry.get();
        RpgEngineRegistry.ClassDefinition playerClass = snapshot.classes().get(profile.getClassId());
        String normalized = skillKey.toLowerCase(Locale.ROOT);
        RpgEngineRegistry.SkillDefinition skill = snapshot.skills().get(normalized);
        if (playerClass == null || skill == null || !playerClass.skills().contains(normalized) || !isUnlocked(snapshot, profile, normalized)) return false;
        LivingEntity target = targetedEntity(player, 32);
        if (!conditionsMet(player, target, profile, skill)) return false;
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
        if (playerCooldowns.getOrDefault(skill.key(), 0L) > now || profile.getCurrentMana() < skill.manaCost()) return false;
        profile.setCurrentMana(profile.getCurrentMana() - skill.manaCost());
        playerCooldowns.put(skill.key(), now + Math.round(skill.cooldownSeconds() * 1000));
        beginCast(player, target, profile, skill);
        awardSkillXp(player, profile, skill);
        return true;
    }

    private void awardSkillXp(Player player, PlayerProfile profile, RpgEngineRegistry.SkillDefinition skill) {
        double gained = plugin.getConfig().getDouble("class-skills.leveling.xp-per-cast", 5.0);
        profile.addClassSkillXp(skill.key(), gained, player);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().saveProfile(profile));
    }

    private LivingEntity targetedEntity(Player player, double range) {
        var hit = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range, 0.5,
                entity -> entity instanceof LivingEntity && !entity.equals(player));
        return hit != null && hit.getHitEntity() instanceof LivingEntity living ? living : null;
    }

    private void beginCast(Player player, LivingEntity target, PlayerProfile profile, RpgEngineRegistry.SkillDefinition skill) {
        Runnable execute = () -> {
            casting.remove(player.getUniqueId());
            for (int repeat = 0; repeat < skill.repeats(); repeat++) {
                int delay = (int) Math.round(repeat * skill.repeatIntervalSeconds() * 20);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    for (JsonObject effect : skill.effects()) applyEffect(player, target, profile, skill, effect);
                }, delay);
            }
        };
        if (skill.castTimeSeconds() <= 0) { execute.run(); return; }
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, execute, Math.max(1, Math.round(skill.castTimeSeconds() * 20))).getTaskId();
        casting.put(player.getUniqueId(), new CastingState(taskId, skill.interruptible()));
        player.sendActionBar(Component.text("施法中：" + skill.name(), NamedTextColor.AQUA));
    }

    private void interrupt(Player player, String message) {
        CastingState removed = casting.remove(player.getUniqueId());
        if (removed == null) return;
        Bukkit.getScheduler().cancelTask(removed.taskId());
        player.sendActionBar(Component.text(message, NamedTextColor.RED));
    }

    private boolean isUnlocked(RpgEngineRegistry snapshot, PlayerProfile profile, String skillKey) {
        boolean appearsInTree = false;
        for (RpgEngineRegistry.TreeDefinition tree : snapshot.skillTrees().values()) {
            if (tree.classId() != null && !tree.classId().equals(profile.getClassId())) continue;
            for (RpgEngineRegistry.TreeNode node : tree.nodes()) {
                if (!node.skill().equals(skillKey)) continue;
                appearsInTree = true;
                if (profile.getUnlockedSkillNodes().contains(tree.key() + ":" + node.key())) return true;
            }
        }
        return !appearsInTree;
    }

    private boolean conditionsMet(Player player, LivingEntity target, PlayerProfile profile, RpgEngineRegistry.SkillDefinition skill) {
        for (JsonObject condition : skill.conditions()) {
            String type = condition.get("type").getAsString().toLowerCase(Locale.ROOT);
            boolean result = switch (type) {
                case "class" -> profile.getClassId().equals(text(condition, "value", ""));
                case "min_level" -> profile.getCharacterLevel() >= decimal(condition, "value", 1);
                case "permission" -> player.hasPermission(text(condition, "value", ""));
                case "sneaking" -> player.isSneaking() == bool(condition, "value", true);
                case "health_below" -> player.getHealth() / player.getMaxHealth() <= decimal(condition, "value", 1);
                case "chance" -> ThreadLocalRandom.current().nextDouble() <= decimal(condition, "value", 1);
                case "weapon_type" -> player.getInventory().getItemInMainHand().getType().name().toLowerCase(Locale.ROOT)
                        .endsWith(text(condition, "value", "").toLowerCase(Locale.ROOT));
                case "target_type" -> target != null && target.getType().getKey().toString().equals(text(condition, "value", ""));
                default -> false;
            };
            if (condition.has("negate") && condition.get("negate").getAsBoolean()) result = !result;
            if (!result) return false;
        }
        return true;
    }

    private void applyEffect(Player player, LivingEntity target, PlayerProfile profile,
                             RpgEngineRegistry.SkillDefinition skill, JsonObject effect) {
        String type = effect.get("type").getAsString().toLowerCase(Locale.ROOT);
        LivingEntity recipient = "self".equals(text(effect, "target", "target")) || target == null ? player : target;
        switch (type) {
            case "damage" -> {
                if (target == null) return;
                double damage = Math.max(0, scaledFormula(effect, profile, player, skill) * profile.getMagicDamageMultiplier());
                executingDamage.add(player.getUniqueId());
                try { target.damage(damage, player); } finally { executingDamage.remove(player.getUniqueId()); }
            }
            case "heal" -> recipient.setHealth(Math.min(recipient.getMaxHealth(), recipient.getHealth() + Math.max(0, scaledFormula(effect, profile, player, skill))));
            case "mana" -> profile.setCurrentMana(profile.getCurrentMana() + scaledFormula(effect, profile, player, skill));
            case "message" -> player.sendMessage(Component.text(text(effect, "value", skill.name()), NamedTextColor.AQUA));
            case "potion" -> applyPotion(recipient, effect);
            case "sound" -> playSound(player, effect);
            case "particle" -> spawnParticle(recipient, effect);
            case "knockback" -> knockback(player, recipient, effect);
            case "fire" -> recipient.setFireTicks(Math.max(0, (int) Math.round(decimal(effect, "seconds", 3) * 20)));
            case "area" -> area(player, profile, skill, effect);
            case "beam" -> beam(player, profile, skill, effect);
            case "chain" -> chain(player, target, profile, skill, effect);
            case "projectile" -> projectile(player, skill, effect);
            case "dash" -> player.setVelocity(player.getLocation().getDirection().normalize().multiply(decimal(effect, "power", 1.5)).setY(decimal(effect, "vertical", 0.15)));
            case "teleport" -> teleport(player, effect);
            case "shield" -> shields.put(player.getUniqueId(), new ShieldState(Math.max(0, scaledFormula(effect, profile, player, skill)), System.currentTimeMillis() + Math.round(decimal(effect, "seconds", 5) * 1000)));
            case "damage_over_time" -> periodic(player, recipient, profile, skill, effect, true);
            case "heal_over_time" -> periodic(player, recipient, profile, skill, effect, false);
            case "lifesteal" -> lifesteal(player, target, profile, effect);
            case "summon" -> summon(player, effect);
            case "sequence" -> sequence(player, target, profile, skill, effect);
            case "command" -> {
                if (!plugin.getConfig().getBoolean("skills.allow-console-command-effects", false)) return;
                String command = text(effect, "value", "").replace("{player}", player.getName());
                if (!command.isBlank() && !command.contains("\n") && !command.contains("\r")) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
            default -> { }
        }
    }

    private void area(Player player, PlayerProfile profile, RpgEngineRegistry.SkillDefinition skill, JsonObject effect) {
        double radius = bounded(decimal(effect, "radius", 4), 0.5, 32);
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || living.equals(player)) continue;
            for (JsonObject nested : nestedEffects(effect, "effects")) applyEffect(player, living, profile, skill, nested);
        }
    }

    private void beam(Player player, PlayerProfile profile, RpgEngineRegistry.SkillDefinition skill, JsonObject effect) {
        double range = bounded(decimal(effect, "range", 20), 1, 64);
        var hit = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range, 0.5,
                entity -> entity instanceof LivingEntity && !entity.equals(player));
        if (hit != null && hit.getHitEntity() instanceof LivingEntity living) nestedEffects(effect, "effects").forEach(nested -> applyEffect(player, living, profile, skill, nested));
        player.getWorld().spawnParticle(Particle.END_ROD, player.getEyeLocation(), Math.max(1, (int) range), player.getEyeLocation().getDirection().getX(), player.getEyeLocation().getDirection().getY(), player.getEyeLocation().getDirection().getZ(), 0.2);
    }

    private void chain(Player player, LivingEntity first, PlayerProfile profile, RpgEngineRegistry.SkillDefinition skill, JsonObject effect) {
        if (first == null) return; int maximum = Math.max(1, Math.min(20, (int) decimal(effect, "targets", 3))); double radius = bounded(decimal(effect, "radius", 6), 1, 24);
        LivingEntity current = first; Set<UUID> visited = new HashSet<>();
        for (int index = 0; index < maximum && current != null; index++) {
            visited.add(current.getUniqueId()); for (JsonObject nested : nestedEffects(effect, "effects")) applyEffect(player, current, profile, skill, nested);
            LivingEntity origin = current; current = origin.getNearbyEntities(radius, radius, radius).stream().filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                    .filter(value -> !value.equals(player) && !visited.contains(value.getUniqueId())).min(java.util.Comparator.comparingDouble(value -> value.getLocation().distanceSquared(origin.getLocation()))).orElse(null);
        }
    }

    private void projectile(Player player, RpgEngineRegistry.SkillDefinition skill, JsonObject effect) {
        Snowball projectile = player.launchProjectile(Snowball.class, player.getEyeLocation().getDirection().multiply(bounded(decimal(effect, "speed", 1.5), 0.1, 5)));
        projectile.setItem(new org.bukkit.inventory.ItemStack(Material.matchMaterial(text(effect, "material", "SNOWBALL")) == null ? Material.SNOWBALL : Material.matchMaterial(text(effect, "material", "SNOWBALL"))));
        skillProjectiles.put(projectile.getUniqueId(), new ProjectileState(skill.key(), effect.deepCopy()));
    }

    private void teleport(Player player, JsonObject effect) {
        double range = bounded(decimal(effect, "range", 8), 1, 32); Location destination = player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(range));
        var block = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), range);
        if (block != null) destination = block.getHitPosition().toLocation(player.getWorld()).subtract(player.getLocation().getDirection().normalize());
        destination.setYaw(player.getYaw()); destination.setPitch(player.getPitch()); if (destination.getBlock().isPassable()) player.teleport(destination);
    }

    private void periodic(Player source, LivingEntity target, PlayerProfile profile, RpgEngineRegistry.SkillDefinition skill, JsonObject effect, boolean damage) {
        int pulses = Math.max(1, Math.min(100, (int) decimal(effect, "pulses", 5))); long interval = Math.max(1, Math.round(decimal(effect, "interval_seconds", 1) * 20));
        for (int pulse = 0; pulse < pulses; pulse++) Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isValid() || target.isDead()) return; JsonObject nested = new JsonObject(); nested.addProperty("type", damage ? "damage" : "heal"); nested.addProperty("value", formula(effect, profile, source)); applyEffect(source, target, profile, skill, nested);
        }, pulse * interval);
    }

    private void lifesteal(Player player, LivingEntity target, PlayerProfile profile, JsonObject effect) {
        if (target == null) return; double damage = Math.max(0, formula(effect, profile, player)); executingDamage.add(player.getUniqueId());
        try { target.damage(damage, player); } finally { executingDamage.remove(player.getUniqueId()); }
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + damage * bounded(decimal(effect, "ratio", 0.5), 0, 1)));
    }

    private void summon(Player player, JsonObject effect) {
        EntityType type; try { type = EntityType.valueOf(text(effect, "entity", "WOLF").toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) { return; }
        int amount = Math.max(1, Math.min(10, (int) decimal(effect, "amount", 1))); for (int i = 0; i < amount; i++) {
            Entity entity = player.getWorld().spawnEntity(player.getLocation(), type); if (entity instanceof org.bukkit.entity.Tameable tameable) { tameable.setOwner(player); tameable.setTamed(true); }
            long lifetime = Math.max(20, Math.round(decimal(effect, "seconds", 20) * 20)); Bukkit.getScheduler().runTaskLater(plugin, () -> { if (entity.isValid()) entity.remove(); }, lifetime);
        }
    }

    private void sequence(Player player, LivingEntity target, PlayerProfile profile, RpgEngineRegistry.SkillDefinition skill, JsonObject effect) {
        List<JsonObject> effects = nestedEffects(effect, "effects"); long interval = Math.max(1, Math.round(decimal(effect, "interval_seconds", 0.25) * 20));
        for (int i = 0; i < effects.size(); i++) { JsonObject nested = effects.get(i); Bukkit.getScheduler().runTaskLater(plugin, () -> applyEffect(player, target, profile, skill, nested), i * interval); }
    }

    private List<JsonObject> nestedEffects(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) return List.of(); List<JsonObject> result = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) if (element.isJsonObject()) result.add(element.getAsJsonObject()); return result;
    }

    static double formula(JsonObject effect, PlayerProfile profile, Player player) {
        double base = decimal(effect, "value", decimal(effect, "base", 0));
        double result = base + profile.getCharacterLevel() * decimal(effect, "level_scale", 0)
                + profile.getIntelligence() * decimal(effect, "intelligence_scale", 0)
                + profile.getStrength() * decimal(effect, "strength_scale", 0)
                + (1 - player.getHealth() / Math.max(1, player.getMaxHealth())) * decimal(effect, "missing_health_scale", 0);
        return bounded(result, decimal(effect, "min", -1_000_000), decimal(effect, "max", 1_000_000));
    }
    private double scaledFormula(JsonObject effect, PlayerProfile profile, Player player, RpgEngineRegistry.SkillDefinition skill) {
        return formula(effect, profile, player) + profile.getClassSkillLevel(skill.key()) * decimal(effect, "skill_level_scale", 0);
    }

    private static double bounded(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private void applyPotion(LivingEntity target, JsonObject effect) {
        PotionEffectType type = PotionEffectType.getByName(text(effect, "potion", "speed").toUpperCase(Locale.ROOT));
        if (type == null) return;
        int ticks = Math.max(1, (int) Math.round(decimal(effect, "seconds", 5) * 20));
        int amplifier = Math.max(0, (int) decimal(effect, "amplifier", 0));
        target.addPotionEffect(new PotionEffect(type, ticks, amplifier, false, true, true));
    }

    private void playSound(Player player, JsonObject effect) {
        String raw = text(effect, "sound", "minecraft:entity.experience_orb.pickup").toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(raw.contains(":") ? raw : "minecraft:" + raw.toLowerCase(Locale.ROOT).replace('_', '.'));
        Sound sound = key == null ? null : Registry.SOUNDS.get(key);
        if (sound != null) player.playSound(player.getLocation(), sound, (float) decimal(effect, "volume", 1), (float) decimal(effect, "pitch", 1));
    }

    private void spawnParticle(LivingEntity target, JsonObject effect) {
        try {
            Particle particle = Particle.valueOf(text(effect, "particle", "CRIT").toUpperCase(Locale.ROOT));
            target.getWorld().spawnParticle(particle, target.getLocation().add(0, 1, 0), Math.max(1, (int) decimal(effect, "count", 10)));
        } catch (IllegalArgumentException ignored) { }
    }

    private void knockback(Player source, LivingEntity target, JsonObject effect) {
        Vector direction = target.getLocation().toVector().subtract(source.getLocation().toVector());
        if (direction.lengthSquared() == 0) direction = source.getLocation().getDirection();
        target.setVelocity(direction.normalize().multiply(decimal(effect, "power", 1)).setY(decimal(effect, "vertical", 0.3)));
    }

    private static String text(JsonObject object, String key, String fallback) {
        return object.has(key) ? object.get(key).getAsString() : fallback;
    }
    private static double decimal(JsonObject object, String key, double fallback) {
        return object.has(key) ? object.get(key).getAsDouble() : fallback;
    }
    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) ? object.get(key).getAsBoolean() : fallback;
    }
    private record CastingState(int taskId, boolean interruptible) {}
    private record ShieldState(double amount, long expiresAt) {}
    private record ProjectileState(String skillKey, JsonObject effect) {}
}
