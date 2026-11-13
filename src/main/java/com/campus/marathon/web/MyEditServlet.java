package com.campus.marathon.web;

import com.campus.marathon.model.EventCategory;
import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.model.SessionUser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /my/edit ：参赛者修改本人报名信息（仅允许修改自己的记录）。
 */
public class MyEditServlet extends BaseServlet {

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
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login?msg=recordMissing");
            return;
        }
        boolean resubmit = MarathonRunner.STATUS_REJECTED.equals(runner.getStatus())
                || MarathonRunner.STATUS_CANCELLED.equals(runner.getStatus());
        request.setAttribute("form", runner);
        request.setAttribute("formMode", "runner-edit");
        request.setAttribute("resubmit", resubmit);
        request.setAttribute("categories", EventCategory.values());
        request.setAttribute("pageTitle", "修改我的报名");
        request.setAttribute("activeNav", "my");
        forward(request, response, "form.jsp");
    }
}
