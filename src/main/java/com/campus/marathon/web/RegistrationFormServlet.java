package com.campus.marathon.web;

import com.campus.marathon.model.EventCategory;
import com.campus.marathon.model.MarathonRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /registrations/form ：新建报名或编辑报名表单。
 */
public class RegistrationFormServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id = parseId(request.getParameter("id"));

        if (id > 0) {
            try {
                MarathonRunner runner = service.findById(id);
                if (runner == null) {
                    redirectToList(request, response, "notFound");
                    return;
                }
                request.setAttribute("form", runner);
                request.setAttribute("formMode", "edit");
                request.setAttribute("pageTitle", "编辑报名");
            } catch (SQLException e) {
                throw new ServletException("读取报名信息失败", e);
            }
        } else {
            MarathonRunner runner = new MarathonRunner();
            runner.setEventCategory(EventCategory.HALF.getCode());
            runner.setStatus(MarathonRunner.STATUS_PENDING);
            request.setAttribute("form", runner);
            request.setAttribute("formMode", "create");
            request.setAttribute("pageTitle", "新建报名");
        }

        request.setAttribute("categories", EventCategory.values());
        request.setAttribute("activeNav", "create");
        forward(request, response, "form.jsp");
    }
}
