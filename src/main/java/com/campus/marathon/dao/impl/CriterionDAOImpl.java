package com.campus.marathon.dao.impl;

import com.campus.marathon.dao.CriterionDAO;
import com.campus.marathon.model.Criterion;
import com.campus.marathon.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 评分维度 DAO JDBC 实现。
 */
public class CriterionDAOImpl implements CriterionDAO {

    private static final String COLUMNS = "id, contest_id, name, weight, description";

    @Override
    public long create(Criterion criterion) throws SQLException {
        String sql = "INSERT INTO criterion (contest_id, name, weight, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, criterion.getContestId());
            ps.setString(2, criterion.getName());
            ps.setInt(3, criterion.getWeight());
            ps.setString(4, criterion.getDescription());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("新增评分维度失败");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    criterion.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("新增评分维度失败，未获取到主键");
    }

    @Override
    public List<Criterion> listByContest(long contestId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM criterion WHERE contest_id = ? ORDER BY id ASC";
        List<Criterion> list = new ArrayList<>();
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
    public boolean updateWeight(long id, long contestId, int weight) throws SQLException {
        String sql = "UPDATE criterion SET weight = ? WHERE id = ? AND contest_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, weight);
            ps.setLong(2, id);
            ps.setLong(3, contestId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean update(Criterion criterion) throws SQLException {
        String sql = "UPDATE criterion SET name = ?, weight = ?, description = ? WHERE id = ? AND contest_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criterion.getName());
            ps.setInt(2, criterion.getWeight());
            ps.setString(3, criterion.getDescription());
            ps.setLong(4, criterion.getId());
            ps.setLong(5, criterion.getContestId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(long id, long contestId) throws SQLException {
        String sql = "DELETE FROM criterion WHERE id = ? AND contest_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, contestId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteByContest(long contestId) throws SQLException {
        String sql = "DELETE FROM criterion WHERE contest_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, contestId);
            return ps.executeUpdate() > 0;
        }
    }

    private Criterion mapRow(ResultSet rs) throws SQLException {
        Criterion c = new Criterion();
        c.setId(rs.getLong("id"));
        c.setContestId(rs.getLong("contest_id"));
        c.setName(rs.getString("name"));
        c.setWeight(rs.getInt("weight"));
        c.setDescription(rs.getString("description"));
        return c;
    }
}
