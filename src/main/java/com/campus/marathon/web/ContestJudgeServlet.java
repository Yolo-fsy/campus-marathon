package com.campus.marathon.web;

import com.campus.marathon.model.Contest;
import com.campus.marathon.model.Judge;
import com.campus.marathon.service.ContestService;
import com.campus.marathon.service.JudgeService;
import com.campus.marathon.util.ContestSchema;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * GET /contests/judges?contestId= 评审管理。
 * POST /contests/judges 添加或删除评审。
 */
public class ContestJudgeServlet extends BaseServlet {

    private final JudgeService judgeService = new JudgeService();
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
            List<Judge> judges = judgeService.listByContest(contestId);
            request.setAttribute("contest", contest);
            request.setAttribute("judges", judges);
        } catch (SQLException e) {
            throw new ServletException("查询评审列表失败", e);
        }
        request.setAttribute("pageTitle", "评审管理");
        forward(request, response, "contest/contest-judges.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long contestId = parseId(request.getParameter("contestId"));
        String action = trim(request.getParameter("action"));
        String redirectMsg;
        try {
            if ("add".equals(action)) {
                String username = trim(request.getParameter("username"));
                String displayName = trim(request.getParameter("displayName"));
                String password = trim(request.getParameter("password"));
                judgeService.addJudge(contestId, username, displayName, password);
                redirectMsg = "judgeAdded";
            } else if ("remove".equals(action)) {
                long judgeId = parseId(request.getParameter("judgeId"));
                judgeService.removeJudge(contestId, judgeId);
                redirectMsg = "judgeRemoved";
            } else {
                redirectMsg = "unknownAction";
            }
        } catch (Exception e) {
            redirectMsg = "err:" + e.getMessage();
        }
        response.sendRedirect(request.getContextPath()
                + "/contests/judges?contestId=" + contestId + "&msg=" + redirectMsg);
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
