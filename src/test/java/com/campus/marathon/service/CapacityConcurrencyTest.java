package com.campus.marathon.service;

import com.campus.marathon.dao.impl.RegistrationDAOImpl;
import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.util.DBUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 容量原子确认的真实数据库并发压测（H2 内存库）：
 * 1. 50 个并发支付到账抢 10 个名额，验证恰好 10 个成功、绝不超卖；
 * 2. 800 次报名写入，验证吞吐满足「2 小时 800 人」的量级。
 */
class CapacityConcurrencyTest {

    private static final RegistrationDAOImpl DAO = new RegistrationDAOImpl();

    @BeforeAll
    static void initSchema() throws Exception {
        try (Connection conn = DBUtil.getConnection(); Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS marathon_runner");
            st.execute("CREATE TABLE marathon_runner ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT,"
                    + "name VARCHAR(50) NOT NULL,"
                    + "student_id VARCHAR(20) NOT NULL,"
                    + "phone VARCHAR(20) NOT NULL,"
                    + "college VARCHAR(60) DEFAULT NULL,"
                    + "event_category VARCHAR(20) NOT NULL,"
                    + "status VARCHAR(20) NOT NULL DEFAULT 'PENDING',"
                    + "pay_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',"
                    + "out_trade_no VARCHAR(64) DEFAULT NULL,"
                    + "pay_amount DECIMAL(10,2) NOT NULL DEFAULT 30.00,"
                    + "pay_time DATETIME DEFAULT NULL,"
                    + "remark VARCHAR(200) DEFAULT NULL,"
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY (id),"
                    + "UNIQUE KEY uk_student_id (student_id),"
                    + "KEY idx_category_status (event_category, status)"
                    + ")");
        }
    }

    @Test
    void concurrentConfirmNeverOversells() throws Exception {
        int contenders = 50;
        int capacity = 10;
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < contenders; i++) {
            MarathonRunner r = new MarathonRunner();
            r.setName("并发选手" + i);
            r.setStudentId(String.format("31%08d", i));
            r.setPhone("13900000000");
            r.setEventCategory("MINI");
            r.setStatus(MarathonRunner.STATUS_PENDING);
            DAO.create(r);
            ids.add(r.getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch fire = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (long id : ids) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    fire.await(5, TimeUnit.SECONDS);
                    if (CapacityGuard.confirmWithCapacity(DAO, id, "MINI", capacity)) {
                        success.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS), "线程未就绪");
        fire.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(capacity, success.get(), "确认成功数必须恰好等于名额数，不允许超卖");
        assertEquals(capacity, DAO.countConfirmedByCategory("MINI", null), "数据库 CONFIRMED 数必须恰好等于名额数");
    }

    @Test
    void eightHundredInsertsThroughput() throws Exception {
        int total = 800;
        long start = System.nanoTime();
        for (int i = 0; i < total; i++) {
            MarathonRunner r = new MarathonRunner();
            r.setName("压测" + i);
            r.setStudentId(String.format("32%08d", i));
            r.setPhone("13800000000");
            r.setEventCategory("FUN");
            r.setStatus(MarathonRunner.STATUS_PENDING);
            DAO.create(r);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("800 次报名写入耗时: " + elapsedMs + "ms");
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM marathon_runner WHERE student_id LIKE '32%'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            assertEquals(total, rs.getInt(1), "800 条报名必须全部落库");
        }
        // 2 小时窗口 = 7,200,000ms，写入耗时应远小于该值
        assertTrue(elapsedMs < 120_000, "800 次写入耗时异常: " + elapsedMs + "ms");
    }
}
