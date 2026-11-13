package com.campus.marathon.web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * POST /pay/notify ：支付宝异步通知，验签入账后应答 success，支付宝收到后才停止重发。
 */
public class PayNotifyServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Map<String, String> params = PayReturnServlet.collectParams(request);
        String result = "fail";
        try {
            boolean valid = paymentService.getAlipayService().verifyCallback(params);
            String tradeStatus = params.get("trade_status");
            if (valid && ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))) {
                paymentService.handlePaid(params.get("out_trade_no"));
                result = "success";
            }
        } catch (SQLException e) {
            // 保持 fail 应答，支付宝会重试
        }
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(result);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
