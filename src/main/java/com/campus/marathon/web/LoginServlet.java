package com.campus.marathon.web;

import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.model.RunnerAccount;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.service.AccountService;
import com.campus.marathon.service.AuthService;
import com.campus.marathon.service.LoginResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /login ：登录页；POST /login ：管理员或参赛者登录。
 */
public class LoginServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser current = currentUser(request);
        if (current != null) {
            redirectHome(request, response, current);
            return;
        }
        request.setAttribute("activeTab", "admin".equals(request.getParameter("role")) ? "admin" : "runner");
        request.setAttribute("activeMode", "phone".equals(request.getParameter("mode")) ? "phone" : "account");
        request.setAttribute("pageTitle", "登录");
        exposeFlash(request);
        forward(request, response, "login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String role = trim(request.getParameter("role"));
        if ("admin".equals(role)) {
            handleAdminLogin(request, response);
        } else {
            handleRunnerLogin(request, response);
        }
    }

    private void handleAdminLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = trim(request.getParameter("username"));
        String password = trim(request.getParameter("password"));
        if (authService.verifyAdmin(username, password)) {
            startSession(request, SessionUser.admin(username));
            response.sendRedirect(request.getContextPath() + "/admin/registrations");
            return;
        }
        request.setAttribute("error", "管理员账号或密码错误，请重新输入。");
        request.setAttribute("activeTab", "admin");
        request.setAttribute("loginUsername", username);
        request.setAttribute("pageTitle", "登录");
        forward(request, response, "login.jsp");
    }

    private void handleRunnerLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("account".equals(trim(request.getParameter("mode")))) {
            handleAccountLogin(request, response);
            return;
        }
        String studentId = trim(request.getParameter("studentId"));
        String phone = trim(request.getParameter("phone"));
        try {
            LoginResult result = authService.loginRunner(studentId, phone);
            if (result.isSuccess()) {
                MarathonRunner runner = result.getRunner();
                startSession(request, SessionUser.runner(runner));
                response.sendRedirect(request.getContextPath() + "/my");
                return;
            }
            request.setAttribute("error", result.getErrorMessage());
        } catch (SQLException e) {
            throw new ServletException("参赛者登录失败", e);
        }
        request.setAttribute("activeTab", "runner");
        request.setAttribute("activeMode", "phone");
        request.setAttribute("loginStudentId", studentId);
        request.setAttribute("loginPhone", phone);
        request.setAttribute("pageTitle", "登录");
        forward(request, response, "login.jsp");
    }

    /**
     * 注册账号登录：学号 + 密码，密码以 PBKDF2 加盐存储。
     */
    private void handleAccountLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String studentId = trim(request.getParameter("studentId"));
        String password = trim(request.getParameter("password"));
        try {
            RunnerAccount account = accountService.findByStudentId(studentId);
            if (account == null) {
                request.setAttribute("error", "该学号尚未注册账号，请先前往注册。");
            } else if (!accountService.matches(account, password)) {
                request.setAttribute("error", "学号或密码不正确，请重新输入。");
            } else {
                startSession(request, SessionUser.runner(account.getStudentId(), account.getName()));
                response.sendRedirect(request.getContextPath() + "/my");
                return;
            }
        } catch (SQLException e) {
            throw new ServletException("账号登录失败", e);
        }
        request.setAttribute("activeTab", "runner");
        request.setAttribute("activeMode", "account");
        request.setAttribute("loginStudentId", studentId);
        request.setAttribute("pageTitle", "登录");
        forward(request, response, "login.jsp");
    }

    private void startSession(HttpServletRequest request, SessionUser user) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        request.getSession(true).setAttribute("currentUser", user);
    }

    private void redirectHome(HttpServletRequest request, HttpServletResponse response, SessionUser user)
            throws IOException {
        String target = user.isAdmin() ? "/admin/registrations" : "/my";
        response.sendRedirect(request.getContextPath() + target);
    }

    private void exposeFlash(HttpServletRequest request) {
        String code = request.getParameter("msg");
        if (code == null) {
            return;
        }
        switch (code) {
            case "authRequired":
                request.setAttribute("flash", "请先登录后再访问该页面。");
                request.setAttribute("flashType", "warning");
                break;
            case "forbidden":
                request.setAttribute("flash", "当前账号无权访问该页面。");
                request.setAttribute("flashType", "warning");
                break;
            case "loggedOut":
                request.setAttribute("flash", "已安全退出登录。");
                request.setAttribute("flashType", "success");
                break;
            case "applied":
                request.setAttribute("flash", "报名提交成功，完成 30 元报名费支付后报名生效。请使用学号与手机号登录。");
                request.setAttribute("flashType", "success");
                request.setAttribute("activeTab", "runner");
                break;
            case "registered":
                request.setAttribute("flash", "账号注册成功，请使用学号与密码登录后进入报名。");
                request.setAttribute("flashType", "success");
                request.setAttribute("activeTab", "runner");
                request.setAttribute("activeMode", "account");
                break;
            case "recordMissing":
                request.setAttribute("flash", "报名记录不存在或已被删除，请重新报名。");
                request.setAttribute("flashType", "warning");
                break;
            case "payFull":
                request.setAttribute("flash", "该项目名额已满，30 元报名费已自动退回，请改报其他项目。");
                request.setAttribute("flashType", "warning");
                break;
            case "payInvalid":
                request.setAttribute("flash", "支付结果校验未通过，如已扣款请联系赛事管理员核对。");
                request.setAttribute("flashType", "warning");
                break;
            default:
                break;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
