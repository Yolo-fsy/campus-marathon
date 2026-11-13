package com.campus.marathon.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmSseParserTest {

    @Test
    void extractsDeltaContent() {
        String line = "{\"choices\":[{\"index\":0,\"delta\":{\"content\":\"报名\"}}]}";
        assertEquals("报名", LlmClient.parseDeltaContent(line));
    }

    @Test
    void decodesEscapedCharacters() {
        String line = "{\"choices\":[{\"delta\":{\"content\":\"第一行\\n第二行\\t制表符\\\\路径\\\"引号\\\"\"}}]}";
        assertEquals("第一行\n第二行\t制表符\\路径\"引号\"", LlmClient.parseDeltaContent(line));
    }

    @Test
    void nullContentOrMalformedLineReturnsNull() {
        assertNull(LlmClient.parseDeltaContent("{\"choices\":[{\"delta\":{\"content\":null}}]}"));
        assertNull(LlmClient.parseDeltaContent("{\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}"));
        assertNull(LlmClient.parseDeltaContent("not-a-json-line"));
        assertNull(LlmClient.parseDeltaContent(null));
    }

    @Test
    void extractsErrorMessageFromErrorBody() {
        String body = "{\"error\":{\"message\":\"Incorrect API key provided\",\"type\":\"invalid_request_error\"}}";
        assertEquals("Incorrect API key provided", LlmClient.extractErrorMessage(body));
    }

    @Test
    void escapeJsonProducesValidPayload() {
        String escaped = LlmClient.escapeJson("他说：\"换行\n测试\"");
        assertTrue(escaped.contains("\\\""));
        assertTrue(escaped.contains("\\n"));
    }
}
