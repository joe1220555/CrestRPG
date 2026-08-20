package tw.crestnetwork.rpg.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public record RpgNpcDefinition(
        String id,
        String displayName,
        String entityType,
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String skinName,
        String skinValue,
        String skinSignature,
        List<String> holograms,
        String interactionType, // CRAFTING_STATION, CLASS_TRAINER, QUEST_GIVER, DIALOGUE, SHOP, TRADER
        String targetId,
        List<String> dialogues,
        List<TradeOffer> trades,
        String greetingText,
        String greetingSound,
        String greetingAction, // SWING_ARM, HAPPY_PARTICLE, HEART_PARTICLE, BOW
        double greetingRadius,
        boolean headLook,
        boolean enabled
) {
    public record TradeOffer(
            String buy1Material,
            int buy1Amount,
            String buy2Material,
            int buy2Amount,
            String resultMaterial,
            int resultAmount,
            String resultItemKey,
            int maxUses
    ) {}

    public static RpgNpcDefinition fromJson(String id, JsonObject json) {
        String name = json.has("display_name") ? json.get("display_name").getAsString() : id;
        String type = json.has("entity_type") ? json.get("entity_type").getAsString() : "PLAYER";
        String world = json.has("world") ? json.get("world").getAsString() : "world";
        double x = json.has("x") ? json.get("x").getAsDouble() : 0.0;
        double y = json.has("y") ? json.get("y").getAsDouble() : 64.0;
        double z = json.has("z") ? json.get("z").getAsDouble() : 0.0;
        float yaw = json.has("yaw") ? json.get("yaw").getAsFloat() : 0.0f;
        float pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 0.0f;

        String skinName = json.has("skin_name") ? json.get("skin_name").getAsString() : null;
        String skinVal = json.has("skin_value") ? json.get("skin_value").getAsString() : null;
        String skinSig = json.has("skin_signature") ? json.get("skin_signature").getAsString() : null;

        List<String> holograms = json.has("holograms") ?
                json.getAsJsonArray("holograms").asList().stream().map(JsonElement::getAsString).toList() : List.of();

        String interaction = json.has("interaction_type") ? json.get("interaction_type").getAsString() : "DIALOGUE";
        String targetId = json.has("target_id") ? json.get("target_id").getAsString() : "";

        List<String> dialogues = json.has("dialogues") ?
                json.getAsJsonArray("dialogues").asList().stream().map(JsonElement::getAsString).toList() : List.of();

        List<TradeOffer> trades = new ArrayList<>();
        if (json.has("trades")) {
            JsonArray array = json.getAsJsonArray("trades");
            for (JsonElement elem : array) {
                if (elem.isJsonObject()) {
                    JsonObject t = elem.getAsJsonObject();
                    String b1 = t.has("buy_1") ? t.get("buy_1").getAsString() : "EMERALD";
                    int b1Count = t.has("buy_1_amount") ? t.get("buy_1_amount").getAsInt() : 1;
                    String b2 = t.has("buy_2") ? t.get("buy_2").getAsString() : null;
                    int b2Count = t.has("buy_2_amount") ? t.get("buy_2_amount").getAsInt() : 0;
                    String res = t.has("result") ? t.get("result").getAsString() : "DIAMOND";
                    int resCount = t.has("result_amount") ? t.get("result_amount").getAsInt() : 1;
                    String itemKey = t.has("result_item_key") ? t.get("result_item_key").getAsString() : null;
                    int max = t.has("max_uses") ? t.get("max_uses").getAsInt() : 9999;
                    trades.add(new TradeOffer(b1, b1Count, b2, b2Count, res, resCount, itemKey, max));
                }
            }
        }

        String greetingText = json.has("greeting_text") ? json.get("greeting_text").getAsString() : null;
        String greetingSound = json.has("greeting_sound") ? json.get("greeting_sound").getAsString() : "entity.villager.yes";
        String greetingAction = json.has("greeting_action") ? json.get("greeting_action").getAsString() : "HAPPY_PARTICLE";
        double greetingRadius = json.has("greeting_radius") ? json.get("greeting_radius").getAsDouble() : 6.0;

        boolean headLook = !json.has("head_look") || json.get("head_look").getAsBoolean();
        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();

        return new RpgNpcDefinition(id, name, type, world, x, y, z, yaw, pitch, skinName, skinVal, skinSig, holograms, interaction, targetId, dialogues, trades, greetingText, greetingSound, greetingAction, greetingRadius, headLook, enabled);
    }
}
