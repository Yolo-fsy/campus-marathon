package com.campus.marathon.web;

import com.alipay.api.AlipayApiException;
import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.service.PaymentService;
import com.campus.marathon.service.ValidationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET  /pay ：缴费确认页（展示 30 元报名费与订单信息）。
 * POST /pay ：创建支付订单并跳转支付宝收银台（mock 模式下直接模拟支付成功）。
 */
public class PayServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        MarathonRunner runner = resolvePendingRunner(request);
        if (runner == null) {
            response.sendRedirect(request.getContextPath() + "/apply");
            return;
        }
        if (MarathonRunner.PAY_PAID.equals(runner.getPayStatus())) {
            response.sendRedirect(request.getContextPath() + "/my");
            return;
        }
        renderPayPage(request, response, runner, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        MarathonRunner runner = resolvePendingRunner(request);
        if (runner == null) {
            response.sendRedirect(request.getContextPath() + "/apply");
            return;
        }
        if (MarathonRunner.PAY_PAID.equals(runner.getPayStatus())) {
            response.sendRedirect(request.getContextPath() + "/my");
            return;
        }
        String outTradeNo;
        try {
            outTradeNo = paymentService.createOrder(runner);
        } catch (ValidationException e) {
            renderPayPage(request, response, runner,
                    e.getFieldErrors().values().iterator().next());
            return;
        } catch (SQLException e) {
            throw new ServletException("创建支付订单失败", e);
        }

        if (paymentService.getAlipayService().isMock()) {
            // 演示模式：模拟支付宝收银台支付成功
            completeMockPayment(request, response, outTradeNo);
            return;
        }
        try {
            String form = paymentService.getAlipayService().buildPayForm(outTradeNo);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(form);
            response.getWriter().flush();
        } catch (AlipayApiException e) {
            throw new ServletException("调起支付宝收银台失败", e);
        }
    }

    /**
     * 定位待支付记录：优先取报名流程中写入会话的 pendingPayId；
     * 否则取当前登录参赛者的未支付报名（支持登录后继续支付）。
     */
    private MarathonRunner resolvePendingRunner(HttpServletRequest request) throws ServletException {
        try {
            HttpSession session = request.getSession(false);
            Long pendingId = session == null ? null : (Long) session.getAttribute("pendingPayId");
            if (pendingId != null) {
                return service.findById(pendingId);
            }
            SessionUser user = currentUser(request);
            if (user != null && user.isRunner()) {
                MarathonRunner runner = service.findByStudentId(user.getStudentId());
                if (runner != null && MarathonRunner.PAY_UNPAID.equals(runner.getPayStatus())
                        && !MarathonRunner.STATUS_CANCELLED.equals(runner.getStatus())) {
                    return runner;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new ServletException("读取待支付报名失败", e);
        }
    }

    private void completeMockPayment(HttpServletRequest request, HttpServletResponse response, String outTradeNo)
            throws IOException, ServletException {
        try {
            PaymentService.PayOutcome outcome = paymentService.handlePaid(outTradeNo);
            afterPaid(request, response, outcome);
        } catch (SQLException e) {
            throw new ServletException("模拟支付入账失败", e);
        }
    }

    /** 支付到账后的统一跳转：成功则自动登录参赛者并进入我的报名。 */
    static void afterPaid(HttpServletRequest request, HttpServletResponse response,
                          PaymentService.PayOutcome outcome) throws IOException {
        String ctx = request.getContextPath();
        switch (outcome.getResult()) {
            case SUCCESS:
            case ALREADY_PAID:
                MarathonRunner runner = outcome.getRunner();
                HttpSession old = request.getSession(false);
                if (old != null) {
                    old.invalidate();
                }
                HttpSession session = request.getSession(true);
                session.setAttribute("currentUser", SessionUser.runner(runner));
                response.sendRedirect(ctx + "/my?msg=paid");
                return;
            case FULL_REFUNDED:
                response.sendRedirect(ctx + "/login?msg=payFull&role=runner");
                return;
            default:
                response.sendRedirect(ctx + "/apply");
        }
    }

    private void renderPayPage(HttpServletRequest request, HttpServletResponse response,
                               MarathonRunner runner, String error)
            throws ServletException, IOException {
        request.setAttribute("runner", runner);
        request.setAttribute("amount", paymentService.getAlipayService().getAmount());
        request.setAttribute("mock", paymentService.getAlipayService().isMock());
        // 已登录参赛者回到「我的报名」，匿名报名者回到报名表单
        request.setAttribute("backUrl", currentUser(request) != null ? "/my" : "/apply");
        request.setAttribute("error", error);
        request.setAttribute("pageTitle", "报名费支付");
        forward(request, response, "pay.jsp");
    }
}
