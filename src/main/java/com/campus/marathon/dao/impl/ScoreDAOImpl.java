package com.campus.marathon.dao.impl;

import com.campus.marathon.dao.ScoreDAO;
import com.campus.marathon.model.Score;
import com.campus.marathon.util.DBUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评分 DAO JDBC 实现。
 * submit() 优先使用 MySQL 的 INSERT ... ON DUPLICATE KEY UPDATE；
 * 不支持该语法时（H2 等）回退为先查后删再插。
 */
public class ScoreDAOImpl implements ScoreDAO {

    private static final String COLUMNS = "id, contestant_id, judge_id, criterion_id, score, comment, created_at";

    /** 缓存数据库是否为 MySQL */
    private static volatile Boolean isMySql;

    @Override
    public void submit(long judgeId, long contestantId, long criterionId, BigDecimal score, String comment)
            throws SQLException {
        if (score == null) {
            throw new IllegalArgumentException("score 不能为 null");
        }
        String trimmedComment = (comment == null || comment.trim().isEmpty()) ? null : comment.trim();
        if (Boolean.TRUE.equals(isMySQL())) {
            submitMysql(judgeId, contestantId, criterionId, score, trimmedComment);
        } else {
            submitFallback(judgeId, contestantId, criterionId, score, trimmedComment);
        }
    }

    private void submitMysql(long judgeId, long contestantId, long criterionId,
                             BigDecimal score, String comment) throws SQLException {
        String sql = "INSERT INTO score (contestant_id, judge_id, criterion_id, score, comment) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE score = VALUES(score), comment = VALUES(comment), "
                + "created_at = CURRENT_TIMESTAMP";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, contestantId);
            ps.setLong(2, judgeId);
            ps.setLong(3, criterionId);
            ps.setBigDecimal(4, score);
            ps.setString(5, comment);
            ps.executeUpdate();
        }
    }

    private void submitFallback(long judgeId, long contestantId, long criterionId,
                                BigDecimal score, String comment) throws SQLException {
        // 先查是否已存在
        Score existing = findOneByUnique(judgeId, contestantId, criterionId);
        try (Connection conn = DBUtil.getConnection()) {
            if (existing != null) {
                // 覆盖：更新
                String updateSql = "UPDATE score SET score = ?, comment = ?, created_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setBigDecimal(1, score);
                    ps.setString(2, comment);
                    ps.setLong(3, existing.getId());
                    ps.executeUpdate();
                }
            } else {
                // 新增
                String insertSql = "INSERT INTO score (contestant_id, judge_id, criterion_id, score, comment) "
                        + "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, contestantId);
                    ps.setLong(2, judgeId);
                    ps.setLong(3, criterionId);
                    ps.setBigDecimal(4, score);
                    ps.setString(5, comment);
                    ps.executeUpdate();
                }
            }
        }
    }

    private Score findOneByUnique(long judgeId, long contestantId, long criterionId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM score WHERE judge_id = ? AND contestant_id = ? AND criterion_id = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, judgeId);
            ps.setLong(2, contestantId);
            ps.setLong(3, criterionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<Score> listByContestant(long contestantId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM score WHERE contestant_id = ? ORDER BY criterion_id ASC, judge_id ASC";
        return queryList(sql, contestantId);
    }

    @Override
    public List<Score> listByContest(long contestId) throws SQLException {
        String sql = "SELECT s." + COLUMNS + " FROM score s "
                + "JOIN contestant ct ON s.contestant_id = ct.id "
                + "WHERE ct.contest_id = ? "
                + "ORDER BY s.contestant_id ASC, s.judge_id ASC, s.criterion_id ASC";
        return queryList(sql, contestId);
    }

    @Override
    public List<Score> listByJudgeAndContestant(long judgeId, long contestantId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM score WHERE judge_id = ? AND contestant_id = ? ORDER BY criterion_id ASC";
        List<Score> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, judgeId);
            ps.setLong(2, contestantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public boolean deleteByContestant(long contestantId) throws SQLException {
        String sql = "DELETE FROM score WHERE contestant_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, contestantId);
            return ps.executeUpdate() > 0;
        }
    }

    private List<Score> queryList(String sql, long param) throws SQLException {
        List<Score> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private Score mapRow(ResultSet rs) throws SQLException {
        Score s = new Score();
        s.setId(rs.getLong("id"));
        s.setContestantId(rs.getLong("contestant_id"));
        s.setJudgeId(rs.getLong("judge_id"));
        s.setCriterionId(rs.getLong("criterion_id"));
        s.setScore(rs.getBigDecimal("score"));
        s.setComment(rs.getString("comment"));
        s.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return s;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    /** 判断当前连接是否为 MySQL 数据库 */
    private boolean isMySQL() {
        if (isMySql != null) {
            return isMySql;
        }
        synchronized (ScoreDAOImpl.class) {
            if (isMySql != null) {
                return isMySql;
            }
            try (Connection conn = DBUtil.getConnection()) {
                DatabaseMetaData md = conn.getMetaData();
                String productName = md.getDatabaseProductName();
                isMySql = productName != null && productName.toLowerCase().contains("mysql");
            } catch (SQLException e) {
                // 默认回退
                isMySql = false;
            }
            return isMySql;
        }
    }
}
