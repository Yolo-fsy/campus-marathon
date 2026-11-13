package com.campus.marathon.dao.impl;

import com.campus.marathon.dao.ContestantDAO;
import com.campus.marathon.model.Contestant;
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
 * 参赛者 DAO JDBC 实现。
 */
public class ContestantDAOImpl implements ContestantDAO {

    private static final String COLUMNS = "id, contest_id, name, student_id, team_name, submitted_at";

    @Override
    public long create(Contestant contestant) throws SQLException {
        String sql = "INSERT INTO contestant (contest_id, name, student_id, team_name) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, contestant.getContestId());
            ps.setString(2, contestant.getName());
            ps.setString(3, contestant.getStudentId());
            ps.setString(4, contestant.getTeamName());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("新增参赛者失败");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    contestant.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("新增参赛者失败，未获取到主键");
    }

    @Override
    public Contestant findById(long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM contestant WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<Contestant> listByContest(long contestId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM contestant WHERE contest_id = ? ORDER BY submitted_at ASC, id ASC";
        List<Contestant> list = new ArrayList<>();
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
    public boolean delete(long id, long contestId) throws SQLException {
        String sql = "DELETE FROM contestant WHERE id = ? AND contest_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, contestId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean update(Contestant contestant) throws SQLException {
        String sql = "UPDATE contestant SET name = ?, student_id = ?, team_name = ? WHERE id = ? AND contest_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contestant.getName());
            ps.setString(2, contestant.getStudentId());
            ps.setString(3, contestant.getTeamName());
            ps.setLong(4, contestant.getId());
            ps.setLong(5, contestant.getContestId());
            return ps.executeUpdate() > 0;
        }
    }

    private Contestant mapRow(ResultSet rs) throws SQLException {
        Contestant c = new Contestant();
        c.setId(rs.getLong("id"));
        c.setContestId(rs.getLong("contest_id"));
        c.setName(rs.getString("name"));
        c.setStudentId(rs.getString("student_id"));
        c.setTeamName(rs.getString("team_name"));
        c.setSubmittedAt(toLocalDateTime(rs.getTimestamp("submitted_at")));
        return c;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
