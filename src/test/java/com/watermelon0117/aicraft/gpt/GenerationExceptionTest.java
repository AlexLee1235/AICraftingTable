package com.watermelon0117.aicraft.gpt;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerationExceptionTest {
    private static final Component UNEXPECTED = Component.translatable("error.aicraft.unexpected");
    private static final Component SERVER_PROBLEM = Component.translatable("error.aicraft.server_problem");
    private static final Component OUT_OF_IMAGES = Component.literal("Out of images today.");
    private static final Component BAD_ANSWER = Component.translatable("error.aicraft.bad_answer");
    private static final String NO_ACCESS_JSON = "{\n  \"error\": {\n    \"message\": \"The model `gpt-6.1-terra` does not exist or you do not have access to it.\",\n    \"type\": \"invalid_request_error\",\n    \"param\": null,\n    \"code\": \"model_not_found\"\n  }\n}";
    private static final Component NO_ACCESS = Component.literal("OpenAI: The model `gpt-6.1-terra` does not exist or you do not have access to it.");
    private static final String REJECTED = "Your request was rejected by the safety system. If you believe this is an error, contact us at help.openai.com and include the request ID req_123.";
    private static final String REJECTED_JSON = "{\"error\":{\"message\":\"" + REJECTED + "\",\"type\":\"image_generation_user_error\",\"param\":null,\"code\":\"moderation_blocked\",\"moderation_details\":{\"moderation_stage\":\"output\",\"categories\":[\"other\"]}}}";

    private static Component shown(GenerationException error) {
        return GenerationException.describe(error);
    }

    @Test
    void aMessageForThePlayerIsFoundThroughWhatWrapsIt() {
        assertEquals(OUT_OF_IMAGES, GenerationException.describe(new CompletionException(new CompletionException(new GenerationException(OUT_OF_IMAGES)))));
    }

    @Test
    void nothingElseShowsItsOwnText() {
        assertEquals(UNEXPECTED, GenerationException.describe(new CompletionException(new NullPointerException("Cannot invoke \"String.length()\""))));
        assertEquals(UNEXPECTED, GenerationException.describe(new CompletionException(new IOException())));
        assertEquals(UNEXPECTED, GenerationException.describe(new IllegalArgumentException("im == null!")));
    }

    @Test
    void aCallerCanSayWhatNoMessageMeans() {
        assertEquals(BAD_ANSWER, GenerationException.describe(new IllegalArgumentException("Block judge answered an unknown tool"), BAD_ANSWER));
        assertEquals(OUT_OF_IMAGES, GenerationException.describe(new GenerationException(OUT_OF_IMAGES), BAD_ANSWER));
    }

    @Test
    void theLogKeepsTheKeyAndWhatFilledIt() {
        assertEquals(Component.translatable("error.aicraft.openai_error", "no").toString(),
                new GenerationException(Component.translatable("error.aicraft.openai_error", "no")).getMessage());
    }

    @Test
    void noAnswerSaysWhoAndWhetherItTimedOut() {
        assertEquals(Component.translatable("error.aicraft.connect_server"), shown(GenerationException.serverNoAnswer(new CompletionException(new ConnectException()))));
        assertEquals(Component.translatable("error.aicraft.timeout_server"), shown(GenerationException.serverNoAnswer(new CompletionException(new HttpTimeoutException("request timed out")))));
        assertEquals(Component.translatable("error.aicraft.connect_openai"), shown(GenerationException.openAINoAnswer(new IOException("HTTP/1.1 header parser received no bytes"))));
        assertEquals(Component.translatable("error.aicraft.timeout_openai"), shown(GenerationException.openAINoAnswer(new HttpTimeoutException("request timed out"))));
    }

    private static String serverError(String code, String message) {
        return "{\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}";
    }

    @Test
    void theServersCodesBecomeOurOwnSentences() {
        for (String code : new String[]{"limit_global", "limit_user", "limit_ip", "bad_answer", "armor_failed", "openai_moderated", "server_problem"}) {
            assertEquals(Component.translatable("error.aicraft." + code), shown(GenerationException.fromServer(serverError(code, "The server's own words."))), code);
        }
        // As the texture server's src/v3.js writes it.
        assertEquals(Component.translatable("error.aicraft.limit_user"), shown(GenerationException.fromServer(
                "{\"error\":{\"code\":\"limit_user\",\"message\":\"Your daily usage quota exceeded. Please try again tomorrow or use your own OpenAI account\"}}")));
    }

    @Test
    void whatOpenAISaidToTheServerIsQuoted() {
        assertEquals(Component.literal("OpenAI: Rate limit reached for gpt-4.1."), shown(GenerationException.fromServer(serverError("openai", "Rate limit reached for gpt-4.1."))));
    }

    @Test
    void aCodeWeHaveNoSentenceForShowsTheServersMessage() {
        assertEquals(Component.literal("A new notice."), shown(GenerationException.fromServer(serverError("something_new", "A new notice."))));
        assertEquals(Component.literal("mode must be one of auto, item"), shown(GenerationException.fromServer(serverError("bad_request", "mode must be one of auto, item"))));
    }

    @Test
    void anythingElseIsAServerProblem() {
        for (String body : new String[]{"", "   ", "null", "[]", "Not found", "INTERNAL_SERVER_ERRORPixelizer is unavailable: fetch failed",
                "<!DOCTYPE html><html><body>Bad Gateway</body></html>", "{\"error\":\"Invalid or missing API key\"}", "{not json",
                "Your daily usage quota exceeded. Please try again tomorrow or use your own OpenAI account",
                "{\"error\":{\"message\":\"No code.\"}}", "{\"error\":{\"code\":\"openai\"}}", "{\"error\":{\"code\":\"something_new\",\"message\":\" \"}}"}) {
            assertEquals(SERVER_PROBLEM, shown(GenerationException.fromServer(body)), body);
        }
    }

    @Test
    void aSafetyRejectionWithAnOwnKeyStaysInOpenAIsWords() {
        assertEquals(Component.literal("OpenAI: " + REJECTED), shown(GenerationException.fromOpenAI(400, REJECTED_JSON)));
    }

    @Test
    void anOwnKeysErrorIsCutToOpenAIsSentence() {
        assertEquals(NO_ACCESS, shown(GenerationException.fromOpenAI(404, NO_ACCESS_JSON)));
        assertEquals(Component.translatable("error.aicraft.openai_status", "502"), shown(GenerationException.fromOpenAI(502, "<html>Bad Gateway</html>")));
        assertEquals(Component.translatable("error.aicraft.openai_status", "500"), shown(GenerationException.fromOpenAI(500, "{\"error\":{\"message\":\"\"}}")));
    }
}
