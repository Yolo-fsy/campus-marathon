<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="header-contest.jsp" %>
<div class="contest-container">
    <div class="breadcrumb">
        <a href="${ctx}/contests">← 返回竞赛列表</a>
    </div>
    <h1 class="page-title">评审管理 - ${contest.name}</h1>

    <c:if test="${not empty param.msg}">
        <c:choose>
            <c:when test="${param.msg eq 'judgeAdded'}"><div class="contest-flash success">评审已添加。</div></c:when>
            <c:when test="${param.msg eq 'judgeRemoved'}"><div class="contest-flash success">评审已移除。</div></c:when>
            <c:when test="${fn:startsWith(param.msg, 'err:')}"><div class="contest-flash error">${fn:substringAfter(param.msg, 'err:')}</div></c:when>
        </c:choose>
    </c:if>

    <div class="contest-card">
        <h2>添加评审账号</h2>
        <form class="contest-form" method="post" action="${ctx}/contests/judges">
            <input type="hidden" name="contestId" value="${contest.id}">
            <input type="hidden" name="action" value="add">
            <div style="display:flex;gap:12px;flex-wrap:wrap;">
                <div class="form-row" style="flex:1;min-width:150px;">
                    <label>用户名</label>
                    <input type="text" name="username" required>
                </div>
                <div class="form-row" style="flex:1;min-width:150px;">
                    <label>展示名</label>
                    <input type="text" name="displayName">
                </div>
                <div class="form-row" style="flex:1;min-width:150px;">
                    <label>初始密码</label>
                    <input type="password" name="password" required minlength="3">
                </div>
                <div class="form-row" style="align-self:flex-end;">
                    <button class="btn btn-primary">添加</button>
                </div>
            </div>
        </form>
    </div>

    <div class="contest-card">
        <h2>当前评审（${fn:length(judges)} 人）</h2>
        <c:if test="${empty judges}">
            <div class="contest-empty">暂无评审。</div>
        </c:if>
        <c:if test="${not empty judges}">
            <table class="contest-table">
                <thead>
                <tr><th>ID</th><th>用户名</th><th>展示名</th><th>创建时间</th><th>操作</th></tr>
                </thead>
                <tbody>
                <c:forEach items="${judges}" var="j">
                    <tr>
                        <td>${j.id}</td>
                        <td>${j.username}</td>
                        <td>${j.displayName}</td>
                        <td>${j.createdAt}</td>
                        <td>
                            <form method="post" action="${ctx}/contests/judges" style="display:inline;" onsubmit="return confirm('确认移除评审 ${j.username}？');">
                                <input type="hidden" name="contestId" value="${contest.id}">
                                <input type="hidden" name="judgeId" value="${j.id}">
                                <input type="hidden" name="action" value="remove">
                                <button class="btn btn-sm btn-danger">移除</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:if>
    </div>
</div>
</body>
</html>
