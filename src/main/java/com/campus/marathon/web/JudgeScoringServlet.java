package com.campus.marathon.web;

import com.campus.marathon.model.Contest;
import com.campus.marathon.model.Contestant;
import com.campus.marathon.model.Criterion;
import com.campus.marathon.model.Judge;
import com.campus.marathon.model.Score;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.service.ContestService;
import com.campus.marathon.service.JudgeService;
import com.campus.marathon.service.ScoringService;
import com.campus.marathon.util.ContestSchema;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /judge/score?contestId=&contestantId= 评分页（显示维度 + 已打分）。
 * POST /judge/score 提交评分（ScoringService.submitScore 覆盖更新）。
 */
public class JudgeScoringServlet extends BaseServlet {

    private final ContestService contestService = new ContestService();
    private final JudgeService judgeService = new JudgeService();
    private final ScoringService scoringService = new ScoringService();

    @Override
    public void init() throws ServletException {
        ContestSchema.init();
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 校验评审登录
        SessionUser user = currentUser(request);
        if (user == null || !user.isJudge()) {
            response.sendRedirect(request.getContextPath() + "/judge/login?msg=needLogin");
            return;
        }

        long contestId = parseId(request.getParameter("contestId"));
        long contestantId = parseId(request.getParameter("contestantId"));

        // 评审只能给自己所属的竞赛打分
        if (user.getContestId() != null && !user.getContestId().equals(contestId)) {
            response.sendRedirect(request.getContextPath() + "/judge/login?msg=forbidden");
            return;
        }

        try {
            Contest contest = contestService.getContest(contestId);
            List<Contestant> contestants = contestService.listContestants(contestId);
            List<Criterion> criteria = contestService.listCriteria(contestId);

            // 找到当前 judgeId
            Judge judge = judgeService.findByUsernameAndContest(user.getUsername(), contestId);

            // 如果选中了某参赛者，回显他对各维度已打的分
            Map<Long, Score> existing = new LinkedHashMap<>();
            if (judge != null && contestantId > 0) {
                List<Score> scores = scoringService.listScoresByJudgeAndContestant(judge.getId(), contestantId);
                for (Score s : scores) {
                    existing.put(s.getCriterionId(), s);
                }
            }

            request.setAttribute("contest", contest);
            request.setAttribute("contestants", contestants);
            request.setAttribute("criteria", criteria);
            request.setAttribute("selectedContestantId", contestantId);
            request.setAttribute("existingScores", existing);
            request.setAttribute("judge", judge);
        } catch (SQLException e) {
            throw new ServletException("加载评分页失败", e);
        }
        request.setAttribute("pageTitle", "评审打分");
        forward(request, response, "contest/judge-score.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 校验评审登录
        SessionUser user = currentUser(request);
        if (user == null || !user.isJudge()) {
            response.sendRedirect(request.getContextPath() + "/judge/login?msg=needLogin");
            return;
        }

        long contestId = parseId(request.getParameter("contestId"));
        long contestantId = parseId(request.getParameter("contestantId"));

        String redirectMsg;
        try {
            Judge judge = judgeService.findByUsernameAndContest(user.getUsername(), contestId);
            if (judge == null) {
                redirectMsg = "forbidden";
            } else {
                // 收集所有 criterionId + score + comment 参数
                java.util.Enumeration<String> paramNames = request.getParameterNames();
                while (paramNames.hasMoreElements()) {
                    String pn = paramNames.nextElement();
                    if (!pn.startsWith("score_")) {
                        continue;
                    }
                    long criterionId = parseId(pn.substring("score_".length()));
                    BigDecimal score = parseBigDecimal(request.getParameter(pn));
                    String comment = trim(request.getParameter("comment_" + criterionId));
                    if (score != null) {
                        scoringService.submitScore(judge.getId(), contestantId, criterionId, score, comment);
                    }
                }
                redirectMsg = "submitted";
            }
        } catch (Exception e) {
            redirectMsg = "err:" + e.getMessage();
        }
        response.sendRedirect(request.getContextPath()
                + "/judge/score?contestId=" + contestId
                + "&contestantId=" + contestantId + "&msg=" + redirectMsg);
    }

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            BigDecimal bd = new BigDecimal(s.trim());
            if (bd.compareTo(BigDecimal.ZERO) < 0 || bd.compareTo(BigDecimal.TEN) > 0) {
                return null;
            }
            return bd;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
