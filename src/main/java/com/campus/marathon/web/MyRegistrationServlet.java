package com.campus.marathon.web;

import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.model.SessionUser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /my ：参赛者查看本人报名信息。
 */
public class MyRegistrationServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        MarathonRunner runner;
        try {
            runner = service.findByStudentId(user.getStudentId());
        } catch (SQLException e) {
            throw new ServletException("读取本人报名信息失败", e);
        }
        if (runner == null) {
            // 已登录账号但还没有报名记录：直接进入报名流程
            response.sendRedirect(request.getContextPath() + "/apply?msg=noRecord");
            return;
        }
        // 已取消的报名对参赛者视为「无报名」，回到报名引导页
        if (MarathonRunner.STATUS_CANCELLED.equals(runner.getStatus())) {
            String code = request.getParameter("msg");
            if ("refunded".equals(code)) {
                request.setAttribute("flash", "已取消报名，30 元报名费已原路退回，名额已释放。");
                request.setAttribute("flashType", "success");
            } else if ("cancelled".equals(code)) {
                request.setAttribute("flash", "已取消报名，名额已释放。");
                request.setAttribute("flashType", "success");
            }
            request.setAttribute("pageTitle", "我的报名");
            request.setAttribute("activeNav", "my");
            forward(request, response, "my-empty.jsp");
            return;
        }
        request.setAttribute("runner", runner);
        request.setAttribute("viewer", "runner");
        request.setAttribute("pageTitle", "我的报名");
        request.setAttribute("activeNav", "my");
        String code = request.getParameter("msg");
        if ("updated".equals(code)) {
            request.setAttribute("flash", "报名信息已更新。");
            request.setAttribute("flashType", "success");
        } else if ("forbidden".equals(code)) {
            request.setAttribute("flash", "当前账号无权访问该页面。");
            request.setAttribute("flashType", "warning");
        } else if ("cancelled".equals(code)) {
            request.setAttribute("flash", "已取消报名，名额已释放。如需继续参赛，可点击「重新报名」再次提交。");
            request.setAttribute("flashType", "success");
        } else if ("cancelFailed".equals(code)) {
            request.setAttribute("flash", "当前报名状态无法取消，如有疑问请联系赛事管理员。");
            request.setAttribute("flashType", "warning");
        } else if ("paid".equals(code)) {
            request.setAttribute("flash", "支付成功，报名完成！名额已锁定。");
            request.setAttribute("flashType", "success");
        } else if ("exists".equals(code)) {
            request.setAttribute("flash", "你已提交报名，请先完成缴费或查看当前报名状态。");
            request.setAttribute("flashType", "warning");
        } else if ("refunded".equals(code)) {
            request.setAttribute("flash", "已取消报名，30 元报名费已原路退回，名额已释放。");
            request.setAttribute("flashType", "success");
        } else if ("refundFailed".equals(code)) {
            request.setAttribute("flash", "退款失败，请稍后重试或联系赛事管理员处理。");
            request.setAttribute("flashType", "warning");
        }
        forward(request, response, "detail.jsp");
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/login?msg=" + message);
    }
}