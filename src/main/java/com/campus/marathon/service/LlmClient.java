package com.campus.marathon.service;

import com.campus.marathon.util.AiConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * OpenAI 兼容接口客户端：POST /chat/completions（stream=true），逐行解析 SSE 并把增量文本回调出去。
 */
public class LlmClient {

    public interface TokenSink {
        void onToken(String token) throws IOException;
    }

    private final AiConfig config;
    private final HttpClient httpClient;

    public LlmClient() {
        this(AiConfig.load());
    }

    public LlmClient(AiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void streamChat(String systemPrompt, String userPrompt, TokenSink sink)
            throws IOException, InterruptedException {
        if (!config.isConfigured()) {
            throw new IOException("未配置模型密钥");
        }
        String body = buildRequestBody(systemPrompt, userPrompt);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getEndpoint()))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("模型接口返回 " + response.statusCode() + "：" + extractErrorMessage(errorBody));
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }
                String delta = parseDeltaContent(data);
                if (delta != null && !delta.isEmpty()) {
                    sink.onToken(delta);
                }
            }
        }
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) {
        StringBuilder json = new StringBuilder();
        json.append("{\"model\":\"").append(escapeJson(config.getModel())).append("\",")
                .append("\"stream\":true,")
                .append("\"temperature\":0.2,")
                .append("\"messages\":[")
                .append("{\"role\":\"system\",\"content\":\"").append(escapeJson(systemPrompt)).append("\"},")
                .append("{\"role\":\"user\",\"content\":\"").append(escapeJson(userPrompt)).append("\"}")
                .append("]}");
        return json.toString();
    }

    /**
     * 从单条 SSE data 的 JSON 中抽出 choices[0].delta.content，缺省或 null 时返回 null。
     */
    public static String parseDeltaContent(String json) {
        if (json == null) {
            return null;
        }
        int contentIndex = json.indexOf("\"content\"");
        if (contentIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', contentIndex);
        if (colon < 0) {
            return null;
        }
        int cursor = colon + 1;
        while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
            cursor++;
        }
        if (cursor >= json.length() || json.charAt(cursor) != '"') {
            return null;
        }
        return readJsonString(json, cursor);
    }

    /**
     * 从错误响应里抽出 error.message，拿不到就截断原始响应。
     */
    static String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "无响应内容";
        }
        int messageIndex = body.indexOf("\"message\"");
        if (messageIndex >= 0) {
            int colon = body.indexOf(':', messageIndex);
            if (colon >= 0) {
                int cursor = colon + 1;
                while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
                    cursor++;
                }
                if (cursor < body.length() && body.charAt(cursor) == '"') {
                    return readJsonString(body, cursor);
                }
            }
        }
        return body.length() <= 200 ? body : body.substring(0, 200) + "…";
    }

    private static String readJsonString(String json, int quoteIndex) {
        StringBuilder result = new StringBuilder();
        for (int i = quoteIndex + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') {
                i++;
                if (i >= json.length()) {
                    break;
                }
                char escaped = json.charAt(i);
                switch (escaped) {
                    case 'n':
                        result.append('\n');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case 'b':
                        result.append('\b');
                        break;
                    case 'f':
                        result.append('\f');
                        break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            try {
                                result.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                                i += 4;
                            } catch (NumberFormatException ignored) {
                                result.append("\\u");
                            }
                        }
                        break;
                    default:
                        result.append(escaped);
                }
            } else if (c == '"') {
                return result.toString();
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }
}
