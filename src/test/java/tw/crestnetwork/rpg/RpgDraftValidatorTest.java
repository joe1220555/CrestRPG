package tw.crestnetwork.rpg;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tw.crestnetwork.rpg.editor.RpgDraftManager;
import tw.crestnetwork.rpg.editor.RpgDraftValidator;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

final class RpgDraftValidatorTest {

    @TempDir
    File tempDir;

    private RpgDraftManager draftManager;

    @BeforeEach
    void setUp() {
        draftManager = new RpgDraftManager(tempDir);
    }

    @Test
    void testValidDraftsValidationPasses() {
        JsonObject item = new JsonObject();
        item.addProperty("key", "iron_ingot");
        item.addProperty("base_item", "minecraft:iron_ingot");
        draftManager.saveDraft("items", "iron_ingot", item);

        RpgDraftValidator.ValidationResult result = RpgDraftValidator.validate(draftManager);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testInvalidKeyFormatFailsValidation() {
        JsonObject item = new JsonObject();
        item.addProperty("key", "INVALID KEY!!");
        item.addProperty("base_item", "minecraft:iron_ingot");
        draftManager.saveDraft("items", "INVALID KEY!!", item);

        RpgDraftValidator.ValidationResult result = RpgDraftValidator.validate(draftManager);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void testMissingReferenceFailsValidation() {
        JsonObject item = new JsonObject();
        item.addProperty("key", "dragon_sword");
        item.addProperty("base_item", "minecraft:diamond_sword");
        item.addProperty("set_id", "non_existent_set");
        draftManager.saveDraft("items", "dragon_sword", item);

        RpgDraftValidator.ValidationResult result = RpgDraftValidator.validate(draftManager);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("non_existent_set")));
    }

    @Test
    void testWeaponRarityReferenceIsValidated() {
        JsonObject weapon = new JsonObject();
        weapon.addProperty("key", "ember_blade");
        weapon.addProperty("base_item", "minecraft:diamond_sword");
        weapon.addProperty("rarity", "missing_epic");
        draftManager.saveDraft("weapons", "ember_blade", weapon);

        RpgDraftValidator.ValidationResult result = RpgDraftValidator.validate(draftManager);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("missing_epic")));
    }
}
