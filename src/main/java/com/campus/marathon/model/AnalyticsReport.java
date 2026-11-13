package com.campus.marathon.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报名数据分析报告：由聚合 SQL 装配，不持久化。
 */
public class AnalyticsReport {

    private int total;
    private int pending;
    private int confirmed;
    private int rejected;
    private int cancelled;

    private final Map<String, Integer> payCounts = new LinkedHashMap<>();
    private BigDecimal revenuePaid = BigDecimal.ZERO;
    private BigDecimal revenueRefunded = BigDecimal.ZERO;

    private final Map<String, Integer> eventCounts = new LinkedHashMap<>();
    private final Map<String, Integer> collegeTotals = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> crossMatrix = new LinkedHashMap<>();
    private final Map<String, Integer> daily = new LinkedHashMap<>();

    /**
     * 汇总一个（状态 + 支付状态）分组。
     * 「报名成功」只认审核通过且已支付，未支付的确认记录按取消口径统计，与列表页保持一致。
     */
    public void recordStatus(String status, String payStatus, int count) {
        total += count;
        if (MarathonRunner.STATUS_CONFIRMED.equals(status)) {
            if (MarathonRunner.PAY_PAID.equals(payStatus)) {
                confirmed += count;
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
     * 汇总一个支付状态分组（含金额小计）。
     */
    public void recordPay(String payStatus, int count, BigDecimal amount) {
        payCounts.merge(payStatus, count, Integer::sum);
        if (MarathonRunner.PAY_PAID.equals(payStatus)) {
            revenuePaid = revenuePaid.add(amount);
        } else if (MarathonRunner.PAY_REFUNDED.equals(payStatus)) {
            revenueRefunded = revenueRefunded.add(amount);
        }
    }

    /**
     * 汇总一个参赛项目分组。
     */
    public void recordEvent(String category, int count) {
        eventCounts.merge(category, count, Integer::sum);
    }

    /**
     * 汇总学院 × 项目交叉分组，同时累加学院合计。
     */
    public void recordCollegeCross(String college, String category, int count) {
        crossMatrix.computeIfAbsent(college, key -> new LinkedHashMap<>())
                .merge(category, count, Integer::sum);
        collegeTotals.merge(college, count, Integer::sum);
    }

    /**
     * 记录某天的报名人数（按日期升序写入）。
     */
    public void recordDaily(java.time.LocalDate day, int count) {
        daily.put(day.toString(), count);
    }

    /**
     * 按报名总数降序排列学院维度，供 TOP 榜与热力矩阵使用。
     */
    public void sortCollegesByTotalDesc() {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(collegeTotals.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        LinkedHashMap<String, Integer> reorderedTotals = new LinkedHashMap<>();
        LinkedHashMap<String, Map<String, Integer>> reorderedCross = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entries) {
            reorderedTotals.put(entry.getKey(), entry.getValue());
            reorderedCross.put(entry.getKey(), crossMatrix.get(entry.getKey()));
        }
        collegeTotals.clear();
        collegeTotals.putAll(reorderedTotals);
        crossMatrix.clear();
        crossMatrix.putAll(reorderedCross);
    }

    /** 确认率 = CONFIRMED / 全部报名。 */
    public double getConfirmRate() {
        return total == 0 ? 0.0 : (double) confirmed / total;
    }

    /** 支付转化率 = 已支付 / 全部报名（支付即确认，不再用已确认做分母）。 */
    public double getPayConversion() {
        Integer paid = payCounts.get(MarathonRunner.PAY_PAID);
        return total == 0 || paid == null ? 0.0 : (double) paid / total;
    }

    public BigDecimal getRevenueNet() {
        return revenuePaid.subtract(revenueRefunded);
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

    public int getCancelled() {
        return cancelled;
    }

    public Map<String, Integer> getPayCounts() {
        return payCounts;
    }

    public BigDecimal getRevenuePaid() {
        return revenuePaid;
    }

    public BigDecimal getRevenueRefunded() {
        return revenueRefunded;
    }

    public Map<String, Integer> getEventCounts() {
        return eventCounts;
    }

    public Map<String, Integer> getCollegeTotals() {
        return collegeTotals;
    }

    public Map<String, Map<String, Integer>> getCrossMatrix() {
        return crossMatrix;
    }

    public Map<String, Integer> getDaily() {
        return daily;
    }
}
