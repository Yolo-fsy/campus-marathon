package com.campus.marathon.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 大模型接口配置。读取 classpath 下的 ai.properties，环境变量优先，
 * 密钥留空时进入检索演示模式，保证没有 Key 也能走通全流程。
 */
public final class AiConfig {

    private static final String CONFIG_FILE = "ai.properties";
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    public AiConfig(String baseUrl, String model, String apiKey, int timeoutSeconds) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeoutSeconds = timeoutSeconds <= 0 ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
    }

    public static AiConfig load() {
        Properties properties = new Properties();
        try (InputStream in = AiConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            // 配置读取失败时退回默认值，由降级模式兜底
        }
        String baseUrl = resolve(properties, "AI_BASE_URL", "ai.base_url", DEFAULT_BASE_URL);
        String model = resolve(properties, "AI_MODEL", "ai.model", DEFAULT_MODEL);
        String apiKey = resolve(properties, "AI_API_KEY", "ai.api_key", "");
        int timeout = DEFAULT_TIMEOUT_SECONDS;
        String timeoutValue = properties.getProperty("ai.timeout_seconds");
        if (timeoutValue != null && !timeoutValue.isBlank()) {
            try {
                timeout = Integer.parseInt(timeoutValue.trim());
            } catch (NumberFormatException ignored) {
                // 非法配置走默认超时
            }
        }
        return new AiConfig(baseUrl, model, apiKey, timeout);
    }

    private static String resolve(Properties properties, String envKey, String propertyKey, String fallback) {
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromProperty = properties.getProperty(propertyKey);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        return fallback;
    }

    private static String trimTrailingSlash(String value) {
        String result = value == null || value.isBlank() ? DEFAULT_BASE_URL : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /** 有密钥才真正调用模型；没有则走检索演示模式。 */
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    public String getEndpoint() {
        return baseUrl + "/chat/completions";
    }
}
