package com.campus.marathon.web;

import com.campus.marathon.model.Judge;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.service.JudgeService;
import com.campus.marathon.util.ContestSchema;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /judge/login 评审登录页。
 * POST /judge/login 登录验证，成功后写入 SessionUser(role=JUDGE)。
 * GET /judge/logout 退出评审登录。
 */
public class JudgeLoginServlet extends BaseServlet {

    private final JudgeService judgeService = new JudgeService();

    @Override
    public void init() throws ServletException {
        ContestSchema.init();
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = trim(request.getParameter("action"));
        if ("logout".equals(action)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute("currentUser");
            }
            response.sendRedirect(request.getContextPath() + "/judge/login?msg=loggedOut");
            return;
        }
        request.setAttribute("pageTitle", "评审登录");
        forward(request, response, "contest/judge-login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = trim(request.getParameter("username"));
        String password = trim(request.getParameter("password"));
        String redirectMsg;
        try {
            Judge judge = judgeService.authenticate(username, password);
            if (judge == null) {
                redirectMsg = "invalid";
            } else {
                SessionUser user = SessionUser.judge(judge.getUsername(),
                        judge.getDisplayName(), judge.getContestId());
                request.getSession().setAttribute("currentUser", user);
                redirectMsg = "ok";
            }
        } catch (SQLException e) {
            redirectMsg = "error";
        }
        if ("ok".equals(redirectMsg)) {
            Long contestId = request.getSession(false) != null
                    ? ((SessionUser) request.getSession().getAttribute("currentUser")).getContestId()
                    : null;
            if (contestId != null) {
                response.sendRedirect(request.getContextPath()
                        + "/judge/score?contestId=" + contestId);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/contests?msg=judgeLoginOk");
            return;
        }
        response.sendRedirect(request.getContextPath()
                + "/judge/login?msg=" + redirectMsg);
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
