package com.watermelon0117.aicraft.gpt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerationExceptionTest {
    private static final String UNEXPECTED = "Something went wrong. Please try again. The game log has the details.";
    private static final String SERVER_PROBLEM = "The server had a problem. Please try again later.";
    private static final String NO_ACCESS_JSON = "{\n  \"error\": {\n    \"message\": \"The model `gpt-6.1-terra` does not exist or you do not have access to it.\",\n    \"type\": \"invalid_request_error\",\n    \"param\": null,\n    \"code\": \"model_not_found\"\n  }\n}";
    private static final String NO_ACCESS = "OpenAI: The model `gpt-6.1-terra` does not exist or you do not have access to it.";

    @Test
    void aMessageForThePlayerIsFoundThroughWhatWrapsIt() {
        assertEquals("Out of images today.", GenerationException.describe(new CompletionException(new CompletionException(new GenerationException("Out of images today.")))));
    }

    @Test
    void nothingElseShowsItsOwnText() {
        assertEquals(UNEXPECTED, GenerationException.describe(new CompletionException(new NullPointerException("Cannot invoke \"String.length()\""))));
        assertEquals(UNEXPECTED, GenerationException.describe(new CompletionException(new IOException())));
        assertEquals(UNEXPECTED, GenerationException.describe(new IllegalArgumentException("im == null!")));
    }

    @Test
    void aCallerCanSayWhatNoMessageMeans() {
        assertEquals("Its answer could not be used.", GenerationException.describe(new IllegalArgumentException("Block judge answered an unknown tool"), "Its answer could not be used."));
        assertEquals("Out of images today.", GenerationException.describe(new GenerationException("Out of images today."), "Its answer could not be used."));
    }

    @Test
    void noAnswerSaysWhetherItTimedOut() {
        assertEquals("Unable to connect to the server. Please check your internet connection or try again later.",
                GenerationException.noAnswer("the server", new CompletionException(new ConnectException())).getMessage());
        assertEquals("Unable to connect to OpenAI. Please check your internet connection or try again later.",
                GenerationException.noAnswer("OpenAI", new IOException("HTTP/1.1 header parser received no bytes")).getMessage());
        assertEquals("Timed out waiting for the server. Please try again.",
                GenerationException.noAnswer("the server", new CompletionException(new HttpTimeoutException("request timed out"))).getMessage());
    }

    @Test
    void theServersOwnSentencesReachThePlayer() {
        assertEquals("The AI could not decide whether this is a block. Please try again.",
                GenerationException.fromServer("The AI could not decide whether this is a block. Please try again.").getMessage());
        assertEquals("Line one\nline two", GenerationException.fromServer("Line one\\nline two").getMessage());
    }

    @Test
    void whatOnlyTheServersLogsShouldSayDoesNot() {
        for (String body : new String[]{"", "   ", "INTERNAL_SERVER_ERRORPixelizer is unavailable: fetch failed", "<!DOCTYPE html><html><body>Bad Gateway</body></html>", "{\"error\":\"bad\"}", "{not json", "OpenAI error 500: upstream"}) {
            assertEquals(SERVER_PROBLEM, GenerationException.fromServer(body).getMessage(), body);
        }
    }

    @Test
    void whatOpenAISaidToTheServerIsCutToItsSentence() {
        assertEquals(NO_ACCESS, GenerationException.fromServer(NO_ACCESS_JSON).getMessage());
        assertEquals(NO_ACCESS, GenerationException.fromServer("OpenAI error 403: " + NO_ACCESS_JSON).getMessage());
    }

    @Test
    void anOwnKeysErrorIsCutToOpenAIsSentence() {
        assertEquals(NO_ACCESS, GenerationException.fromOpenAI(404, NO_ACCESS_JSON).getMessage());
        assertEquals("OpenAI had a problem (HTTP 502). Please try again later.", GenerationException.fromOpenAI(502, "<html>Bad Gateway</html>").getMessage());
        assertEquals("OpenAI had a problem (HTTP 500). Please try again later.", GenerationException.fromOpenAI(500, "{\"error\":{\"message\":\"\"}}").getMessage());
    }
}
