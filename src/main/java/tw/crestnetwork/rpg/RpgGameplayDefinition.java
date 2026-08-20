package tw.crestnetwork.rpg;

import com.google.gson.JsonObject;

record RpgGameplayDefinition(
        long revisionId,
        int version,
        String section,
        String name,
        JsonObject data,
        boolean enabled
) {}
