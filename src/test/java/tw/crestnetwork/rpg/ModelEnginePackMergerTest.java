package tw.crestnetwork.rpg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelEnginePackMergerTest {
    @TempDir Path temporary;

    @Test
    void mergesBaseAndCurrentVersionOverlayWithoutRemovingOraxenAssets() throws Exception {
        Path modelPack = temporary.resolve("ModelEngine/resource pack");
        Path oraxenPack = temporary.resolve("Oraxen/pack");
        Files.createDirectories(modelPack.resolve("assets/modelengine/models"));
        Files.createDirectories(modelPack.resolve("modelengine_26_2/assets/minecraft/shaders/core"));
        Files.createDirectories(oraxenPack.resolve("assets/oraxen/textures"));
        Files.writeString(modelPack.resolve("assets/modelengine/models/monster.json"), "base");
        Files.writeString(modelPack.resolve("modelengine_26_2/assets/minecraft/shaders/core/entity.vsh"), "26.2");
        Files.writeString(oraxenPack.resolve("assets/oraxen/textures/weapon.png"), "keep");

        ModelEnginePackMerger merger = new ModelEnginePackMerger();
        assertTrue(merger.merge(modelPack, oraxenPack, "26.2"));
        assertEquals("base", Files.readString(oraxenPack.resolve("assets/modelengine/models/monster.json")));
        assertEquals("26.2", Files.readString(oraxenPack.resolve("assets/minecraft/shaders/core/entity.vsh")));
        assertEquals("keep", Files.readString(oraxenPack.resolve("assets/oraxen/textures/weapon.png")));
        assertFalse(merger.merge(modelPack, oraxenPack, "26.2"));
    }
}
