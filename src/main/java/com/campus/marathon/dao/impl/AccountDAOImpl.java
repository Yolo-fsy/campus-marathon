package com.campus.marathon.dao.impl;

import com.campus.marathon.dao.AccountDAO;
import com.campus.marathon.model.RunnerAccount;
import com.campus.marathon.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * 参赛者账号 JDBC 实现。
 */
public class AccountDAOImpl implements AccountDAO {

    private static final String SELECT_BASE =
            "SELECT id, student_id, name, phone, college, credential, created_at FROM runner_account ";

    @Override
    public RunnerAccount findByStudentId(String studentId) throws SQLException {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BASE + "WHERE student_id = ?")) {
            statement.setString(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public void create(RunnerAccount account) throws SQLException {
        String sql = "INSERT INTO runner_account (student_id, name, phone, college, credential, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"})) {
            statement.setString(1, account.getStudentId());
            statement.setString(2, account.getName());
            statement.setString(3, account.getPhone());
            statement.setString(4, account.getCollege());
            statement.setString(5, account.getCredential());
            statement.setTimestamp(6, Timestamp.valueOf(
                    account.getCreatedAt() == null ? java.time.LocalDateTime.now() : account.getCreatedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    account.setId(keys.getLong(1));
                }
            }
        }
    }

    private RunnerAccount mapRow(ResultSet rs) throws SQLException {
        RunnerAccount account = new RunnerAccount();
        account.setId(rs.getLong("id"));
        account.setStudentId(rs.getString("student_id"));
        account.setName(rs.getString("name"));
        account.setPhone(rs.getString("phone"));
        account.setCollege(rs.getString("college"));
        account.setCredential(rs.getString("credential"));
        Timestamp created = rs.getTimestamp("created_at");
        account.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return account;
    }
}
