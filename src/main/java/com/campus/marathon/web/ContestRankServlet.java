package com.campus.marathon.web;

import com.campus.marathon.model.Contest;
import com.campus.marathon.model.ContestRank;
import com.campus.marathon.service.ContestService;
import com.campus.marathon.service.ScoringService;
import com.campus.marathon.util.ContestSchema;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * GET /contests/rank?contestId= 实时排名页。
 */
public class ContestRankServlet extends BaseServlet {

    private final ContestService contestService = new ContestService();
    private final ScoringService scoringService = new ScoringService();

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
            List<ContestRank> ranks = scoringService.computeContestRank(contestId);
            request.setAttribute("contest", contest);
            request.setAttribute("ranks", ranks);
        } catch (SQLException e) {
            throw new ServletException("计算竞赛排名失败", e);
        }
        request.setAttribute("pageTitle", "竞赛排名");
        forward(request, response, "contest/contest-rank.jsp");
    }
}
