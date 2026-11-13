package com.campus.marathon.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 知识库建表兜底：首次访问 /ai 时自动创建表，避免只部署 WAR 却忘记跑 SQL 脚本。
 */
public final class KbSchema {

    private static volatile boolean initialized;

    private KbSchema() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        synchronized (KbSchema.class) {
            if (initialized) {
                return;
            }
            try (Connection connection = DBUtil.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS kb_document ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT,"
                        + "owner_student_id VARCHAR(40) NOT NULL,"
                        + "owner_name VARCHAR(50) DEFAULT NULL,"
                        + "title VARCHAR(200) NOT NULL,"
                        + "file_name VARCHAR(200) NOT NULL,"
                        + "content_type VARCHAR(80) DEFAULT NULL,"
                        + "char_count INT NOT NULL DEFAULT 0,"
                        + "chunk_count INT NOT NULL DEFAULT 0,"
                        + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "is_admin_visible TINYINT NOT NULL DEFAULT 0,"
                        + "PRIMARY KEY (id),"
                        + "KEY idx_kb_owner (owner_student_id)"
                        + ")");
                statement.execute("CREATE TABLE IF NOT EXISTS kb_chunk ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT,"
                        + "doc_id BIGINT NOT NULL,"
                        + "seq INT NOT NULL,"
                        + "content TEXT NOT NULL,"
                        + "char_count INT NOT NULL DEFAULT 0,"
                        + "PRIMARY KEY (id),"
                        + "KEY idx_kb_chunk_doc (doc_id, seq)"
                        + ")");
                initialized = true;
            } catch (SQLException e) {
                throw new IllegalStateException("初始化知识库数据表失败：" + e.getMessage(), e);
            }
        }
    }
}
