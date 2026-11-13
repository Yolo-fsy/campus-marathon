package com.campus.marathon.web;

import com.campus.marathon.model.EventCategory;
import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.service.ValidationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * POST /registrations/save ：创建或更新报名记录。
 */
public class RegistrationSaveServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idValue = request.getParameter("id");
        boolean creating = idValue == null || idValue.trim().isEmpty();
        long id = parseId(idValue);

        MarathonRunner runner = bindRunner(request);
        if (!creating) {
            runner.setId(id);
        }

        try {
            if (creating) {
                service.create(runner);
                redirectToList(request, response, "created");
            } else {
                if (id <= 0 || !service.update(runner)) {
                    redirectToList(request, response, "notFound");
                    return;
                }
                redirectToList(request, response, "updated");
            }
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getFieldErrors());
            request.setAttribute("form", runner);
            request.setAttribute("formMode", creating ? "create" : "edit");
            request.setAttribute("categories", EventCategory.values());
            request.setAttribute("pageTitle", creating ? "新建报名" : "编辑报名");
            request.setAttribute("activeNav", creating ? "create" : "list");
            forward(request, response, "form.jsp");
        } catch (SQLException e) {
            throw new ServletException("保存报名信息失败", e);
        }
    }
}
