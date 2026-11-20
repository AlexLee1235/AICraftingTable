package com.watermelon0117.aicraft.common;

import com.mojang.logging.LogUtils;
import com.watermelon0117.aicraft.AICraftingTable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Before 2.0 the API key was saved as "OpanAI API Key". Forge deletes every key its spec does not have when it loads the
 * file, which would throw away a player's key, so the old name is renamed first, while the mod is constructed: Forge
 * loads common configs after that.
 */
public final class MisspelledConfigKey {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String FILE = AICraftingTable.MODID + "-common.toml";
    private static final Pattern OLD_KEY = Pattern.compile("^([ \\t]*)([\"'])OpanAI API Key\\2(?=[ \\t]*=)", Pattern.MULTILINE);
    private static final Pattern NEW_KEY = Pattern.compile("^[ \\t]*([\"'])" + Pattern.quote(AICraftingTableCommonConfigs.OPENAI_API_KEY_NAME) + "\\1[ \\t]*=", Pattern.MULTILINE);
    private static final Pattern OLD_COMMENT = Pattern.compile("^([ \\t]*)#Add APIKEY to use your own OpanAI account[ \\t]*(?=\\r?$)", Pattern.MULTILINE);

    private MisspelledConfigKey() {
    }

    public static void fix(Path configDir) {
        Path file = configDir.resolve(FILE);
        try {
            if (rename(file)) {
                LOGGER.info("Renamed the misspelled OpenAI API key setting in {}", file);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not rename the misspelled OpenAI API key setting in {}", file, e);
        }
    }

    /** Returns whether the file was changed. A file that already has the new name is left alone. */
    static boolean rename(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return false;
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        if (!OLD_KEY.matcher(text).find() || NEW_KEY.matcher(text).find()) {
            return false;
        }
        String fixed = OLD_KEY.matcher(text).replaceFirst("$1\"" + Matcher.quoteReplacement(AICraftingTableCommonConfigs.OPENAI_API_KEY_NAME) + "\"");
        fixed = OLD_COMMENT.matcher(fixed).replaceFirst("$1#" + Matcher.quoteReplacement(AICraftingTableCommonConfigs.OPENAI_API_KEY_COMMENT));
        Files.writeString(file, fixed, StandardCharsets.UTF_8);
        return true;
    }
}
