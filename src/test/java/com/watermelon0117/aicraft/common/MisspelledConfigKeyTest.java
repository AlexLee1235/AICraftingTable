package com.watermelon0117.aicraft.common;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MisspelledConfigKeyTest {
    /** The file as Forge wrote it before the key was renamed. */
    private static final String OLD = """

            ["Configs for AICraftingTable"]
            \t#Add APIKEY to use your own OpanAI account
            \t"OpanAI API Key" = "sk-test-123"
            \t#Set to true to stop new blocks from being unbreakable. A block that would be unbreakable is made as hard as an iron block instead.
            \t"Forbid Unbreakable Blocks" = true

            """;

    @TempDir
    Path dir;
    private CommentedFileConfig loaded;

    @AfterEach
    void unload() {
        AICraftingTableCommonConfigs.SPEC.setConfig(null);
        if (loaded != null) {
            loaded.close();
        }
    }

    /** Built and handed to the spec the way Forge's ConfigFileTypeHandler and ModConfig do it. */
    private void loadLikeForge(Path file) {
        loaded = CommentedFileConfig.builder(file).sync().preserveInsertionOrder().autosave().writingMode(WritingMode.REPLACE).build();
        loaded.load();
        AICraftingTableCommonConfigs.SPEC.setConfig(loaded);
    }

    private Path write(String text) throws IOException {
        Path file = dir.resolve(MisspelledConfigKey.FILE);
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void forgeDeletesTheOldKeyWhenNothingRenamesIt() throws IOException {
        Path file = write(OLD);
        loadLikeForge(file);
        assertEquals("", AICraftingTableCommonConfigs.OPENAI_API_KEY.get());
        assertFalse(Files.readString(file).contains("sk-test-123"));
    }

    @Test
    void theRenamedFileKeepsTheKeyAndNeedsNoOtherCorrection() throws IOException {
        Path file = write(OLD);
        assertTrue(MisspelledConfigKey.rename(file));
        String renamed = Files.readString(file);
        assertFalse(renamed.contains("OpanAI"));
        assertTrue(renamed.contains("\t#" + AICraftingTableCommonConfigs.OPENAI_API_KEY_COMMENT + "\n"));

        loadLikeForge(file);
        assertEquals("sk-test-123", AICraftingTableCommonConfigs.OPENAI_API_KEY.get());
        assertTrue(AICraftingTableCommonConfigs.FORBID_UNBREAKABLE_BLOCKS.get());
        // Forge saves the file only when it had to correct something.
        assertEquals(renamed, Files.readString(file));
    }

    @Test
    void windowsLineEndingsWork() throws IOException {
        Path file = write(OLD.replace("\n", "\r\n"));
        assertTrue(MisspelledConfigKey.rename(file));
        assertFalse(Files.readString(file).contains("OpanAI"));
        loadLikeForge(file);
        assertEquals("sk-test-123", AICraftingTableCommonConfigs.OPENAI_API_KEY.get());
    }

    @Test
    void aFileWithTheNewNameIsLeftAlone() throws IOException {
        String both = OLD.replace("\t\"Forbid", "\t\"OpenAI API Key\" = \"sk-new\"\n\t\"Forbid");
        Path file = write(both);
        assertFalse(MisspelledConfigKey.rename(file));
        assertEquals(both, Files.readString(file));

        String current = OLD.replace("OpanAI API Key", "OpenAI API Key");
        write(current);
        assertFalse(MisspelledConfigKey.rename(file));
        assertEquals(current, Files.readString(file));
    }

    @Test
    void aMissingFileIsFine() throws IOException {
        assertFalse(MisspelledConfigKey.rename(dir.resolve(MisspelledConfigKey.FILE)));
        MisspelledConfigKey.fix(dir);
        assertFalse(Files.exists(dir.resolve(MisspelledConfigKey.FILE)));
    }
}
