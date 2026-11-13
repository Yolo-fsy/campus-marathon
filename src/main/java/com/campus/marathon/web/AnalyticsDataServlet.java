package com.campus.marathon.web;

import com.campus.marathon.dao.AnalyticsDAO;
import com.campus.marathon.dao.impl.AnalyticsDAOImpl;
import com.campus.marathon.model.AnalyticsReport;
import com.campus.marathon.model.EventCategory;
import com.campus.marathon.model.MarathonRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 报名数据分析 JSON 接口：/analytics/data
 * 每次请求实时聚合 marathon_runner 全量记录，供可视化页面 fetch 渲染；
 * 前端定时轮询即可看到报名 / 支付的最新变化。
 */
public class AnalyticsDataServlet extends BaseServlet {

    private static final DateTimeFormatter GENERATED_AT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AnalyticsDAO analyticsDAO = new AnalyticsDAOImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        try {
            AnalyticsReport report = analyticsDAO.loadReport();
            response.getWriter().write(toJson(report));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"聚合查询失败，请稍后重试\"}");
        }
    }

    /**
     * 手工拼装 JSON：字段与层级完全受控，无需引入序列化依赖。
     */
    private String toJson(AnalyticsReport r) {
        StringBuilder json = new StringBuilder(2048);
        json.append("{\"generatedAt\":").append(q(LocalDateTime.now().format(GENERATED_AT)))
                .append(",\"total\":").append(r.getTotal())
                .append(",\"pending\":").append(r.getPending())
                .append(",\"confirmed\":").append(r.getConfirmed())
                .append(",\"rejected\":").append(r.getRejected())
                .append(",\"cancelled\":").append(r.getCancelled())
                .append(",\"confirmRate\":").append(round4(r.getConfirmRate()))
                .append(",\"payConversion\":").append(round4(r.getPayConversion()))
                .append(",\"payCounts\":").append(mapToJson(r.getPayCounts()))
                .append(",\"revenuePaid\":").append(r.getRevenuePaid())
                .append(",\"revenueRefunded\":").append(r.getRevenueRefunded())
                .append(",\"revenueNet\":").append(r.getRevenueNet())
                .append(",\"eventCounts\":").append(mapToJson(r.getEventCounts()));

        json.append(",\"events\":{");
        EventCategory[] categories = EventCategory.values();
        for (int i = 0; i < categories.length; i++) {
            EventCategory category = categories[i];
            json.append(q(category.getCode())).append(":{\"label\":").append(q(category.getLabel()))
                    .append(",\"capacity\":").append(category.getCapacity()).append('}');
            if (i < categories.length - 1) {
                json.append(',');
            }
        }
        json.append('}');

        json.append(",\"collegeTotals\":[");
        int i = 0;
        for (Map.Entry<String, Integer> entry : r.getCollegeTotals().entrySet()) {
            if (i++ > 0) {
                json.append(',');
            }
            json.append("{\"name\":").append(q(entry.getKey()))
                    .append(",\"count\":").append(entry.getValue()).append('}');
        }
        json.append(']');

        json.append(",\"crossMatrix\":[");
        i = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : r.getCrossMatrix().entrySet()) {
            if (i++ > 0) {
                json.append(',');
            }
            json.append("{\"college\":").append(q(entry.getKey()))
                    .append(",\"counts\":").append(mapToJson(entry.getValue())).append('}');
        }
        json.append(']');

        json.append(",\"daily\":[");
        i = 0;
        for (Map.Entry<String, Integer> entry : r.getDaily().entrySet()) {
            if (i++ > 0) {
                json.append(',');
            }
            json.append("{\"date\":").append(q(entry.getKey()))
                    .append(",\"count\":").append(entry.getValue()).append('}');
        }
        json.append(']');

        return json.append('}').toString();
    }

    /**
     * Map 序列化为 JSON 对象（键为受控枚举或日期，仅值需要转义）。
     */
    private String mapToJson(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder(128).append('{');
        int i = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (i++ > 0) {
                sb.append(',');
            }
            sb.append(q(entry.getKey())).append(':').append(entry.getValue());
        }
        return sb.append('}').toString();
    }

    /**
     * JSON 字符串字面量：转义反斜杠、双引号与控制字符。
     */
    private String q(String raw) {
        if (raw == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 8).append('"');
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            switch (ch) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.append('"').toString();
    }

    /**
     * 比率保留 4 位小数，避免浮点噪声进入 JSON。
     */
    private String round4(double value) {
        return new BigDecimal(value).setScale(4, RoundingMode.HALF_UP).toPlainString();
    }
}
