package com.campus.marathon.service;

import com.campus.marathon.dao.AccountDAO;
import com.campus.marathon.model.RunnerAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 账号注册与密码校验单元测试（不依赖数据库）。
 */
class AccountServiceTest {

    private InMemoryAccountDAO dao;
    private AccountService service;

    @BeforeEach
    void setUp() {
        dao = new InMemoryAccountDAO();
        service = new AccountService(dao);
    }

    @Test
    @DisplayName("注册成功：密码以 PBKDF2 加盐哈希存储，不落明文")
    void registerStoresHashedCredential() throws Exception {
        service.register("张三", "2023010203", "13800138001", "计算机科学与技术学院", "abc123", "abc123");

        RunnerAccount account = dao.findByStudentId("2023010203");
        assertNotNull(account);
        assertEquals("张三", account.getName());
        assertEquals("13800138001", account.getPhone());
        assertEquals("计算机科学与技术学院", account.getCollege());
        assertTrue(account.getCredential().startsWith("pbkdf2$"));
        assertFalse(account.getCredential().contains("abc123"));
        assertTrue(service.matches(account, "abc123"));
    }

    @Test
    @DisplayName("重复学号注册被拒绝")
    void registerRejectsDuplicateStudentId() throws Exception {
        service.register("张三", "2023010203", "13800138001", "", "abc123", "abc123");

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.register("李四", "2023010203", "13900139002", "", "abc123", "abc123"));
        assertTrue(e.getFieldErrors().containsKey("studentId"));
    }

    @Test
    @DisplayName("密码过短或两次不一致时拒绝注册")
    void registerRejectsWeakOrMismatchedPassword() {
        ValidationException shortPwd = assertThrows(ValidationException.class,
                () -> service.register("张三", "2023010203", "13800138001", "", "12345", "12345"));
        assertTrue(shortPwd.getFieldErrors().containsKey("password"));

        ValidationException mismatch = assertThrows(ValidationException.class,
                () -> service.register("张三", "2023010203", "13800138001", "", "abc123", "abc124"));
        assertTrue(mismatch.getFieldErrors().containsKey("confirm"));
    }

    @Test
    @DisplayName("学号或手机号格式非法时拒绝注册")
    void registerValidatesStudentIdAndPhone() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.register("张三", "2023", "1380013", "", "abc123", "abc123"));
        assertTrue(e.getFieldErrors().containsKey("studentId"));
        assertTrue(e.getFieldErrors().containsKey("phone"));
    }

    @Test
    @DisplayName("密码错误时校验失败，未知账号返回 null")
    void matchesRejectsWrongPassword() throws Exception {
        service.register("张三", "2023010203", "13800138001", "", "abc123", "abc123");

        assertFalse(service.matches(dao.findByStudentId("2023010203"), "abcdef"));
        assertEquals(null, dao.findByStudentId("2099999999"));
    }

    /** 内存版账号 DAO，避免测试依赖真实数据库。 */
    static class InMemoryAccountDAO implements AccountDAO {

        private final Map<String, RunnerAccount> store = new HashMap<>();

        @Override
        public RunnerAccount findByStudentId(String studentId) {
            return store.get(studentId);
        }

        @Override
        public void create(RunnerAccount account) throws SQLException {
            if (store.containsKey(account.getStudentId())) {
                throw new SQLException("duplicate student_id");
            }
            account.setId((long) store.size() + 1);
            store.put(account.getStudentId(), account);
        }
    }
}
