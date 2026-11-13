package com.campus.marathon.web;

import com.campus.marathon.service.PaymentService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * GET /pay/return ：支付宝同步回跳，验签后入账。
 */
public class PayReturnServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Map<String, String> params = collectParams(request);
        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        // 用户中途放弃支付时支付宝会以空参数回跳，此时不应报“校验未通过”，直接回到缴费页
        if (outTradeNo == null || outTradeNo.trim().isEmpty() || params.get("sign") == null) {
            response.sendRedirect(request.getContextPath() + "/pay");
            return;
        }
        try {
            if (!paymentService.getAlipayService().verifyCallback(params)) {
                response.sendRedirect(request.getContextPath() + "/login?msg=payInvalid&role=runner");
                return;
            }
            if (tradeStatus != null && !tradeStatus.isEmpty()
                    && !"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                response.sendRedirect(request.getContextPath() + "/pay");
                return;
            }
            PaymentService.PayOutcome outcome = paymentService.handlePaid(outTradeNo);
            PayServlet.afterPaid(request, response, outcome);
        } catch (SQLException e) {
            throw new ServletException("支付入账失败", e);
        }
    }

    static Map<String, String> collectParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                params.put(entry.getKey(), values[0]);
            }
        }
        return params;
    }
}
