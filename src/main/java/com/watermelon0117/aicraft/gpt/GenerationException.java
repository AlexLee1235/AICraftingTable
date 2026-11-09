package com.watermelon0117.aicraft.gpt;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;

import java.net.http.HttpTimeoutException;
import java.util.Map;

/** A failure to make something, with a message written for the player, which their own game shows in its language. It is the only kind of failure whose own text they are shown. */
public class GenerationException extends RuntimeException {
    private static final String UNEXPECTED = "error.aicraft.unexpected";
    private static final String SERVER_PROBLEM = "error.aicraft.server_problem";
    /** The texture server's fixed sentences. Released mods show them as they are, so they cannot be reworded there, and here they say which message to show. */
    private static final Map<String, String> SERVER_SENTENCES = Map.of(
            "The system-wide usage limit has been reached. Please try again tomorrow or use your own OpenAI account", "error.aicraft.limit_global",
            "Your daily usage quota exceeded. Please try again tomorrow or use your own OpenAI account", "error.aicraft.limit_user",
            "Too many request from your IP. Please try again tomorrow or use your own OpenAI account", "error.aicraft.limit_ip",
            "The AI could not decide whether this is a block. Please try again.", "error.aicraft.bad_answer");

    private final Component playerMessage;

    /** The log gets the translation key and arguments, whatever language the game is in. */
    public GenerationException(Component playerMessage) {
        super(playerMessage.toString());
        this.playerMessage = playerMessage;
    }

    public GenerationException(Component playerMessage, Throwable cause) {
        super(playerMessage.toString(), cause);
        this.playerMessage = playerMessage;
    }

    /** What to tell the player about any failure. */
    public static Component describe(Throwable error) {
        return describe(error, Component.translatable(UNEXPECTED));
    }

    /** @param otherwise what to say when nothing in the failure was written for the player */
    public static Component describe(Throwable error, Component otherwise) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof GenerationException generation) {
                return generation.playerMessage;
            }
        }
        return otherwise;
    }

    /** For a request that the texture server never answered. */
    public static GenerationException serverNoAnswer(Throwable error) {
        return new GenerationException(Component.translatable(timedOut(error) ? "error.aicraft.timeout_server" : "error.aicraft.connect_server"), error);
    }

    /** For a request that OpenAI never answered. */
    public static GenerationException openAINoAnswer(Throwable error) {
        return new GenerationException(Component.translatable(timedOut(error) ? "error.aicraft.timeout_openai" : "error.aicraft.connect_openai"), error);
    }

    private static boolean timedOut(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /** For an answer from the texture server other than 200. Its text is written for players, unless it is a crash, a hosting page or what OpenAI said to it. */
    public static GenerationException fromServer(String body) {
        String text = body.trim();
        if (text.startsWith("{") || text.startsWith("OpenAI error ")) {
            String said = openAISaid(text);
            return new GenerationException(said == null ? Component.translatable(SERVER_PROBLEM) : quoteOpenAI(said));
        }
        if (text.isEmpty() || text.startsWith("<") || text.startsWith("INTERNAL_SERVER_ERROR")) {
            return new GenerationException(Component.translatable(SERVER_PROBLEM));
        }
        String sentence = text.replace("\\n", "\n").replace("\\", "");
        String key = SERVER_SENTENCES.get(sentence);
        return new GenerationException(key == null ? Component.literal(sentence) : Component.translatable(key));
    }

    /** For an answer from OpenAI other than 200. */
    public static GenerationException fromOpenAI(int status, String body) {
        String said = openAISaid(body);
        return new GenerationException(said == null ? Component.translatable("error.aicraft.openai_status", String.valueOf(status)) : quoteOpenAI(said));
    }

    /** OpenAI's sentence stays in English: translating it would take another paid request. */
    private static Component quoteOpenAI(String said) {
        return Component.literal("OpenAI: " + said);
    }

    /** @return the sentence in OpenAI's error JSON, which may follow other text, or null if there is none */
    private static String openAISaid(String body) {
        int start = body.indexOf('{');
        if (start < 0) {
            return null;
        }
        try {
            JsonElement error = JsonParser.parseString(body.substring(start)).getAsJsonObject().get("error");
            JsonElement message = error != null && error.isJsonObject() ? error.getAsJsonObject().get("message") : null;
            return message != null && message.isJsonPrimitive() && !message.getAsString().isBlank() ? message.getAsString() : null;
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }
    }
}
