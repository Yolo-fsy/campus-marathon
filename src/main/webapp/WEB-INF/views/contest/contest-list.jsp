<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="header-contest.jsp" %>
<div class="contest-container">
    <h1 class="page-title">竞赛管理</h1>

    <c:if test="${not empty param.msg}">
        <c:choose>
            <c:when test="${param.msg eq 'created'}">
                <div class="contest-flash success">竞赛已创建。</div>
            </c:when>
            <c:when test="${param.msg eq 'updated'}">
                <div class="contest-flash success">竞赛信息已更新。</div>
            </c:when>
            <c:when test="${param.msg eq 'judgingStarted'}">
                <div class="contest-flash success">已启动评审，维度锁定。</div>
            </c:when>
            <c:when test="${param.msg eq 'finished'}">
                <div class="contest-flash success">竞赛已结束。</div>
            </c:when>
            <c:when test="${param.msg eq 'reverted'}">
                <div class="contest-flash warning">已回退到草稿，可补充维度/评审。</div>
            </c:when>
            <c:when test="${fn:startsWith(param.msg, 'err:')}">
                <div class="contest-flash error">${fn:substringAfter(param.msg, 'err:')}</div>
            </c:when>
        </c:choose>
    </c:if>

    <div class="contest-card">
        <div class="action-bar">
            <c:if test="${sessionScope.currentUser.admin}">
                <a class="btn btn-primary" href="${ctx}/contests/form">+ 新建竞赛</a>
            </c:if>
            <a class="btn btn-ghost ${empty status ? 'active' : ''}" href="${ctx}/contests">全部</a>
            <a class="btn btn-ghost ${status eq 'DRAFT' ? 'active' : ''}" href="${ctx}/contests?status=DRAFT">草稿</a>
            <a class="btn btn-ghost ${status eq 'JUDGING' ? 'active' : ''}" href="${ctx}/contests?status=JUDGING">评审中</a>
            <a class="btn btn-ghost ${status eq 'FINISHED' ? 'active' : ''}" href="${ctx}/contests?status=FINISHED">已结束</a>
        </div>

        <c:choose>
            <c:when test="${empty contests}">
                <div class="contest-empty">还没有任何竞赛<c:if test="${sessionScope.currentUser.admin}">，点击上方「+ 新建竞赛」开始</c:if>。</div>
            </c:when>
            <c:otherwise>
                <table class="contest-table">
                    <thead>
                    <tr><th>ID</th><th>竞赛名称</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${contests}" var="c">
                        <tr>
                            <td>${c.id}</td>
                            <td>
                                <strong>${c.name}</strong>
                                <c:if test="${not empty c.description}">
                                    <div style="font-size:12px;color:#64748b;margin-top:2px;">${c.description}</div>
                                </c:if>
                            </td>
                            <td><span class="status-tag status-${c.status == 'DRAFT' ? 'draft' : (c.status == 'JUDGING' ? 'judging' : 'finished')}">${c.statusLabel}</span></td>
                            <td>${c.createdAt}</td>
                            <td>
                                <c:if test="${sessionScope.currentUser.admin}">
                                    <a class="btn btn-sm btn-ghost" href="${ctx}/contests/form?id=${c.id}">编辑</a>
                                    <a class="btn btn-sm btn-ghost" href="${ctx}/contests/contestants?contestId=${c.id}">参赛者</a>
                                    <a class="btn btn-sm btn-ghost" href="${ctx}/contests/judges?contestId=${c.id}">评审</a>
                                    <a class="btn btn-sm btn-ghost" href="${ctx}/contests/criteria?contestId=${c.id}">维度</a>
                                </c:if>
                                <a class="btn btn-sm btn-primary" href="${ctx}/contests/rank?contestId=${c.id}">排名</a>

                                <c:if test="${sessionScope.currentUser.admin}">
                                    <form method="post" action="${ctx}/contests" style="display:inline;">
                                        <input type="hidden" name="contestId" value="${c.id}">
                                        <c:if test="${c.status eq 'DRAFT'}">
                                            <button class="btn btn-sm btn-success" name="action" value="startJudging">启动评审</button>
                                        </c:if>
                                        <c:if test="${c.status eq 'JUDGING'}">
                                            <button class="btn btn-sm btn-warning" name="action" value="revertDraft">回退草稿</button>
                                            <button class="btn btn-sm btn-danger" name="action" value="finish">结束评审</button>
                                        </c:if>
                                    </form>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>
