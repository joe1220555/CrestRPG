package tw.crestnetwork.rpg.editor;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.function.Consumer;

public final class EditorSession {
    private final UUID playerId;
    private String currentKind;
    private String currentKey;
    private JsonObject editingDraft;
    private Consumer<String> activeChatInputHandler;
    private String inputPromptMessage;

    public EditorSession(Player player) {
        this.playerId = player.getUniqueId();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getCurrentKind() {
        return currentKind;
    }

    public void setCurrentKind(String currentKind) {
        this.currentKind = currentKind;
    }

    public String getCurrentKey() {
        return currentKey;
    }

    public void setCurrentKey(String currentKey) {
        this.currentKey = currentKey;
    }

    public JsonObject getEditingDraft() {
        return editingDraft;
    }

    public void setEditingDraft(JsonObject editingDraft) {
        this.editingDraft = editingDraft;
    }

    public boolean isExpectingChatInput() {
        return activeChatInputHandler != null;
    }

    public void awaitChatInput(String promptMessage, Consumer<String> handler) {
        this.inputPromptMessage = promptMessage;
        this.activeChatInputHandler = handler;
    }

    public String getInputPromptMessage() {
        return inputPromptMessage;
    }

    public boolean processChatInput(String input) {
        if (activeChatInputHandler == null) return false;
        Consumer<String> handler = activeChatInputHandler;
        clearChatInput();
        handler.accept(input);
        return true;
    }

    public void clearChatInput() {
        this.activeChatInputHandler = null;
        this.inputPromptMessage = null;
    }
}
