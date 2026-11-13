package com.campus.marathon.web;

import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.model.SessionUser;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 角色访问控制：/admin/** 仅管理员，/my/** 仅参赛者。
 */
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // 无需初始化参数
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getServletPath();

        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("currentUser");

        if (path.startsWith("/admin/")) {
            if (user == null) {
                deny(request, response, null, "authRequired");
                return;
            }
            if (!user.isAdmin()) {
                deny(request, response, user, "forbidden");
                return;
            }
        } else if (path.startsWith("/my")) {
            if (user == null) {
                deny(request, response, null, "authRequired");
                return;
            }
            if (!user.isRunner()) {
                deny(request, response, user, "forbidden");
                return;
            }
        } else if (path.equals("/ai") || path.startsWith("/ai/")) {
            // 知识库只要求登录，不区分角色
            if (user == null) {
                deny(request, response, null, "authRequired");
                return;
            }
        } else if (path.equals("/contests") || path.startsWith("/contests/")) {
            if (user == null) {
                deny(request, response, null, "authRequired");
                return;
            }
            // 列表与排名对所有登录用户开放；
            // 赛事配置、评委、评分维度、参赛者管理以及状态流转仅管理员可操作
            boolean management = path.startsWith("/contests/form")
                    || path.startsWith("/contests/judges")
                    || path.startsWith("/contests/criteria")
                    || path.startsWith("/contests/contestants")
                    || (path.equals("/contests") && "POST".equalsIgnoreCase(request.getMethod()));
            if (management && !user.isAdmin()) {
                deny(request, response, user, "forbidden");
                return;
            }
        } else if (path.equals("/analytics") || path.startsWith("/analytics/")) {
            // 数据看板展示全量报名统计，仅管理员可见
            if (user == null) {
                deny(request, response, null, "authRequired");
                return;
            }
            if (!user.isAdmin()) {
                deny(request, response, user, "forbidden");
                return;
            }
        } else if (path.startsWith("/apply")) {
            // 报名需先注册账号并登录，未登录直接引导到注册页
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login?msg=authRequired&role=runner");
                return;
            }
            if (!user.isRunner()) {
                deny(request, response, user, "forbidden");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 未登录跳登录页；已登录越权则回到自己的主页并带提示，避免提示被首页重定向吞掉。
     */
    private void deny(HttpServletRequest request, HttpServletResponse response, SessionUser user, String message)
            throws IOException {
        String home = "/login";
        if (user != null) {
            home = user.isAdmin() ? "/admin/registrations" : "/my";
        }
        response.sendRedirect(request.getContextPath() + home + "?msg=" + message);
    }

    @Override
    public void destroy() {
        // 无需释放资源
    }
}
