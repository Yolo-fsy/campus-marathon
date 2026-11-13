package com.campus.marathon.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图形验证码校验逻辑单元测试（不依赖会话与容器）。
 */
class CaptchaServiceTest {

    private static final long NOW = 1_700_000_000_000L;

    @Test
    @DisplayName("验证码正确且在有效期内：校验通过")
    void validateAcceptsCorrectCode() {
        assertTrue(CaptchaService.validate("4821", NOW + 60_000L, NOW, "4821"));
        assertTrue(CaptchaService.validate("4821", NOW + 60_000L, NOW, " 4821 "));
    }

    @Test
    @DisplayName("验证码错误：校验不通过")
    void validateRejectsWrongCode() {
        assertFalse(CaptchaService.validate("4821", NOW + 60_000L, NOW, "4822"));
    }

    @Test
    @DisplayName("验证码过期：校验不通过")
    void validateRejectsExpiredCode() {
        assertFalse(CaptchaService.validate("4821", NOW - 1L, NOW, "4821"));
    }

    @Test
    @DisplayName("空值或未生成验证码：校验不通过")
    void validateRejectsEmptyValues() {
        assertFalse(CaptchaService.validate(null, NOW + 60_000L, NOW, "4821"));
        assertFalse(CaptchaService.validate("4821", null, NOW, "4821"));
        assertFalse(CaptchaService.validate("4821", NOW + 60_000L, NOW, null));
        assertFalse(CaptchaService.validate("4821", NOW + 60_000L, NOW, "  "));
    }

    @Test
    @DisplayName("随机验证码：长度为 4 且全部为数字")
    void randomCodeIsFourDigits() {
        for (int i = 0; i < 200; i++) {
            String code = CaptchaService.randomCode();
            assertEquals(4, code.length());
            assertTrue(code.matches("\\d{4}"));
        }
    }
}
