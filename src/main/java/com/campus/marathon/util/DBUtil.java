package com.campus.marathon.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC 连接工具：读取 classpath 下的 db.properties 并创建连接。
 */
public final class DBUtil {

    private static final Properties CONFIG = new Properties();
    private static final String URL;
    private static final String USERNAME;
    private static final String PASSWORD;

    static {
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("未找到数据库配置文件 db.properties");
            }
            CONFIG.load(in);
            Class.forName(CONFIG.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver"));
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError("初始化数据库配置失败：" + e.getMessage());
        }
        URL = CONFIG.getProperty("jdbc.url");
        USERNAME = CONFIG.getProperty("jdbc.username", "root");
        PASSWORD = CONFIG.getProperty("jdbc.password", "");
    }

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
