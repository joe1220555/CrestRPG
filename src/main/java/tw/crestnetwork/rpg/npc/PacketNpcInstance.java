package tw.crestnetwork.rpg.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import tw.crestnetwork.rpg.CrestRpgPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketNpcInstance {
    private final CrestRpgPlugin plugin;
    private final RpgNpcDefinition definition;
    private final Location location;
    private final Set<UUID> visiblePlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> greetingCooldowns = new ConcurrentHashMap<>();
    private final List<ArmorStand> hologramStands = Collections.synchronizedList(new ArrayList<>());

    public PacketNpcInstance(CrestRpgPlugin plugin, RpgNpcDefinition definition) {
        this.plugin = plugin;
        this.definition = definition;
        World w = Bukkit.getWorld(definition.world());
        this.location = w != null ? new Location(w, definition.x(), definition.y(), definition.z(), definition.yaw(), definition.pitch())
                : new Location(Bukkit.getWorlds().get(0), definition.x(), definition.y(), definition.z(), definition.yaw(), definition.pitch());
    }

    public RpgNpcDefinition getDefinition() {
        return definition;
    }

    public Location getLocation() {
        return location.clone();
    }

    public boolean tryGreet(Player player) {
        long now = System.currentTimeMillis();
        Long last = greetingCooldowns.get(player.getUniqueId());
        if (last != null && (now - last) < 15000) {
            return false;
        }
        greetingCooldowns.put(player.getUniqueId(), now);
        return true;
    }

    public void spawnHolograms() {
        despawnHolograms();
        if (definition.holograms().isEmpty()) return;

        World world = location.getWorld();
        if (world == null) return;

        double yOffset = 1.9 + (definition.holograms().size() * 0.28);
        for (String text : definition.holograms()) {
            Location loc = location.clone().add(0, yOffset, 0);
            yOffset -= 0.28;

            ArmorStand stand = world.spawn(loc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setMarker(true);
                as.setCustomNameVisible(true);
                Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
                as.customName(comp);
            });
            hologramStands.add(stand);
        }
    }

    public void despawnHolograms() {
        for (ArmorStand stand : hologramStands) {
            if (stand != null && stand.isValid()) {
                stand.remove();
            }
        }
        hologramStands.clear();
    }

    public boolean isNearby(Player player) {
        if (!player.getWorld().equals(location.getWorld())) return false;
        return player.getLocation().distanceSquared(location) <= 2304; // Within 48 blocks
    }

    public boolean isInGreetingRange(Player player) {
        if (!player.getWorld().equals(location.getWorld())) return false;
        double radius = definition.greetingRadius() > 0 ? definition.greetingRadius() : 6.0;
        return player.getLocation().distanceSquared(location) <= (radius * radius);
    }

    public boolean isVisibleTo(Player player) {
        return visiblePlayers.contains(player.getUniqueId());
    }

    public void showTo(Player player) {
        visiblePlayers.add(player.getUniqueId());
    }

    public void hideFrom(Player player) {
        visiblePlayers.remove(player.getUniqueId());
    }

    public void destroy() {
        despawnHolograms();
        visiblePlayers.clear();
        greetingCooldowns.clear();
    }
}
