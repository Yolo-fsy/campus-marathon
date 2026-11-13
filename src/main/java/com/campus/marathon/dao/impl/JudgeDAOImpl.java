package com.campus.marathon.dao.impl;

import com.campus.marathon.dao.JudgeDAO;
import com.campus.marathon.model.Judge;
import com.campus.marathon.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评审 DAO JDBC 实现。
 */
public class JudgeDAOImpl implements JudgeDAO {

    private static final String COLUMNS = "id, contest_id, username, display_name, credential, salt, created_at";

    @Override
    public long create(Judge judge) throws SQLException {
        String sql = "INSERT INTO judge (contest_id, username, display_name, credential, salt) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, judge.getContestId());
            ps.setString(2, judge.getUsername());
            ps.setString(3, judge.getDisplayName());
            ps.setString(4, judge.getCredential());
            ps.setString(5, judge.getSalt());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("新增评审失败");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    judge.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("新增评审失败，未获取到主键");
    }

    @Override
    public Judge findByUsername(String username) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM judge WHERE username = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public Judge findByUsernameAndContest(String username, long contestId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM judge WHERE username = ? AND contest_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong(2, contestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<Judge> listByContest(long contestId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM judge WHERE contest_id = ? ORDER BY id ASC";
        List<Judge> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, contestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public long addJudge(long contestId, String username, String displayName, String credential, String salt)
            throws SQLException {
        Judge judge = new Judge();
        judge.setContestId(contestId);
        judge.setUsername(username);
        judge.setDisplayName(displayName);
        judge.setCredential(credential);
        judge.setSalt(salt);
        return create(judge);
    }

    @Override
    public boolean removeJudge(long contestId, long judgeId) throws SQLException {
        String sql = "DELETE FROM judge WHERE id = ? AND contest_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, judgeId);
            ps.setLong(2, contestId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateCredential(long id, String credential, String salt) throws SQLException {
        String sql = "UPDATE judge SET credential = ?, salt = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, credential);
            ps.setString(2, salt);
            ps.setLong(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Judge mapRow(ResultSet rs) throws SQLException {
        Judge j = new Judge();
        j.setId(rs.getLong("id"));
        j.setContestId(rs.getLong("contest_id"));
        j.setUsername(rs.getString("username"));
        j.setDisplayName(rs.getString("display_name"));
        j.setCredential(rs.getString("credential"));
        j.setSalt(rs.getString("salt"));
        j.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return j;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
