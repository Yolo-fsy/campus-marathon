package com.campus.marathon.web;

import com.campus.marathon.model.Contest;
import com.campus.marathon.model.Criterion;
import com.campus.marathon.service.ContestService;
import com.campus.marathon.util.ContestSchema;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * GET /contests/criteria?contestId= 评分维度管理。
 * POST /contests/criteria 创建 / 更新权重 / 删除。
 */
public class ContestCriterionServlet extends BaseServlet {

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
            List<Criterion> criteria = contestService.listCriteria(contestId);
            request.setAttribute("contest", contest);
            request.setAttribute("criteria", criteria);
            request.setAttribute("weightSum", criteria.stream().mapToInt(Criterion::getWeight).sum());
        } catch (SQLException e) {
            throw new ServletException("查询评分维度失败", e);
        }
        request.setAttribute("pageTitle", "评分维度管理");
        forward(request, response, "contest/contest-criteria.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long contestId = parseId(request.getParameter("contestId"));
        String action = trim(request.getParameter("action"));
        String redirectMsg;
        try {
            switch (action) {
                case "add": {
                    String name = trim(request.getParameter("name"));
                    int weight = parseInt(request.getParameter("weight"), 0);
                    String desc = trim(request.getParameter("description"));
                    contestService.addCriterion(contestId, name, weight, desc);
                    redirectMsg = "critAdded";
                    break;
                }
                case "update": {
                    long criterionId = parseId(request.getParameter("criterionId"));
                    String name = trim(request.getParameter("name"));
                    int weight = parseInt(request.getParameter("weight"), 0);
                    String desc = trim(request.getParameter("description"));
                    contestService.updateCriterion(contestId, criterionId, name, weight, desc);
                    redirectMsg = "critUpdated";
                    break;
                }
                case "delete": {
                    long criterionId = parseId(request.getParameter("criterionId"));
                    contestService.deleteCriterion(contestId, criterionId);
                    redirectMsg = "critDeleted";
                    break;
                }
                default:
                    redirectMsg = "unknownAction";
            }
        } catch (Exception e) {
            redirectMsg = "err:" + e.getMessage();
        }
        response.sendRedirect(request.getContextPath()
                + "/contests/criteria?contestId=" + contestId + "&msg=" + redirectMsg);
    }

    private int parseInt(String s, int defaultValue) {
        if (s == null || s.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
