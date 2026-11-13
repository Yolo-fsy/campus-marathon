package com.campus.marathon.dao.impl;

import com.campus.marathon.dao.RegistrationDAO;
import com.campus.marathon.model.DashboardStats;
import com.campus.marathon.model.MarathonRunner;
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
 * 基于 JDBC 的报名记录 DAO 实现。
 */
public class RegistrationDAOImpl implements RegistrationDAO {

    private static final String COLUMNS =
            "id, name, student_id, phone, college, event_category, status, remark, "
                    + "pay_status, out_trade_no, pay_amount, pay_time, created_at, updated_at";

    @Override
    public long create(MarathonRunner runner) throws SQLException {
        String sql = "INSERT INTO marathon_runner "
                + "(name, student_id, phone, college, event_category, status, remark) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, runner.getName());
            ps.setString(2, runner.getStudentId());
            ps.setString(3, runner.getPhone());
            ps.setString(4, runner.getCollege());
            ps.setString(5, runner.getEventCategory());
            ps.setString(6, runner.getStatus());
            ps.setString(7, runner.getRemark());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("新增报名失败，影响行数为 0");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    runner.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("新增报名失败，未获取到主键");
    }

    @Override
    public boolean update(MarathonRunner runner) throws SQLException {
        String sql = "UPDATE marathon_runner SET name = ?, student_id = ?, phone = ?, college = ?, "
                + "event_category = ?, status = ?, remark = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runner.getName());
            ps.setString(2, runner.getStudentId());
            ps.setString(3, runner.getPhone());
            ps.setString(4, runner.getCollege());
            ps.setString(5, runner.getEventCategory());
            ps.setString(6, runner.getStatus());
            ps.setString(7, runner.getRemark());
            ps.setLong(8, runner.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM marathon_runner WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public MarathonRunner findById(long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM marathon_runner WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public MarathonRunner findByStudentId(String studentId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM marathon_runner WHERE student_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public boolean updateStatus(long id, String status) throws SQLException {
        String sql = "UPDATE marathon_runner SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean tryConfirmWithCapacity(long id, String category, int capacity) throws SQLException {
        // 单条 UPDATE 完成「校验名额 + 置为已确认」：子查询与更新在同一语句内执行，
        // 数据库保证其原子性，并发到账时后到的语句会读到先到语句已确认的计数，从而拒绝超卖。
        // 派生表包裹一层以兼容 MySQL「不能在 UPDATE 中直接子查询同表」的限制。
        String sql = "UPDATE marathon_runner SET status = 'CONFIRMED', updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND status <> 'CONFIRMED' AND ("
                + "SELECT cnt FROM (SELECT COUNT(*) AS cnt FROM marathon_runner "
                + "WHERE event_category = ? AND status = 'CONFIRMED') occupied) < ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, category);
            ps.setInt(3, capacity);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updatePayment(long id, String outTradeNo, String payStatus, LocalDateTime payTime)
            throws SQLException {
        String sql = "UPDATE marathon_runner SET out_trade_no = ?, pay_status = ?, pay_time = ?, "
                + "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, outTradeNo);
            ps.setString(2, payStatus);
            ps.setTimestamp(3, payTime == null ? null : Timestamp.valueOf(payTime));
            ps.setLong(4, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public MarathonRunner findByOutTradeNo(String outTradeNo) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM marathon_runner WHERE out_trade_no = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, outTradeNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public boolean reapply(MarathonRunner runner) throws SQLException {
        String sql = "UPDATE marathon_runner SET name = ?, phone = ?, college = ?, event_category = ?, remark = ?, "
                + "status = ?, pay_status = ?, out_trade_no = NULL, pay_time = NULL, "
                + "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runner.getName());
            ps.setString(2, runner.getPhone());
            ps.setString(3, runner.getCollege());
            ps.setString(4, runner.getEventCategory());
            ps.setString(5, runner.getRemark());
            ps.setString(6, MarathonRunner.STATUS_PENDING);
            ps.setString(7, MarathonRunner.PAY_UNPAID);
            ps.setLong(8, runner.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public List<MarathonRunner> findAll(String keyword, String category, String status, String payStatus)
            throws SQLException {
        List<String> conditions = new ArrayList<>();
        List<String> parameters = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            conditions.add("(name LIKE ? OR student_id LIKE ? OR phone LIKE ? OR college LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        if (category != null && !category.trim().isEmpty()) {
            conditions.add("event_category = ?");
            parameters.add(category.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            conditions.add("status = ?");
            parameters.add(status.trim());
        }
        if (payStatus != null && !payStatus.trim().isEmpty()) {
            conditions.add("pay_status = ?");
            parameters.add(payStatus.trim());
        }

        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM marathon_runner");
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY created_at DESC, id DESC");

        List<MarathonRunner> runners = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                ps.setString(i + 1, parameters.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    runners.add(mapRow(rs));
                }
            }
        }
        return runners;
    }

    @Override
    public boolean existsByStudentId(String studentId, Long excludeId) throws SQLException {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM marathon_runner WHERE student_id = ?"
                : "SELECT COUNT(*) FROM marathon_runner WHERE student_id = ? AND id <> ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            if (excludeId != null) {
                ps.setLong(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public int countConfirmedByCategory(String category, Long excludeId) throws SQLException {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM marathon_runner WHERE event_category = ? AND status = ?"
                : "SELECT COUNT(*) FROM marathon_runner WHERE event_category = ? AND status = ? AND id <> ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setString(2, MarathonRunner.STATUS_CONFIRMED);
            if (excludeId != null) {
                ps.setLong(3, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public int cancelExpiredUnpaid(LocalDateTime deadline) throws SQLException {
        // 审核通过即进入 24 小时缴费窗口；窗口过期仍未付款（或已退款）的记录置为已取消，
        // 释放预占名额，避免长期停留在「报名成功但未支付」的矛盾状态。
        String sql = "UPDATE marathon_runner SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP "
                + "WHERE status = 'CONFIRMED' AND (pay_status IS NULL OR pay_status <> 'PAID') "
                + "AND updated_at < ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(deadline));
            return ps.executeUpdate();
        }
    }

    @Override
    public DashboardStats loadStats() throws SQLException {
        String sql = "SELECT event_category, status, pay_status, COUNT(*) AS total FROM marathon_runner "
                + "GROUP BY event_category, status, pay_status";
        String paySql = "SELECT pay_status, COUNT(*) AS total FROM marathon_runner GROUP BY pay_status";
        DashboardStats stats = new DashboardStats();
        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stats.record(rs.getString("event_category"), rs.getString("status"),
                            rs.getString("pay_status"), rs.getInt("total"));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(paySql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stats.recordPay(rs.getString("pay_status"), rs.getInt("total"));
                }
            }
        }
        return stats;
    }

    private MarathonRunner mapRow(ResultSet rs) throws SQLException {
        MarathonRunner runner = new MarathonRunner();
        runner.setId(rs.getLong("id"));
        runner.setName(rs.getString("name"));
        runner.setStudentId(rs.getString("student_id"));
        runner.setPhone(rs.getString("phone"));
        runner.setCollege(rs.getString("college"));
        runner.setEventCategory(rs.getString("event_category"));
        runner.setStatus(rs.getString("status"));
        runner.setRemark(rs.getString("remark"));
        runner.setPayStatus(rs.getString("pay_status"));
        runner.setOutTradeNo(rs.getString("out_trade_no"));
        runner.setPayAmount(rs.getBigDecimal("pay_amount"));
        runner.setPayTime(toLocalDateTime(rs.getTimestamp("pay_time")));
        runner.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        runner.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return runner;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
