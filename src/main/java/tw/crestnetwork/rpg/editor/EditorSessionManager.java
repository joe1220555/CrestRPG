package tw.crestnetwork.rpg.editor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EditorSessionManager implements Listener {
    private final Plugin plugin;
    private final RpgDraftManager draftManager;
    private final Map<UUID, EditorSession> sessions = new ConcurrentHashMap<>();

    public EditorSessionManager(Plugin plugin, RpgDraftManager draftManager) {
        this.plugin = plugin;
        this.draftManager = draftManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public EditorSession getOrCreateSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), k -> new EditorSession(player));
    }

    public EditorSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public RpgDraftManager getDraftManager() {
        return draftManager;
    }

    public void removeSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        EditorSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.isExpectingChatInput()) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("取消")) {
            session.clearChatInput();
            player.sendMessage(Component.text("[RPG 編輯器] 已取消輸入。", NamedTextColor.YELLOW));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> session.processChatInput(message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }
}
