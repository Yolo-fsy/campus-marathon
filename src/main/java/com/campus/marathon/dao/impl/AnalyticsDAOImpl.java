package com.campus.marathon.dao.impl;

import com.campus.marathon.dao.AnalyticsDAO;
import com.campus.marathon.model.AnalyticsReport;
import com.campus.marathon.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * 分析专用 DAO：用 GROUP BY 聚合 SQL 在数据库侧完成统计，
 * 避免把全量明细拉回内存计算；同一连接内执行多条查询。
 */
public class AnalyticsDAOImpl implements AnalyticsDAO {

    @Override
    public AnalyticsReport loadReport() throws SQLException {
        AnalyticsReport report = new AnalyticsReport();
        try (Connection conn = DBUtil.getConnection()) {
            loadStatus(conn, report);
            loadPayAndRevenue(conn, report);
            loadEventCounts(conn, report);
            loadCollegeCross(conn, report);
            loadDaily(conn, report);
        }
        return report;
    }

    /** 报名状态 × 支付状态分布。 */
    private void loadStatus(Connection conn, AnalyticsReport report) throws SQLException {
        String sql = "SELECT status, pay_status, COUNT(*) AS cnt FROM marathon_runner "
                + "GROUP BY status, pay_status";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                report.recordStatus(rs.getString("status"), rs.getString("pay_status"), rs.getInt("cnt"));
            }
        }
    }

    /** 支付状态分布 + 收退款金额小计。 */
    private void loadPayAndRevenue(Connection conn, AnalyticsReport report) throws SQLException {
        String sql = "SELECT pay_status, COUNT(*) AS cnt, COALESCE(SUM(pay_amount), 0) AS amount "
                + "FROM marathon_runner GROUP BY pay_status";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                report.recordPay(rs.getString("pay_status"), rs.getInt("cnt"), rs.getBigDecimal("amount"));
            }
        }
    }

    /** 各参赛项目报名人数。 */
    private void loadEventCounts(Connection conn, AnalyticsReport report) throws SQLException {
        String sql = "SELECT event_category, COUNT(*) AS cnt FROM marathon_runner GROUP BY event_category";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                report.recordEvent(rs.getString("event_category"), rs.getInt("cnt"));
            }
        }
    }

    /** 学院 × 项目交叉矩阵（空学院不参与统计），并按学院合计降序排列。 */
    private void loadCollegeCross(Connection conn, AnalyticsReport report) throws SQLException {
        String sql = "SELECT college, event_category, COUNT(*) AS cnt FROM marathon_runner "
                + "WHERE college IS NOT NULL AND college <> '' "
                + "GROUP BY college, event_category";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                report.recordCollegeCross(rs.getString("college"), rs.getString("event_category"), rs.getInt("cnt"));
            }
        }
        report.sortCollegesByTotalDesc();
    }

    /** 每日新增报名趋势。 */
    private void loadDaily(Connection conn, AnalyticsReport report) throws SQLException {
        String sql = "SELECT CAST(created_at AS DATE) AS day, COUNT(*) AS cnt FROM marathon_runner "
                + "GROUP BY CAST(created_at AS DATE) ORDER BY day";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDate day = rs.getDate("day").toLocalDate();
                report.recordDaily(day, rs.getInt("cnt"));
            }
        }
    }
}
