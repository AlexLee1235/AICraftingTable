package com.watermelon0117.aicraft.gpt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** A key missing from a language file would show the player the key itself, and one that loses a %s would lose what fills it. */
class LangFilesTest {
    private static final Path LANG = Path.of("src/main/resources/assets/aicraft/lang");
    private static final Path CODE = Path.of("src/main/java");
    private static final List<String> TRANSLATED = List.of("zh_cn", "zh_tw", "ru_ru", "hu_hu", "uk_ua");
    private static final String PREFIX = "error.aicraft.";
    private static final Pattern KEY_IN_CODE = Pattern.compile("\"(error\\.aicraft\\.[a-z_]+)\"");

    private static Map<String, String> errors(String language) throws IOException {
        JsonObject file = JsonParser.parseString(Files.readString(LANG.resolve(language + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, String> found = new TreeMap<>();
        file.keySet().stream().filter(key -> key.startsWith(PREFIX)).forEach(key -> found.put(key, file.get(key).getAsString()));
        return found;
    }

    private static long placeholders(String text) {
        return Pattern.compile("%s").matcher(text).results().count();
    }

    @Test
    void everyLanguageHasEveryErrorAndNothingElse() throws IOException {
        Set<String> english = errors("en_us").keySet();
        for (String language : TRANSLATED) {
            assertEquals(english, errors(language).keySet(), language);
            JsonObject file = JsonParser.parseString(Files.readString(LANG.resolve(language + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(english, new TreeSet<>(file.keySet()), language);
        }
    }

    @Test
    void everyTranslationKeepsWhatFillsIt() throws IOException {
        Map<String, String> english = errors("en_us");
        for (String language : TRANSLATED) {
            for (Map.Entry<String, String> entry : errors(language).entrySet()) {
                assertEquals(placeholders(english.get(entry.getKey())), placeholders(entry.getValue()), language + " " + entry.getKey());
                assertFalse(entry.getValue().replace("%s", "").contains("%"), language + " " + entry.getKey());
                assertFalse(entry.getValue().isBlank(), language + " " + entry.getKey());
            }
        }
    }

    @Test
    void nothingWasLeftInEnglish() throws IOException {
        Map<String, String> english = errors("en_us");
        for (String language : TRANSLATED) {
            for (Map.Entry<String, String> entry : errors(language).entrySet()) {
                assertNotEquals(english.get(entry.getKey()), entry.getValue(), language + " " + entry.getKey());
            }
        }
    }

    @Test
    void theCodeAndTheEnglishFileNameTheSameErrors() throws IOException {
        Set<String> used = new TreeSet<>();
        try (Stream<Path> files = Files.walk(CODE)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = KEY_IN_CODE.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    used.add(matcher.group(1));
                }
            }
        }
        assertEquals(errors("en_us").keySet(), used);
    }
}
