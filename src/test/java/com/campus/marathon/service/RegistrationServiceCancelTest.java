package com.campus.marathon.service;

import com.campus.marathon.dao.RegistrationDAO;
import com.campus.marathon.model.DashboardStats;
import com.campus.marathon.model.MarathonRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 参赛者「取消报名」的单元测试。
 * 使用内存假 DAO，避免依赖真实数据库。
 */
class RegistrationServiceCancelTest {

    /** 内存版 DAO：按 id 存储记录，记录 updateStatus 的调用（包级可见，供支付测试复用）。 */
    static class InMemoryRegistrationDAO implements RegistrationDAO {
        private final Map<Long, MarathonRunner> store = new HashMap<>();
        private long statusUpdatedId = -1;
        private String statusUpdatedValue;
        private int updateStatusCalls;

        void save(MarathonRunner runner) {
            store.put(runner.getId(), runner);
        }

        MarathonRunner get(long id) {
            return store.get(id);
        }

        @Override
        public long create(MarathonRunner runner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean update(MarathonRunner runner) {
            store.put(runner.getId(), runner);
            return true;
        }

        @Override
        public boolean delete(long id) {
            return store.remove(id) != null;
        }

        @Override
        public MarathonRunner findById(long id) {
            return store.get(id);
        }

        @Override
        public MarathonRunner findByStudentId(String studentId) {
            return store.values().stream()
                    .filter(r -> studentId.equals(r.getStudentId()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean updateStatus(long id, String status) {
            MarathonRunner runner = store.get(id);
            if (runner == null) {
                return false;
            }
            updateStatusCalls++;
            statusUpdatedId = id;
            statusUpdatedValue = status;
            runner.setStatus(status);
            return true;
        }

        @Override
        public boolean updatePayment(long id, String outTradeNo, String payStatus,
                                     java.time.LocalDateTime payTime) {
            MarathonRunner runner = store.get(id);
            if (runner == null) {
                return false;
            }
            runner.setOutTradeNo(outTradeNo);
            runner.setPayStatus(payStatus);
            runner.setPayTime(payTime);
            return true;
        }

        @Override
        public MarathonRunner findByOutTradeNo(String outTradeNo) {
            return store.values().stream()
                    .filter(r -> outTradeNo != null && outTradeNo.equals(r.getOutTradeNo()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean reapply(MarathonRunner runner) {
            MarathonRunner existing = store.get(runner.getId());
            if (existing == null) {
                return false;
            }
            runner.setStatus(MarathonRunner.STATUS_PENDING);
            runner.setPayStatus(MarathonRunner.PAY_UNPAID);
            runner.setOutTradeNo(null);
            runner.setPayTime(null);
            store.put(runner.getId(), runner);
            return true;
        }

        @Override
        public List<MarathonRunner> findAll(String keyword, String category, String status, String payStatus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByStudentId(String studentId, Long excludeId) {
            MarathonRunner found = findByStudentId(studentId);
            return found != null && (excludeId == null || !excludeId.equals(found.getId()));
        }

        @Override
        public int countConfirmedByCategory(String category, Long excludeId) {
            return (int) store.values().stream()
                    .filter(r -> category.equals(r.getEventCategory()))
                    .filter(r -> MarathonRunner.STATUS_CONFIRMED.equals(r.getStatus()))
                    .filter(r -> excludeId == null || r.getId() != excludeId)
                    .count();
        }

        @Override
        public synchronized boolean tryConfirmWithCapacity(long id, String category, int capacity) {
            MarathonRunner runner = store.get(id);
            if (runner == null || MarathonRunner.STATUS_CONFIRMED.equals(runner.getStatus())) {
                return false;
            }
            long used = store.values().stream()
                    .filter(r -> category.equals(r.getEventCategory()))
                    .filter(r -> MarathonRunner.STATUS_CONFIRMED.equals(r.getStatus()))
                    .count();
            if (used >= capacity) {
                return false;
            }
            runner.setStatus(MarathonRunner.STATUS_CONFIRMED);
            return true;
        }

        @Override
        public int cancelExpiredUnpaid(java.time.LocalDateTime deadline) {
            int changed = 0;
            for (MarathonRunner runner : store.values()) {
                if (MarathonRunner.STATUS_CONFIRMED.equals(runner.getStatus())
                        && !MarathonRunner.PAY_PAID.equals(runner.getPayStatus())
                        && runner.getUpdatedAt() != null
                        && runner.getUpdatedAt().isBefore(deadline)) {
                    runner.setStatus(MarathonRunner.STATUS_CANCELLED);
                    changed++;
                }
            }
            return changed;
        }

        @Override
        public DashboardStats loadStats() {
            DashboardStats stats = new DashboardStats();
            for (MarathonRunner runner : store.values()) {
                stats.record(runner.getEventCategory(), runner.getStatus(), runner.getPayStatus(), 1);
                stats.recordPay(runner.getPayStatus(), 1);
            }
            return stats;
        }
    }

    private InMemoryRegistrationDAO dao;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        dao = new InMemoryRegistrationDAO();
        service = new RegistrationService(dao);
    }

    private MarathonRunner runnerWithStatus(long id, String studentId, String status) {
        MarathonRunner runner = new MarathonRunner();
        runner.setId(id);
        runner.setName("李四");
        runner.setStudentId(studentId);
        runner.setPhone("13800138001");
        runner.setEventCategory("MINI");
        runner.setStatus(status);
        return runner;
    }

    @Test
    @DisplayName("待审核状态可取消，状态置为 CANCELLED")
    void cancelPendingRegistration() throws SQLException {
        dao.save(runnerWithStatus(1L, "2023010203", MarathonRunner.STATUS_PENDING));

        boolean result = service.cancelOwn("2023010203");

        assertTrue(result);
        assertEquals(1, dao.updateStatusCalls);
        assertEquals(1L, dao.statusUpdatedId);
        assertEquals(MarathonRunner.STATUS_CANCELLED, dao.statusUpdatedValue);
        assertEquals(MarathonRunner.STATUS_CANCELLED, dao.get(1L).getStatus());
    }

    @Test
    @DisplayName("报名成功状态可取消，取消后释放名额（不再计入 CONFIRMED）")
    void cancelConfirmedRegistrationReleasesQuota() throws SQLException {
        dao.save(runnerWithStatus(1L, "2023010203", MarathonRunner.STATUS_CONFIRMED));
        assertEquals(1, dao.countConfirmedByCategory("MINI", null));

        boolean result = service.cancelOwn("2023010203");

        assertTrue(result);
        assertEquals(MarathonRunner.STATUS_CANCELLED, dao.get(1L).getStatus());
        assertEquals(0, dao.countConfirmedByCategory("MINI", null), "取消后名额应释放");
    }

    @Test
    @DisplayName("已取消的报名重复取消返回 false，不触发状态更新")
    void cancelAlreadyCancelledReturnsFalse() throws SQLException {
        dao.save(runnerWithStatus(1L, "2023010203", MarathonRunner.STATUS_CANCELLED));

        boolean result = service.cancelOwn("2023010203");

        assertFalse(result);
        assertEquals(0, dao.updateStatusCalls, "重复取消不应再次写库");
    }

    @Test
    @DisplayName("学号无报名记录时返回 false")
    void cancelWithoutRecordReturnsFalse() throws SQLException {
        boolean result = service.cancelOwn("2099999999");

        assertFalse(result);
        assertEquals(0, dao.updateStatusCalls);
    }

    @Test
    @DisplayName("取消只影响本人记录，他人报名不受影响")
    void cancelOnlyAffectsOwnRecord() throws SQLException {
        dao.save(runnerWithStatus(1L, "2023010203", MarathonRunner.STATUS_PENDING));
        dao.save(runnerWithStatus(2L, "2023010204", MarathonRunner.STATUS_CONFIRMED));

        service.cancelOwn("2023010203");

        assertEquals(MarathonRunner.STATUS_CANCELLED, dao.get(1L).getStatus());
        assertEquals(MarathonRunner.STATUS_CONFIRMED, dao.get(2L).getStatus(), "他人记录不应被修改");
    }

    @Test
    @DisplayName("驳回状态的报名也可取消")
    void cancelRejectedRegistration() throws SQLException {
        dao.save(runnerWithStatus(1L, "2023010203", MarathonRunner.STATUS_REJECTED));

        boolean result = service.cancelOwn("2023010203");

        assertTrue(result);
        assertEquals(MarathonRunner.STATUS_CANCELLED, dao.get(1L).getStatus());
    }

    @Test
    @DisplayName("审核通过后超过 24 小时未支付：读取统计时自动取消，不再计入报名成功")
    void expiredUnpaidConfirmedIsAutoCancelled() throws SQLException {
        MarathonRunner expired = runnerWithStatus(1L, "2023010205", MarathonRunner.STATUS_CONFIRMED);
        expired.setPayStatus(MarathonRunner.PAY_UNPAID);
        expired.setUpdatedAt(LocalDateTime.now().minusHours(MarathonRunner.PAYMENT_DEADLINE_HOURS + 1));
        dao.save(expired);

        DashboardStats stats = service.loadStats();

        assertEquals(MarathonRunner.STATUS_CANCELLED, dao.get(1L).getStatus(), "超时未支付应自动取消");
        assertEquals(0, stats.getConfirmed());
        assertEquals(1, stats.getCancelled());
    }

    @Test
    @DisplayName("缴费窗口内未支付记录不清理，但「报名成功」只统计已支付")
    void confirmedCardCountsPaidOnly() throws SQLException {
        MarathonRunner paid = runnerWithStatus(1L, "2023010206", MarathonRunner.STATUS_CONFIRMED);
        paid.setPayStatus(MarathonRunner.PAY_PAID);
        paid.setUpdatedAt(LocalDateTime.now().minusDays(30));
        dao.save(paid);

        MarathonRunner fresh = runnerWithStatus(2L, "2023010207", MarathonRunner.STATUS_CONFIRMED);
        fresh.setPayStatus(MarathonRunner.PAY_UNPAID);
        fresh.setUpdatedAt(LocalDateTime.now().minusHours(1));
        dao.save(fresh);

        DashboardStats stats = service.loadStats();

        assertEquals(MarathonRunner.STATUS_CONFIRMED, dao.get(1L).getStatus());
        assertEquals(MarathonRunner.STATUS_CONFIRMED, dao.get(2L).getStatus(), "缴费窗口内的记录不应被清理");
        assertEquals(1, stats.getConfirmed(), "仅已支付记录计入报名成功");
        assertEquals(1, stats.getUnpaid());
    }
}
