package com.campus.marathon.web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 报名数据分析页面入口：/analytics
 * 页面本身是壳，指标由前端 fetch /analytics/data 动态渲染。
 */
public class AnalyticsPageServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("activeNav", "analytics");
        request.setAttribute("pageTitle", "报名数据分析");
        forward(request, response, "analytics.jsp");
    }
}
