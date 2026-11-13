package com.campus.marathon.web;

import com.campus.marathon.model.Contest;
import com.campus.marathon.service.ContestService;
import com.campus.marathon.util.ContestSchema;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * GET /contests 竞赛列表（全部 / 按状态）。
 * POST /contests 切换竞赛状态（启动评审 / 结束评审 / 回退草稿）。
 */
public class ContestListServlet extends BaseServlet {

    private final ContestService contestService = new ContestService();

    @Override
    public void init() throws ServletException {
        ContestSchema.init();
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String status = trim(request.getParameter("status"));
        String msg = trim(request.getParameter("msg"));

        try {
            List<Contest> contests;
            if (status.isEmpty()) {
                contests = contestService.listAll();
            } else {
                contests = contestService.listByStatus(status);
            }
            request.setAttribute("contests", contests);
            request.setAttribute("status", status);
        } catch (SQLException e) {
            throw new ServletException("查询竞赛列表失败", e);
        }

        request.setAttribute("msg", msg);
        request.setAttribute("pageTitle", "竞赛管理");
        forward(request, response, "contest/contest-list.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long contestId = parseId(request.getParameter("contestId"));
        String action = trim(request.getParameter("action"));
        String redirectMsg;
        try {
            switch (action) {
                case "startJudging":
                    contestService.startJudging(contestId);
                    redirectMsg = "judgingStarted";
                    break;
                case "finish":
                    contestService.finishContest(contestId);
                    redirectMsg = "finished";
                    break;
                case "revertDraft":
                    contestService.revertToDraft(contestId);
                    redirectMsg = "reverted";
                    break;
                default:
                    redirectMsg = "unknownAction";
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
