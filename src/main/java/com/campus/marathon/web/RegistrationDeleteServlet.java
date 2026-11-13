package com.campus.marathon.web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * POST /registrations/delete ：删除报名记录。
 */
public class RegistrationDeleteServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id = parseId(request.getParameter("id"));
        if (id <= 0) {
            redirectToList(request, response, "notFound");
            return;
        }
        try {
            if (service.delete(id)) {
                redirectToList(request, response, "deleted");
            } else {
                redirectToList(request, response, "notFound");
            }
        } catch (SQLException e) {
            throw new ServletException("删除报名记录失败", e);
        }
    }
}
