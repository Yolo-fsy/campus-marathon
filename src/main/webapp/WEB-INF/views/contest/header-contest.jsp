<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${empty pageTitle ? '竞赛管理' : pageTitle} · 校园竞赛</title>
    <link rel="stylesheet" href="${ctx}/assets/css/contest.css">
</head>
<body class="contest-body">
<header class="contest-topbar">
    <a class="brand" href="${ctx}/contests">
        <span class="brand-mark">🏆</span>
        <span>校园竞赛管理</span>
    </a>
    <nav>
        <a class="${activeContest eq 'list' ? 'active' : ''}" href="${ctx}/contests">竞赛列表</a>
        <a class="${activeContest eq 'rank' ? 'active' : ''}" href="${ctx}/contests/rank">排名</a>
        <a class="${activeContest eq 'judgeLogin' ? 'active' : ''}" href="${ctx}/judge/login">评审登录</a>
    </nav>
    <div class="user-area">
        <c:if test="${not empty sessionScope.currentUser}">
            <span>${sessionScope.currentUser.displayName}（${sessionScope.currentUser.roleLabel}）</span>
        </c:if>
        <a href="${ctx}/">返回主系统</a>
    </div>
</header>
