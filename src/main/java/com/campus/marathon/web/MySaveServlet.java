package com.campus.marathon.web;

import com.campus.marathon.model.EventCategory;
import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.service.ValidationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * POST /my/save ：参赛者保存本人报名信息，学号与状态不可由本人篡改。
 */
public class MySaveServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        MarathonRunner existing;
        try {
            existing = service.findByStudentId(user.getStudentId());
        } catch (SQLException e) {
            throw new ServletException("读取本人报名信息失败", e);
        }
        if (existing == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login?msg=recordMissing");
            return;
        }

        boolean resubmit = MarathonRunner.STATUS_REJECTED.equals(existing.getStatus())
                || MarathonRunner.STATUS_CANCELLED.equals(existing.getStatus());

        MarathonRunner runner = bindRunner(request);
        runner.setId(existing.getId());
        runner.setStudentId(existing.getStudentId());
        runner.setStatus(resubmit ? MarathonRunner.STATUS_PENDING : existing.getStatus());

        try {
            if (!service.update(runner)) {
                response.sendRedirect(request.getContextPath() + "/login?msg=recordMissing");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/my?msg=updated");
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getFieldErrors());
            request.setAttribute("form", runner);
            request.setAttribute("formMode", "runner-edit");
            request.setAttribute("resubmit", resubmit);
            request.setAttribute("categories", EventCategory.values());
            request.setAttribute("pageTitle", "修改我的报名");
            request.setAttribute("activeNav", "my");
            forward(request, response, "form.jsp");
        } catch (SQLException e) {
            throw new ServletException("更新报名信息失败", e);
        }
    }
}
