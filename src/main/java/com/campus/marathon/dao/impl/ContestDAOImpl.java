package com.campus.marathon.dao.impl;

import com.campus.marathon.dao.ContestDAO;
import com.campus.marathon.model.Contest;
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
 * 竞赛 DAO JDBC 实现。
 */
public class ContestDAOImpl implements ContestDAO {

    private static final String COLUMNS = "id, name, description, status, created_at, updated_at";

    @Override
    public long create(Contest contest) throws SQLException {
        String sql = "INSERT INTO contest (name, description, status) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, contest.getName());
            ps.setString(2, contest.getDescription());
            ps.setString(3, contest.getStatus() == null ? Contest.STATUS_DRAFT : contest.getStatus());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("新增竞赛失败，影响行数为 0");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    contest.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("新增竞赛失败，未获取到主键");
    }

    @Override
    public Contest findById(long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM contest WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<Contest> listAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM contest ORDER BY created_at DESC, id DESC";
        List<Contest> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public List<Contest> listByStatus(String status) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM contest WHERE status = ? ORDER BY created_at DESC, id DESC";
        List<Contest> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public boolean updateStatus(long id, String status) throws SQLException {
        String sql = "UPDATE contest SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean update(Contest contest) throws SQLException {
        String sql = "UPDATE contest SET name = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contest.getName());
            ps.setString(2, contest.getDescription());
            ps.setLong(3, contest.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM contest WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Contest mapRow(ResultSet rs) throws SQLException {
        Contest c = new Contest();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setStatus(rs.getString("status"));
        c.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        c.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return c;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
