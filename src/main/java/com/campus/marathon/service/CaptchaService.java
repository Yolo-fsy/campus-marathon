package com.campus.marathon.service;

import javax.servlet.http.HttpSession;
import java.security.SecureRandom;

/**
 * 注册图形验证码（会话级）：4 位数字，5 分钟有效，校验后立即失效。
 */
public final class CaptchaService {

    public static final String SESSION_CODE = "regCaptchaCode";
    public static final String SESSION_EXPIRE_AT = "regCaptchaExpireAt";

    public static final int TTL_SECONDS = 300;
    static final int CODE_LENGTH = 4;

    private static final SecureRandom RANDOM = new SecureRandom();

    private CaptchaService() {
    }

    /**
     * 生成新验证码并写入会话，返回验证码文本（供图片绘制）。
     */
    public static String issue(HttpSession session) {
        String code = randomCode();
        session.setAttribute(SESSION_CODE, code);
        session.setAttribute(SESSION_EXPIRE_AT, System.currentTimeMillis() + TTL_SECONDS * 1000L);
        return code;
    }

    /**
     * 校验并立即失效：无论成功与否，同一张图片验证码都只能使用一次。
     */
    public static boolean verify(HttpSession session, String input) {
        Object stored = session.getAttribute(SESSION_CODE);
        Object expireAt = session.getAttribute(SESSION_EXPIRE_AT);
        boolean ok = validate(stored == null ? null : stored.toString(),
                expireAt == null ? null : (Long) expireAt,
                System.currentTimeMillis(), input);
        clear(session);
        return ok;
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(SESSION_CODE);
        session.removeAttribute(SESSION_EXPIRE_AT);
    }

    /**
     * 纯函数校验，便于单元测试：空值、过期、内容不一致均不通过。
     */
    static boolean validate(String stored, Long expireAtMillis, long nowMillis, String input) {
        if (stored == null || expireAtMillis == null || input == null) {
            return false;
        }
        String trimmed = input.trim();
        return !trimmed.isEmpty()
                && nowMillis <= expireAtMillis
                && stored.equalsIgnoreCase(trimmed);
    }

    static String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append((char) ('0' + RANDOM.nextInt(10)));
        }
        return sb.toString();
    }
}
