package com.campus.marathon.web;

import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.service.ValidationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * POST /admin/registrations/review ：管理员审核（通过 / 驳回）。
 */
public class AdminReviewServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id = parseId(request.getParameter("id"));
        String status = request.getParameter("status");
        status = status == null ? "" : status.trim().toUpperCase();

        if (id <= 0
                || !(MarathonRunner.STATUS_CONFIRMED.equals(status)
                || MarathonRunner.STATUS_REJECTED.equals(status))) {
            redirectToList(request, response, "notFound");
            return;
        }

        try {
            if (MarathonRunner.STATUS_REJECTED.equals(status)) {
                // 驳回：已支付记录先退款再置为驳回，避免参赛者被驳回却仍在扣费
                String message = paymentService.rejectWithRefund(id);
                redirectToList(request, response, message);
                return;
            }
            boolean updated = service.review(id, status);
            if (!updated) {
                redirectToList(request, response, "notFound");
                return;
            }
            redirectToList(request, response, "approved");
        } catch (ValidationException e) {
            redirectToList(request, response, "full");
        } catch (SQLException e) {
            throw new ServletException("更新报名状态失败", e);
        }
    }
}
