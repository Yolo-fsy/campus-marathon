package com.campus.marathon.service;

import com.campus.marathon.dao.RegistrationDAO;
import com.campus.marathon.dao.impl.RegistrationDAOImpl;
import com.campus.marathon.model.MarathonRunner;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 登录认证：管理员账号来自 classpath 的 auth.properties，
 * 参赛者使用「学号 + 手机号」核验已有报名记录。
 */
public class AuthService {

    private static final Properties AUTH_CONFIG = new Properties();

    static {
        try (InputStream in = AuthService.class.getClassLoader().getResourceAsStream("auth.properties")) {
            if (in != null) {
                AUTH_CONFIG.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("读取 auth.properties 失败：" + e.getMessage());
        }
    }

    private final RegistrationDAO registrationDAO;

    public AuthService() {
        this(new RegistrationDAOImpl());
    }

    public AuthService(RegistrationDAO registrationDAO) {
        this.registrationDAO = registrationDAO;
    }

    public boolean verifyAdmin(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        String expectedUsername = AUTH_CONFIG.getProperty("admin.username", "admin").trim();
        String expectedPassword = AUTH_CONFIG.getProperty("admin.password", "admin123").trim();
        return expectedUsername.equals(username.trim()) && expectedPassword.equals(password.trim());
    }

    public LoginResult loginRunner(String studentId, String phone) throws SQLException {
        String id = studentId == null ? "" : studentId.trim();
        String mobile = phone == null ? "" : phone.trim();
        if (id.isEmpty() || mobile.isEmpty()) {
            return LoginResult.failure("请填写学号与手机号");
        }

        MarathonRunner runner = registrationDAO.findByStudentId(id);
        if (runner == null) {
            return LoginResult.failure("该学号还没有报名记录，请先完成在线报名");
        }
        if (!mobile.equals(runner.getPhone())) {
            return LoginResult.failure("学号与手机号不匹配，请检查后重试");
        }
        return LoginResult.success(runner);
    }
}
