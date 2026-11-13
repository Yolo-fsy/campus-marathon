package com.campus.marathon.web;

import com.campus.marathon.model.SessionUser;
import com.campus.marathon.service.AccountService;
import com.campus.marathon.service.CaptchaService;
import com.campus.marathon.service.ValidationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /register 注册页；POST /register 校验图形验证码后创建参赛者账号。
 */
public class RegisterServlet extends BaseServlet {

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser current = currentUser(request);
        if (current != null) {
            response.sendRedirect(request.getContextPath()
                    + (current.isAdmin() ? "/admin/registrations" : "/my"));
            return;
        }
        render(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String studentId = request.getParameter("studentId");
        String phone = request.getParameter("phone");
        String college = request.getParameter("college");
        String code = request.getParameter("code");
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirm");

        HttpSession session = request.getSession(true);
        if (!CaptchaService.verify(session, code)) {
            renderWithError(request, response, "code", "验证码错误或已失效，请重新输入");
            return;
        }
        try {
            accountService.register(name, studentId, phone, college, password, confirm);
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getFieldErrors());
            render(request, response);
            return;
        } catch (SQLException e) {
            throw new ServletException("注册账号失败", e);
        }
        response.sendRedirect(request.getContextPath() + "/login?msg=registered&mode=account");
    }

    private void renderWithError(HttpServletRequest request, HttpServletResponse response,
                                 String field, String message) throws ServletException, IOException {
        java.util.Map<String, String> errors = new java.util.LinkedHashMap<>();
        errors.put(field, message);
        request.setAttribute("errors", errors);
        render(request, response);
    }

    private void render(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("formName", request.getParameter("name"));
        request.setAttribute("formStudentId", request.getParameter("studentId"));
        request.setAttribute("formPhone", request.getParameter("phone"));
        request.setAttribute("formCollege", request.getParameter("college"));
        request.setAttribute("pageTitle", "注册参赛账号");
        request.setAttribute("activeNav", "login");
        forward(request, response, "register.jsp");
    }
}
