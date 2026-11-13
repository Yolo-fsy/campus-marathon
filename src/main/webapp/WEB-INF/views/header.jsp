<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${empty pageTitle ? '校园马拉松' : pageTitle} · 校园马拉松报名系统</title>
    <link rel="stylesheet" href="${ctx}/assets/css/style.css">
    <link rel="stylesheet" href="${ctx}/assets/css/support.css?v=2">
    <c:if test="${activeNav eq 'ai'}">
        <link rel="stylesheet" href="${ctx}/assets/css/ai.css?v=1">
    </c:if>
    <c:if test="${activeNav eq 'analytics'}">
        <link rel="stylesheet" href="${ctx}/assets/css/analytics.css?v=1">
    </c:if>
    <script src="${ctx}/assets/js/support.js?v=2" charset="UTF-8" defer></script>
</head>
<body>
<header class="topbar">
    <div class="topbar-inner">
        <a class="brand" href="${ctx}/">
            <span class="brand-mark" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                    <circle cx="16.4" cy="4.6" r="2.1" fill="#fff"/>
                    <path d="M13.2 8.4l-3.1 4 3.2 2.2 1.2 5" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M10.1 12.4l-3.9.9M13.3 10.6l3.6 1.9 1.7 4.3" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
                </svg>
            </span>
            <span class="brand-text">
                <strong>CAMPUS MARATHON</strong>
                <small>校园马拉松报名系统</small>
            </span>
        </a>

        <nav class="main-nav">
            <c:choose>
                <c:when test="${sessionScope.currentUser.admin}">
                    <a class="nav-link ${activeNav eq 'list' ? 'is-active' : ''}"
                       href="${ctx}/admin/registrations">报名总览</a>
                    <a class="nav-link ${activeNav eq 'analytics' ? 'is-active' : ''}"
                       href="${ctx}/analytics">数据分析</a>
                    <a class="nav-link ${activeNav eq 'create' ? 'is-active' : ''}"
                       href="${ctx}/admin/registrations/form">新建报名</a>
                    <a class="nav-link ${activeNav eq 'ai' ? 'is-active' : ''}"
                       href="${ctx}/ai">AI 知识库</a>
                </c:when>
                <c:when test="${sessionScope.currentUser.runner}">
                    <a class="nav-link ${activeNav eq 'my' ? 'is-active' : ''}"
                       href="${ctx}/my">我的报名</a>
                    <a class="nav-link ${activeNav eq 'ai' ? 'is-active' : ''}"
                       href="${ctx}/ai">AI 知识库</a>
                </c:when>
                <c:otherwise>
                    <a class="nav-link ${activeNav eq 'apply' ? 'is-active' : ''}"
                       href="${ctx}/apply">在线报名</a>
                </c:otherwise>
            </c:choose>
        </nav>

        <div class="topbar-actions">
            <c:choose>
                <c:when test="${not empty sessionScope.currentUser}">
                    <div class="user-chip">
                        <span class="user-avatar">${sessionScope.currentUser.initial}</span>
                        <span class="user-meta">
                            <strong>${fn:escapeXml(sessionScope.currentUser.displayName)}</strong>
                            <small>${sessionScope.currentUser.roleLabel}</small>
                        </span>
                    </div>
                    <a class="icon-btn" href="${ctx}/logout">退出</a>
                </c:when>
                <c:otherwise>
                    <a class="btn btn-outline btn-sm" href="${ctx}/login">登录</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>
