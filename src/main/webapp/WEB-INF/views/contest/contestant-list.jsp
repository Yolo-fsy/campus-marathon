<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="header-contest.jsp" %>
<div class="contest-container">
    <div class="breadcrumb">
        <a href="${ctx}/contests">← 返回竞赛列表</a>
    </div>
    <h1 class="page-title">参赛者管理 - ${contest.name}</h1>

    <c:if test="${not empty param.msg}">
        <c:choose>
            <c:when test="${param.msg eq 'contestantAdded'}"><div class="contest-flash success">参赛者已添加。</div></c:when>
            <c:when test="${param.msg eq 'contestantRemoved'}"><div class="contest-flash success">参赛者已移除。</div></c:when>
            <c:when test="${fn:startsWith(param.msg, 'err:')}"><div class="contest-flash error">${fn:substringAfter(param.msg, 'err:')}</div></c:when>
        </c:choose>
    </c:if>

    <div class="contest-card">
        <h2>添加参赛者</h2>
        <form class="contest-form" method="post" action="${ctx}/contests/contestants">
            <input type="hidden" name="contestId" value="${contest.id}">
            <input type="hidden" name="action" value="add">
            <div style="display:flex;gap:12px;flex-wrap:wrap;">
                <div class="form-row" style="flex:1;min-width:120px;">
                    <label>姓名</label>
                    <input type="text" name="name">
                </div>
                <div class="form-row" style="flex:1;min-width:120px;">
                    <label>学号</label>
                    <input type="text" name="studentId">
                </div>
                <div class="form-row" style="flex:1;min-width:120px;">
                    <label>团队名称（可选）</label>
                    <input type="text" name="teamName">
                </div>
                <div class="form-row" style="align-self:flex-end;">
                    <button class="btn btn-primary">添加</button>
                </div>
            </div>
        </form>
    </div>

    <div class="contest-card">
        <h2>参赛者列表（${fn:length(contestants)} 人）</h2>
        <c:if test="${empty contestants}">
            <div class="contest-empty">暂无参赛者。</div>
        </c:if>
        <c:if test="${not empty contestants}">
            <table class="contest-table">
                <thead>
                <tr><th>ID</th><th>姓名</th><th>学号</th><th>团队</th><th>提交时间</th><th>操作</th></tr>
                </thead>
                <tbody>
                <c:forEach items="${contestants}" var="ct">
                    <tr>
                        <td>${ct.id}</td>
                        <td>${ct.name}</td>
                        <td>${ct.studentId}</td>
                        <td>${ct.teamName}</td>
                        <td>${ct.submittedAt}</td>
                        <td>
                            <form method="post" action="${ctx}/contests/contestants" style="display:inline;" onsubmit="return confirm('移除参赛者 ${ct.displayName}？关联评分也会一起删除');">
                                <input type="hidden" name="contestId" value="${contest.id}">
                                <input type="hidden" name="contestantId" value="${ct.id}">
                                <input type="hidden" name="action" value="delete">
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
