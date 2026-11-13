package com.campus.marathon.dao;

import com.campus.marathon.model.DashboardStats;
import com.campus.marathon.model.MarathonRunner;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报名记录数据访问对象。
 */
public interface RegistrationDAO {

    /**
     * 新增报名，返回生成的主键。
     */
    long create(MarathonRunner runner) throws SQLException;

    /**
     * 按主键更新报名信息，返回是否更新成功。
     */
    boolean update(MarathonRunner runner) throws SQLException;

    /**
     * 按主键删除报名记录。
     */
    boolean delete(long id) throws SQLException;

    /**
     * 按主键查询，不存在时返回 null。
     */
    MarathonRunner findById(long id) throws SQLException;

    /**
     * 按学号查询报名记录，不存在时返回 null。
     */
    MarathonRunner findByStudentId(String studentId) throws SQLException;

    /**
     * 仅更新报名状态（审核通过 / 驳回 / 取消），返回是否更新成功。
     */
    boolean updateStatus(long id, String status) throws SQLException;

    /**
     * 更新支付信息（订单号、支付状态、支付时间），返回是否更新成功。
     */
    boolean updatePayment(long id, String outTradeNo, String payStatus, java.time.LocalDateTime payTime)
            throws SQLException;

    /**
     * 按支付订单号查询报名记录，不存在时返回 null。
     */
    MarathonRunner findByOutTradeNo(String outTradeNo) throws SQLException;

    /**
     * 取消后重新报名：更新基本信息并重置报名状态与支付信息。
     */
    boolean reapply(MarathonRunner runner) throws SQLException;

    /**
     * 条件查询报名列表，参数为空表示不限制该条件。
     */
    List<MarathonRunner> findAll(String keyword, String category, String status, String payStatus)
            throws SQLException;

    /**
     * 判断学号是否已被占用，excludeId 用于编辑时排除自身。
     */
    boolean existsByStudentId(String studentId, Long excludeId) throws SQLException;

    /**
     * 统计某项目已确认（占用名额）的人数。
     */
    int countConfirmedByCategory(String category, Long excludeId) throws SQLException;

    /**
     * 原子确认名额：仅当该项目当前确认人数低于限额时才将记录置为 CONFIRMED，
     * 由单条 UPDATE 在数据库内完成校验与写入，避免并发支付到账时的超卖。
     * 返回 true 表示确认成功；false 表示名额已被并发请求占满。
     */
    boolean tryConfirmWithCapacity(long id, String category, int capacity) throws SQLException;

    /**
     * 将审核通过后超过缴费期限仍未支付（或已退款）的记录置为已取消，返回更新行数。
     * 用于在读取统计前惰性清理超时订单，避免出现「报名成功但从未付款」的脏数据。
     */
    int cancelExpiredUnpaid(LocalDateTime deadline) throws SQLException;

    /**
     * 按项目和状态分组统计。
     */
    DashboardStats loadStats() throws SQLException;
}
