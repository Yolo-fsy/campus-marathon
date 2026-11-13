package com.campus.marathon.web;

import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.service.PaymentService;
import com.campus.marathon.service.RegistrationService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 控制器基类：共享 Service 实例与通用跳转方法。
 */
abstract class BaseServlet extends HttpServlet {

    protected final RegistrationService service = new RegistrationService();

    protected final PaymentService paymentService = new PaymentService();

    protected long parseId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return -1L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    protected void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/" + view).forward(request, response);
    }

    protected void redirectToList(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        String returnTo = request.getParameter("returnTo");
        String target = isSafeReturnPath(returnTo)
                ? appendMessage(returnTo.trim(), message)
                : "/admin/registrations?msg=" + message;
        response.sendRedirect(request.getContextPath() + target);
    }

    /**
     * 仅接受站内相对路径，避免借助 returnTo 参数跳转到外部站点。
     */
    private static boolean isSafeReturnPath(String value) {
        if (value == null) {
            return false;
        }
        String path = value.trim();
        return path.startsWith("/")
                && !path.startsWith("//")
                && !path.contains("://")
                && !path.contains("\r")
                && !path.contains("\n");
    }

    /**
     * 保留来源页的筛选参数，用本次操作结果替换掉旧的 msg 参数。
     */
    private static String appendMessage(String path, String message) {
        int queryIndex = path.indexOf('?');
        String base = queryIndex < 0 ? path : path.substring(0, queryIndex);
        StringBuilder query = new StringBuilder();
        if (queryIndex >= 0) {
            for (String pair : path.substring(queryIndex + 1).split("&")) {
                if (pair.isEmpty() || pair.equals("msg") || pair.startsWith("msg=")) {
                    continue;
                }
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(pair);
            }
        }
        if (query.length() > 0) {
            query.append('&');
        }
        query.append("msg=").append(message);
        return base + "?" + query;
    }

    /**
     * 将表单参数绑定为实体，状态校验由 Service 统一处理。
     */
    protected MarathonRunner bindRunner(HttpServletRequest request) {
        MarathonRunner runner = new MarathonRunner();
        runner.setName(request.getParameter("name"));
        runner.setStudentId(request.getParameter("studentId"));
        runner.setPhone(request.getParameter("phone"));
        runner.setCollege(request.getParameter("college"));
        runner.setEventCategory(request.getParameter("eventCategory"));
        runner.setStatus(request.getParameter("status"));
        runner.setRemark(request.getParameter("remark"));
        return runner;
    }

    protected SessionUser currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (SessionUser) session.getAttribute("currentUser");
    }
}
