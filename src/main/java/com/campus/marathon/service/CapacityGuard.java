package com.campus.marathon.service;

import com.campus.marathon.dao.RegistrationDAO;

import java.sql.SQLException;

/**
 * 名额确认守卫：JVM 级互斥锁 + 数据库原子 UPDATE 双重防超卖。
 *
 * 说明：PaymentService（支付到账自动确认）与 RegistrationService（管理员审核确认）
 * 共享同一把锁，单机部署（单个 Tomcat JVM）下并发确认被串行化；
 * 数据库层的 tryConfirmWithCapacity 原子 UPDATE 作为第二道防线。
 * 若未来改为多实例部署，需将锁下沉为数据库行锁或分布式锁。
 */
public final class CapacityGuard {

    private static final Object LOCK = new Object();

    private CapacityGuard() {
    }

    /**
     * 在互斥锁内执行原子名额确认。返回 true 表示确认成功，false 表示名额已满。
     */
    public static boolean confirmWithCapacity(RegistrationDAO dao, long id, String category, int capacity)
            throws SQLException {
        synchronized (LOCK) {
            return dao.tryConfirmWithCapacity(id, category, capacity);
        }
    }
}
