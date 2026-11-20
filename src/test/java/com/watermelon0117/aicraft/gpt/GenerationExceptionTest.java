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
    private static final Component MODERATED = Component.translatable("error.aicraft.openai_moderated");

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

    @Test
    void theServersFixedSentencesBecomeOurOwn() {
        assertEquals(Component.translatable("error.aicraft.limit_global"),
                shown(GenerationException.fromServer("The system-wide usage limit has been reached. Please try again tomorrow or use your own OpenAI account")));
        assertEquals(Component.translatable("error.aicraft.limit_user"),
                shown(GenerationException.fromServer("Your daily usage quota exceeded. Please try again tomorrow or use your own OpenAI account")));
        assertEquals(Component.translatable("error.aicraft.limit_ip"),
                shown(GenerationException.fromServer("Too many request from your IP. Please try again tomorrow or use your own OpenAI account")));
        assertEquals(BAD_ANSWER, shown(GenerationException.fromServer("The AI could not decide whether this is a block. Please try again.")));
    }

    @Test
    void otherServerSentencesReachThePlayerAsTheyAre() {
        assertEquals(Component.literal("A new notice."), shown(GenerationException.fromServer("A new notice.")));
        assertEquals(Component.literal("Line one\nline two"), shown(GenerationException.fromServer("Line one\\nline two")));
    }

    @Test
    void whatOnlyTheServersLogsShouldSayDoesNot() {
        for (String body : new String[]{"", "   ", "INTERNAL_SERVER_ERRORPixelizer is unavailable: fetch failed", "<!DOCTYPE html><html><body>Bad Gateway</body></html>", "{\"error\":\"bad\"}", "{not json", "OpenAI error 500: upstream"}) {
            assertEquals(SERVER_PROBLEM, shown(GenerationException.fromServer(body)), body);
        }
    }

    @Test
    void whatOpenAISaidToTheServerIsCutToItsSentence() {
        assertEquals(NO_ACCESS, shown(GenerationException.fromServer(NO_ACCESS_JSON)));
        assertEquals(NO_ACCESS, shown(GenerationException.fromServer("OpenAI error 403: " + NO_ACCESS_JSON)));
    }

    @Test
    void aSafetyRejectionThroughTheServerIsInThePlayersLanguage() {
        assertEquals(MODERATED, shown(GenerationException.fromServer(REJECTED_JSON)));
        assertEquals(MODERATED, shown(GenerationException.fromServer("OpenAI error 400: " + REJECTED_JSON)));
    }

    @Test
    void eitherTheCodeOrTheSentenceMarksASafetyRejection() {
        assertEquals(MODERATED, shown(GenerationException.fromServer("{\"error\":{\"message\":\"Your request was rejected by the safety system.\",\"code\":null}}")));
        assertEquals(MODERATED, shown(GenerationException.fromServer("{\"error\":{\"message\":\"Blocked.\",\"code\":\"moderation_blocked\"}}")));
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
