package com.campus.marathon.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 评审账号密码工具：手动实现加盐 SHA-256（不引入 BCrypt）。
 * credential = SHA-256(salt + password) 的十六进制字符串。
 */
public final class PasswordUtil {

    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** 生成 16 字节随机盐，十六进制字符串（32 字符） */
    public static String generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH];
        RANDOM.nextBytes(saltBytes);
        return HexFormat.of().formatHex(saltBytes);
    }

    /** 计算 SHA-256(salt + password) 的十六进制哈希 */
    public static String hash(String salt, String password) {
        if (salt == null || password == null) {
            throw new IllegalArgumentException("salt 和 password 都不能为 null");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            md.update(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用：" + e.getMessage(), e);
        }
    }

    /** 验证密码是否正确 */
    public static boolean verify(String salt, String password, String expectedCredential) {
        if (salt == null || password == null || expectedCredential == null) {
            return false;
        }
        return expectedCredential.equalsIgnoreCase(hash(salt, password));
    }
}
