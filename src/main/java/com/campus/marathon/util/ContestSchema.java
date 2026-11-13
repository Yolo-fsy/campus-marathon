package com.campus.marathon.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 竞赛管理系统建表兜底：首次访问竞赛相关功能时自动建表并插入种子数据，
 * 避免只部署 WAR 却忘记跑 SQL 脚本。
 *
 * <p>与 KbSchema 风格一致：双重检查锁 + CREATE TABLE IF NOT EXISTS。
 */
public final class ContestSchema {

    private static volatile boolean initialized;

    private ContestSchema() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        synchronized (ContestSchema.class) {
            if (initialized) {
                return;
            }
            try (Connection connection = DBUtil.getConnection();
                 Statement statement = connection.createStatement()) {

                // 1. 竞赛主表
                statement.execute("CREATE TABLE IF NOT EXISTS contest ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT,"
                        + "name VARCHAR(200) NOT NULL,"
                        + "description TEXT DEFAULT NULL,"
                        + "status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',"
                        + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "PRIMARY KEY (id),"
                        + "KEY idx_contest_status (status)"
                        + ")");

                // 2. 参赛者表
                statement.execute("CREATE TABLE IF NOT EXISTS contestant ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT,"
                        + "contest_id BIGINT NOT NULL,"
                        + "name VARCHAR(50) DEFAULT NULL,"
                        + "student_id VARCHAR(40) DEFAULT NULL,"
                        + "team_name VARCHAR(100) DEFAULT NULL,"
                        + "submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "PRIMARY KEY (id),"
                        + "KEY idx_contestant_contest (contest_id),"
                        + "CONSTRAINT fk_contestant_contest FOREIGN KEY (contest_id) "
                        + "REFERENCES contest(id) ON DELETE CASCADE"
                        + ")");

                // 3. 评审表
                statement.execute("CREATE TABLE IF NOT EXISTS judge ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT,"
                        + "contest_id BIGINT NOT NULL,"
                        + "username VARCHAR(50) NOT NULL,"
                        + "display_name VARCHAR(50) DEFAULT NULL,"
                        + "credential VARCHAR(128) NOT NULL,"
                        + "salt VARCHAR(64) NOT NULL,"
                        + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "PRIMARY KEY (id),"
                        + "UNIQUE KEY uk_judge_contest_username (contest_id, username),"
                        + "KEY idx_judge_username (username),"
                        + "CONSTRAINT fk_judge_contest FOREIGN KEY (contest_id) "
                        + "REFERENCES contest(id) ON DELETE CASCADE"
                        + ")");

                // 4. 评分维度表
                statement.execute("CREATE TABLE IF NOT EXISTS criterion ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT,"
                        + "contest_id BIGINT NOT NULL,"
                        + "name VARCHAR(100) NOT NULL,"
                        + "weight INT NOT NULL DEFAULT 0,"
                        + "description VARCHAR(500) DEFAULT NULL,"
                        + "PRIMARY KEY (id),"
                        + "KEY idx_criterion_contest (contest_id),"
                        + "CONSTRAINT fk_criterion_contest FOREIGN KEY (contest_id) "
                        + "REFERENCES contest(id) ON DELETE CASCADE"
                        + ")");

                // 5. 评分记录表（同一 judge + contestant + criterion 唯一）
                statement.execute("CREATE TABLE IF NOT EXISTS score ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT,"
                        + "contestant_id BIGINT NOT NULL,"
                        + "judge_id BIGINT NOT NULL,"
                        + "criterion_id BIGINT NOT NULL,"
                        + "score DECIMAL(4,2) NOT NULL,"
                        + "comment VARCHAR(500) DEFAULT NULL,"
                        + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "PRIMARY KEY (id),"
                        + "UNIQUE KEY uk_score_unique (judge_id, contestant_id, criterion_id),"
                        + "KEY idx_score_contestant (contestant_id),"
                        + "KEY idx_score_judge (judge_id),"
                        + "KEY idx_score_criterion (criterion_id),"
                        + "CONSTRAINT fk_score_contestant FOREIGN KEY (contestant_id) "
                        + "REFERENCES contestant(id) ON DELETE CASCADE,"
                        + "CONSTRAINT fk_score_judge FOREIGN KEY (judge_id) "
                        + "REFERENCES judge(id) ON DELETE CASCADE,"
                        + "CONSTRAINT fk_score_criterion FOREIGN KEY (criterion_id) "
                        + "REFERENCES criterion(id) ON DELETE CASCADE"
                        + ")");

                // 种子数据（仅当没有任何竞赛时插入）
                seedIfEmpty(connection);

                initialized = true;
            } catch (SQLException e) {
                throw new IllegalStateException("初始化竞赛数据表失败：" + e.getMessage(), e);
            }
        }
    }

    /** 仅当 contest 表为空时插入一份种子数据 */
    private static void seedIfEmpty(Connection conn) throws SQLException {
        long count;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM contest")) {
            rs.next();
            count = rs.getLong(1);
        }
        if (count > 0) {
            return;
        }

        // 1. 竞赛
        long contestId;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO contest (name, description, status) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "2026 校园程序设计大赛");
            ps.setString(2, "面向全校本科生的算法与工程综合竞赛，分评审维度加权排名。");
            ps.setString(3, "DRAFT");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                contestId = keys.getLong(1);
            }
        }

        // 2. 参赛者 × 2
        long c1Id, c2Id;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO contestant (contest_id, name, student_id, team_name) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, contestId);
            ps.setString(2, "林小白");
            ps.setString(3, "2023010101");
            ps.setString(4, "灵犀小组");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                c1Id = keys.getLong(1);
            }
            ps.setLong(1, contestId);
            ps.setString(2, "陈大力");
            ps.setString(3, "2022020202");
            ps.setString(4, "愚公移山队");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                c2Id = keys.getLong(1);
            }
        }

        // 3. 评审 judge1 / judge1
        long judgeId;
        {
            String salt = PasswordUtil.generateSalt();
            String credential = PasswordUtil.hash(salt, "judge1");
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO judge (contest_id, username, display_name, credential, salt) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, contestId);
                ps.setString(2, "judge1");
                ps.setString(3, "示范评审");
                ps.setString(4, credential);
                ps.setString(5, salt);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    judgeId = keys.getLong(1);
                }
            }
        }

        // 4. 评分维度 × 3（技术难度 30 / 创新性 40 / 完成度 30）
        long critTechId, critInnoId, critFinId;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO criterion (contest_id, name, weight, description) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            // 技术难度 30
            ps.setLong(1, contestId);
            ps.setString(2, "技术难度");
            ps.setInt(3, 30);
            ps.setString(4, "算法复杂度、数据结构运用、边界条件处理");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                critTechId = keys.getLong(1);
            }
            // 创新性 40
            ps.setLong(1, contestId);
            ps.setString(2, "创新性");
            ps.setInt(3, 40);
            ps.setString(4, "解法新颖性、工程巧思、文档表达");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                critInnoId = keys.getLong(1);
            }
            // 完成度 30
            ps.setLong(1, contestId);
            ps.setString(2, "完成度");
            ps.setInt(3, 30);
            ps.setString(4, "功能完备度、鲁棒性、性能与代码规范");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                critFinId = keys.getLong(1);
            }
        }

        // 5. 给 seed 的参赛者一份初始评分（judge1 各维度打 8 分，方便排名演示）
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO score (contestant_id, judge_id, criterion_id, score, comment) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            // 灵犀小组
            ps.setLong(1, c1Id); ps.setLong(2, judgeId); ps.setLong(3, critTechId);
            ps.setBigDecimal(4, new java.math.BigDecimal("8.00")); ps.setString(5, "seed 初始分");
            ps.addBatch();
            ps.setLong(1, c1Id); ps.setLong(2, judgeId); ps.setLong(3, critInnoId);
            ps.setBigDecimal(4, new java.math.BigDecimal("9.00")); ps.setString(5, "seed 初始分");
            ps.addBatch();
            ps.setLong(1, c1Id); ps.setLong(2, judgeId); ps.setLong(3, critFinId);
            ps.setBigDecimal(4, new java.math.BigDecimal("7.50")); ps.setString(5, "seed 初始分");
            ps.addBatch();
            // 愚公移山队
            ps.setLong(1, c2Id); ps.setLong(2, judgeId); ps.setLong(3, critTechId);
            ps.setBigDecimal(4, new java.math.BigDecimal("7.00")); ps.setString(5, "seed 初始分");
            ps.addBatch();
            ps.setLong(1, c2Id); ps.setLong(2, judgeId); ps.setLong(3, critInnoId);
            ps.setBigDecimal(4, new java.math.BigDecimal("8.00")); ps.setString(5, "seed 初始分");
            ps.addBatch();
            ps.setLong(1, c2Id); ps.setLong(2, judgeId); ps.setLong(3, critFinId);
            ps.setBigDecimal(4, new java.math.BigDecimal("8.50")); ps.setString(5, "seed 初始分");
            ps.addBatch();
            ps.executeBatch();
        }
    }
}
