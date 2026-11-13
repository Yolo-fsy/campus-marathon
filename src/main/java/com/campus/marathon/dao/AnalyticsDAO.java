package com.campus.marathon.dao;

import com.campus.marathon.model.AnalyticsReport;

import java.sql.SQLException;

/**
 * 报名数据分析数据访问对象：面向聚合统计的只读查询。
 */
public interface AnalyticsDAO {

    /**
     * 基于全量报名记录聚合可视化指标（状态分布、支付漏斗、
     * 项目容量、学院交叉矩阵、每日趋势与财务汇总）。
     */
    AnalyticsReport loadReport() throws SQLException;
}
