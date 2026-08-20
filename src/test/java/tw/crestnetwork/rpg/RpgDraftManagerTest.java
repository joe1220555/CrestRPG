package tw.crestnetwork.rpg;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tw.crestnetwork.rpg.editor.RpgDraftManager;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

final class RpgDraftManagerTest {

    @TempDir
    File tempDir;

    private RpgDraftManager draftManager;

    @BeforeEach
    void setUp() {
        draftManager = new RpgDraftManager(tempDir);
    }

    @Test
    void testSaveAndLoadDraft() {
        JsonObject draft = new JsonObject();
        draft.addProperty("key", "test_item");
        draft.addProperty("name", "測試物品");
        draftManager.saveDraft("items", "test_item", draft);

        JsonObject loaded = draftManager.getDraft("items", "test_item");
        assertNotNull(loaded);
        assertEquals("測試物品", loaded.get("name").getAsString());
    }

    @Test
    void testDeleteDraft() {
        JsonObject draft = new JsonObject();
        draft.addProperty("key", "test_gem");
        draftManager.saveDraft("gems", "test_gem", draft);

        assertTrue(draftManager.deleteDraft("gems", "test_gem"));
        assertNull(draftManager.getDraft("gems", "test_gem"));
    }
}
