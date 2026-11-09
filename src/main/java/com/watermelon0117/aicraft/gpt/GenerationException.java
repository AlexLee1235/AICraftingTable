package com.watermelon0117.aicraft.gpt;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.net.http.HttpTimeoutException;

/** A failure to make something, with a message written for the player. It is the only kind of failure whose own text they are shown. */
public class GenerationException extends RuntimeException {
    private static final String UNEXPECTED = "Something went wrong. Please try again. The game log has the details.";
    private static final String SERVER_PROBLEM = "The server had a problem. Please try again later.";

    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    /** What to tell the player about any failure. */
    public static String describe(Throwable error) {
        return describe(error, UNEXPECTED);
    }

    /** @param otherwise what to say when nothing in the failure was written for the player */
    public static String describe(Throwable error, String otherwise) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof GenerationException) {
                return current.getMessage();
            }
        }
        return otherwise;
    }

    /** For a request that {@code service} never answered. */
    public static GenerationException noAnswer(String service, Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof HttpTimeoutException) {
                return new GenerationException("Timed out waiting for " + service + ". Please try again.", error);
            }
        }
        return new GenerationException("Unable to connect to " + service + ". Please check your internet connection or try again later.", error);
    }

    /** For an answer from the texture server other than 200. Its text is written for players, unless it is a crash, a hosting page or what OpenAI said to it. */
    public static GenerationException fromServer(String body) {
        String text = body.trim();
        if (text.startsWith("{") || text.startsWith("OpenAI error ")) {
            String said = openAISaid(text);
            return new GenerationException(said == null ? SERVER_PROBLEM : "OpenAI: " + said);
        }
        if (text.isEmpty() || text.startsWith("<") || text.startsWith("INTERNAL_SERVER_ERROR")) {
            return new GenerationException(SERVER_PROBLEM);
        }
        return new GenerationException(text.replace("\\n", "\n").replace("\\", ""));
    }

    /** For an answer from OpenAI other than 200. */
    public static GenerationException fromOpenAI(int status, String body) {
        String said = openAISaid(body);
        return new GenerationException(said == null ? "OpenAI had a problem (HTTP " + status + "). Please try again later." : "OpenAI: " + said);
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
