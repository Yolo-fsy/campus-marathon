package com.campus.marathon.web;

import com.campus.marathon.model.Contest;
import com.campus.marathon.service.ContestService;
import com.campus.marathon.util.ContestSchema;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /contests/form 新建竞赛页。
 * POST /contests/form 创建竞赛。
 */
public class ContestFormServlet extends BaseServlet {

    private final ContestService contestService = new ContestService();

    @Override
    public void init() throws ServletException {
        ContestSchema.init();
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id = parseId(request.getParameter("id"));
        if (id > 0) {
            try {
                Contest contest = contestService.getContest(id);
                request.setAttribute("contest", contest);
            } catch (SQLException e) {
                throw new ServletException("查询竞赛失败", e);
            }
        }
        request.setAttribute("pageTitle", "新建/编辑竞赛");
        forward(request, response, "contest/contest-form.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id = parseId(request.getParameter("id"));
        String name = trim(request.getParameter("name"));
        String description = trim(request.getParameter("description"));
        String redirectMsg;
        try {
            if (id > 0) {
                contestService.updateBasicInfo(id, name, description);
                redirectMsg = "updated";
            } else {
                contestService.create(name, description);
                redirectMsg = "created";
            }
        } catch (Exception e) {
            redirectMsg = "err:" + e.getMessage();
        }
        response.sendRedirect(request.getContextPath() + "/contests?msg=" + redirectMsg);
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
