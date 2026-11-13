package com.campus.marathon.web;

import com.campus.marathon.model.EventCategory;
import com.campus.marathon.model.MarathonRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * GET /registrations ：报名列表（支持关键词、项目、状态筛选）。
 */
public class RegistrationListServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = trim(request.getParameter("keyword"));
        String category = trim(request.getParameter("category"));
        String status = trim(request.getParameter("status"));
        String payStatus = trim(request.getParameter("payStatus"));

        try {
            List<MarathonRunner> runners = service.search(keyword, category, status, payStatus);
            request.setAttribute("runners", runners);
            request.setAttribute("stats", service.loadStats());
        } catch (SQLException e) {
            throw new ServletException("查询报名列表失败", e);
        }

        request.setAttribute("categories", EventCategory.values());
        request.setAttribute("keyword", keyword);
        request.setAttribute("selectedCategory", category);
        request.setAttribute("selectedStatus", status);
        request.setAttribute("selectedPayStatus", payStatus);
        request.setAttribute("pageTitle", "报名总览");
        request.setAttribute("activeNav", "list");
        exposeFlash(request);
        forward(request, response, "list.jsp");
    }

    private void exposeFlash(HttpServletRequest request) {
        String code = request.getParameter("msg");
        if (code == null) {
            return;
        }
        switch (code) {
            case "created":
                request.setAttribute("flash", "报名创建成功，记录已进入待审核队列。");
                request.setAttribute("flashType", "success");
                break;
            case "updated":
                request.setAttribute("flash", "报名信息已更新。");
                request.setAttribute("flashType", "success");
                break;
            case "deleted":
                request.setAttribute("flash", "报名记录已删除。");
                request.setAttribute("flashType", "success");
                break;
            case "approved":
                request.setAttribute("flash", "该报名已通过审核，名额已锁定。");
                request.setAttribute("flashType", "success");
                break;
            case "rejected":
                request.setAttribute("flash", "该报名已驳回，参赛者可修改信息后重新提交。");
                request.setAttribute("flashType", "warning");
                break;
            case "refunded":
                request.setAttribute("flash", "该报名已驳回，30 元报名费已原路退回参赛者账户。");
                request.setAttribute("flashType", "success");
                break;
            case "refundFailed":
                request.setAttribute("flash", "退款失败，报名未驳回：请检查支付宝退款状态后重试，或让参赛者自行取消报名。");
                request.setAttribute("flashType", "warning");
                break;
            case "full":
                request.setAttribute("flash", "该项目名额已满，无法通过审核。");
                request.setAttribute("flashType", "warning");
                break;
            case "notFound":
                request.setAttribute("flash", "未找到对应的报名记录，可能已被删除。");
                request.setAttribute("flashType", "warning");
                break;
            case "forbidden":
                request.setAttribute("flash", "当前账号无权访问该页面。");
                request.setAttribute("flashType", "warning");
                break;
            default:
                break;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
