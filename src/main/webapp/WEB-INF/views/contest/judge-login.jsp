<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>评审登录 · 校园竞赛</title>
    <link rel="stylesheet" href="${ctx}/assets/css/contest.css">
</head>
<body class="contest-body">
<div class="judge-login-wrap">
    <div class="judge-login-box">
        <h1>🏆 评审登录</h1>

        <c:if test="${not empty param.msg}">
            <c:choose>
                <c:when test="${param.msg eq 'invalid'}">
                    <div class="contest-flash error">用户名或密码错误。</div>
                </c:when>
                <c:when test="${param.msg eq 'loggedOut'}">
                    <div class="contest-flash success">已退出登录。</div>
                </c:when>
                <c:when test="${param.msg eq 'needLogin'}">
                    <div class="contest-flash warning">请先登录评审账号。</div>
                </c:when>
            </c:choose>
        </c:if>

        <form class="contest-form" method="post" action="${ctx}/judge/login">
            <div class="form-row">
                <label>用户名</label>
                <input type="text" name="username" required autofocus>
            </div>
            <div class="form-row">
                <label>密码</label>
                <input type="password" name="password" required>
            </div>
            <div class="form-row">
                <button class="btn btn-primary" style="width:100%;padding:10px;">登录</button>
            </div>
        </form>

        <p style="margin-top:18px;font-size:12px;color:#64748b;text-align:center;">
            示例账号：judge1 / judge1（由种子数据自动生成）
        </p>

        <div style="text-align:center;margin-top:12px;">
            <a href="${ctx}/contests" style="font-size:13px;color:#2563eb;text-decoration:none;">← 返回竞赛管理</a>
        </div>
    </div>
</div>
</body>
</html>
