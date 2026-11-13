package com.campus.marathon.web;

import com.campus.marathon.model.EventCategory;
import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.model.RunnerAccount;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.service.AccountService;
import com.campus.marathon.service.ValidationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * 登录后的在线报名，分两步：
 * GET /apply 选择参赛项目；GET /apply/form?category=xxx 填写参赛人信息；POST /apply 提交。
 */
public class ApplyServlet extends BaseServlet {

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        MarathonRunner existing;
        try {
            existing = service.findByStudentId(user.getStudentId());
        } catch (SQLException e) {
            throw new ServletException("读取报名记录失败", e);
        }
        // 已有有效报名（未取消）时直接回到「我的报名」，继续支付或查看状态
        if (existing != null && !MarathonRunner.STATUS_CANCELLED.equals(existing.getStatus())) {
            response.sendRedirect(request.getContextPath() + "/my?msg=exists");
            return;
        }
        if (request.getServletPath().endsWith("/form")) {
            renderForm(request, response, prefill(user, existing, resolveCategory(request)), null);
            return;
        }
        request.setAttribute("activeCode", resolveCategory(request));
        request.setAttribute("categories", EventCategory.values());
        request.setAttribute("pageTitle", "选择参赛项目");
        request.setAttribute("activeNav", "apply");
        forward(request, response, "apply-select.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        MarathonRunner runner = bindRunner(request);
        // 学号一律取自登录账号，避免代他人报名
        runner.setStudentId(user.getStudentId());
        runner.setStatus(MarathonRunner.STATUS_PENDING);
        try {
            long id = service.applyOrReapply(runner);
            // 报名成功后进入缴费环节：将待支付记录写入会话，由 /pay 完成 30 元报名费支付
            request.getSession(true).setAttribute("pendingPayId", id);
            response.sendRedirect(request.getContextPath() + "/pay");
        } catch (ValidationException e) {
            renderForm(request, response, runner, e);
        } catch (SQLException e) {
            throw new ServletException("提交报名失败", e);
        }
    }

    /**
     * 优先沿用未取消记录中的资料，其次用注册账号的姓名与手机号。
     */
    private MarathonRunner prefill(SessionUser user, MarathonRunner existing, String categoryCode) {
        MarathonRunner runner = newRunner(categoryCode);
        runner.setStudentId(user.getStudentId());
        if (existing != null) {
            runner.setName(existing.getName());
            runner.setPhone(existing.getPhone());
            runner.setCollege(existing.getCollege());
        } else {
            try {
                RunnerAccount account = accountService.findByStudentId(user.getStudentId());
                if (account != null) {
                    runner.setName(account.getName());
                    runner.setPhone(account.getPhone());
                    runner.setCollege(account.getCollege());
                }
            } catch (SQLException e) {
                throw new IllegalStateException("读取账号信息失败", e);
            }
        }
        return runner;
    }

    /**
     * 读取项目参数：合法则使用，否则回落到默认半程马拉松。
     */
    private String resolveCategory(HttpServletRequest request) {
        String category = request.getParameter("category");
        if (EventCategory.isValid(category)) {
            return EventCategory.fromCode(category).getCode();
        }
        return EventCategory.HALF.getCode();
    }

    private MarathonRunner newRunner(String categoryCode) {
        MarathonRunner runner = new MarathonRunner();
        runner.setEventCategory(categoryCode);
        runner.setStatus(MarathonRunner.STATUS_PENDING);
        return runner;
    }

    private void renderForm(HttpServletRequest request, HttpServletResponse response,
                            MarathonRunner runner, ValidationException e)
            throws ServletException, IOException {
        request.setAttribute("errors", e == null ? null : e.getFieldErrors());
        request.setAttribute("form", runner);
        request.setAttribute("formMode", "apply");
        request.setAttribute("categories", EventCategory.values());
        request.setAttribute("pageTitle", "填写参赛人信息");
        request.setAttribute("activeNav", "apply");
        forward(request, response, "form.jsp");
    }
}
