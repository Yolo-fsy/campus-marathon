package com.campus.marathon.web;

import com.campus.marathon.model.Contest;
import com.campus.marathon.model.Contestant;
import com.campus.marathon.service.ContestService;
import com.campus.marathon.util.ContestSchema;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * GET /contests/contestants?contestId= 参赛者列表。
 * POST /contests/contestants 添加 / 删除。
 */
public class ContestantListServlet extends BaseServlet {

    private final ContestService contestService = new ContestService();

    @Override
    public void init() throws ServletException {
        ContestSchema.init();
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long contestId = parseId(request.getParameter("contestId"));
        try {
            Contest contest = contestService.getContest(contestId);
            List<Contestant> contestants = contestService.listContestants(contestId);
            request.setAttribute("contest", contest);
            request.setAttribute("contestants", contestants);
        } catch (SQLException e) {
            throw new ServletException("查询参赛者列表失败", e);
        }
        request.setAttribute("pageTitle", "参赛者管理");
        forward(request, response, "contest/contestant-list.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long contestId = parseId(request.getParameter("contestId"));
        String action = trim(request.getParameter("action"));
        String redirectMsg;
        try {
            if ("add".equals(action)) {
                String name = trim(request.getParameter("name"));
                String studentId = trim(request.getParameter("studentId"));
                String teamName = trim(request.getParameter("teamName"));
                contestService.addContestant(contestId, name, studentId, teamName);
                redirectMsg = "contestantAdded";
            } else if ("delete".equals(action)) {
                long contestantId = parseId(request.getParameter("contestantId"));
                contestService.removeContestant(contestId, contestantId);
                redirectMsg = "contestantRemoved";
            } else {
                redirectMsg = "unknownAction";
            }
        } catch (Exception e) {
            redirectMsg = "err:" + e.getMessage();
        }
        response.sendRedirect(request.getContextPath()
                + "/contests/contestants?contestId=" + contestId + "&msg=" + redirectMsg);
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
