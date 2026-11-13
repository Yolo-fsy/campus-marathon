package com.campus.marathon.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 报名总览统计，列表页顶部卡片使用。
 */
public class DashboardStats {

    private int total;
    private int pending;
    private int confirmed;
    private int rejected;
    private int cancelled;
    private int unpaid;
    private final Map<String, Integer> confirmedByCategory = new LinkedHashMap<>();

    /**
     * 汇总一组（项目 + 状态 + 支付状态 + 数量）的统计结果。
     * 「报名成功」只认「审核通过且已支付」：审核通过但未支付/已退款的记录
     * 不占名额，也不计入报名成功，统一按取消口径统计，保证卡片与容量条自洽。
     */
    public void record(String category, String status, String payStatus, int count) {
        total += count;
        if (MarathonRunner.STATUS_CONFIRMED.equals(status)) {
            if (MarathonRunner.PAY_PAID.equals(payStatus)) {
                confirmed += count;
                confirmedByCategory.merge(category, count, Integer::sum);
            } else {
                cancelled += count;
            }
        } else if (MarathonRunner.STATUS_REJECTED.equals(status)) {
            rejected += count;
        } else if (MarathonRunner.STATUS_CANCELLED.equals(status)) {
            cancelled += count;
        } else {
            pending += count;
        }
    }

    /**
     * 汇总支付状态的统计结果。
     */
    public void recordPay(String payStatus, int count) {
        if (MarathonRunner.PAY_UNPAID.equals(payStatus)) {
            unpaid += count;
        }
    }

    public int getCancelled() {
        return cancelled;
    }

    public int getUnpaid() {
        return unpaid;
    }

    public int getTotal() {
        return total;
    }

    public int getPending() {
        return pending;
    }

    public int getConfirmed() {
        return confirmed;
    }

    public int getRejected() {
        return rejected;
    }

    public Map<String, Integer> getConfirmedByCategory() {
        return confirmedByCategory;
    }
}
