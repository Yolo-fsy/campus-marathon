package com.campus.marathon.web;

import com.campus.marathon.model.SessionUser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * POST /my/cancel ：参赛者取消本人报名，学号取自会话，不接受外部传入。
 */
public class MyCancelServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        try {
            String message = paymentService.cancelWithRefund(user.getStudentId());
            response.sendRedirect(request.getContextPath() + "/my?msg=" + message);
        } catch (SQLException e) {
            throw new ServletException("取消报名失败", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/my");
    }
}
