package com.campus.marathon.web;

import com.campus.marathon.model.MarathonRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /registrations/detail ：报名详情。
 */
public class RegistrationDetailServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id = parseId(request.getParameter("id"));
        if (id <= 0) {
            redirectToList(request, response, "notFound");
            return;
        }
        try {
            MarathonRunner runner = service.findById(id);
            if (runner == null) {
                redirectToList(request, response, "notFound");
                return;
            }
            request.setAttribute("runner", runner);
        } catch (SQLException e) {
            throw new ServletException("读取报名详情失败", e);
        }
        request.setAttribute("pageTitle", "报名详情");
        request.setAttribute("activeNav", "list");
        request.setAttribute("viewer", "admin");
        forward(request, response, "detail.jsp");
    }
}
