package com.campus.marathon.service;

import com.campus.marathon.dao.AccountDAO;
import com.campus.marathon.dao.impl.AccountDAOImpl;
import com.campus.marathon.model.RunnerAccount;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 参赛者账号：注册校验、密码加密（PBKDF2）与登录核验。
 */
public class AccountService {

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z·\\s]{2,20}$");
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{8,12}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private static final String PBKDF2 = "pbkdf2$";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountDAO accountDAO;

    public AccountService() {
        this(new AccountDAOImpl());
    }

    public AccountService(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    public RunnerAccount findByStudentId(String studentId) throws SQLException {
        if (studentId == null || studentId.trim().isEmpty()) {
            return null;
        }
        return accountDAO.findByStudentId(studentId.trim());
    }

    /**
     * 注册：字段校验 + 学号查重，密码以 PBKDF2 加盐存储。
     */
    public void register(String name, String studentId, String phone, String college,
                         String password, String confirm) throws ValidationException, SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        String trimmedName = trim(name);
        String trimmedId = trim(studentId);
        String trimmedPhone = trim(phone);
        String trimmedCollege = trim(college);

        if (trimmedName.isEmpty()) {
            errors.put("name", "请填写姓名");
        } else if (!NAME_PATTERN.matcher(trimmedName).matches()) {
            errors.put("name", "姓名为 2-20 个字符，仅支持中英文字母");
        }

        if (trimmedId.isEmpty()) {
            errors.put("studentId", "请填写学号");
        } else if (!STUDENT_ID_PATTERN.matcher(trimmedId).matches()) {
            errors.put("studentId", "学号应为 8-12 位数字");
        }

        if (trimmedPhone.isEmpty()) {
            errors.put("phone", "请填写手机号");
        } else if (!PHONE_PATTERN.matcher(trimmedPhone).matches()) {
            errors.put("phone", "请输入 11 位有效手机号");
        }

        if (trimmedCollege.length() > 40) {
            errors.put("college", "学院名称不超过 40 个字符");
        }

        if (password == null || password.isEmpty()) {
            errors.put("password", "请设置登录密码");
        } else if (password.length() < 6) {
            errors.put("password", "密码至少 6 位");
        } else if (password.length() > 32) {
            errors.put("password", "密码不超过 32 位");
        }

        if (confirm == null || !confirm.equals(password)) {
            errors.put("confirm", "两次输入的密码不一致");
        }

        if (errors.isEmpty() && accountDAO.findByStudentId(trimmedId) != null) {
            errors.put("studentId", "该学号已注册账号，请直接登录");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        RunnerAccount account = new RunnerAccount();
        account.setName(trimmedName);
        account.setStudentId(trimmedId);
        account.setPhone(trimmedPhone);
        account.setCollege(trimmedCollege.isEmpty() ? null : trimmedCollege);
        account.setCredential(hash(password));
        account.setCreatedAt(LocalDateTime.now());
        accountDAO.create(account);
    }

    /**
     * 校验账号密码，密码错误与账号不存在由调用方分别提示。
     */
    public boolean matches(RunnerAccount account, String rawPassword) {
        if (account == null || rawPassword == null || account.getCredential() == null) {
            return false;
        }
        String[] parts = account.getCredential().split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(rawPassword, salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        try {
            byte[] key = derive(password, salt, ITERATIONS, KEY_BITS);
            return PBKDF2 + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("密码加密失败", e);
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations, int bits)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bits);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
