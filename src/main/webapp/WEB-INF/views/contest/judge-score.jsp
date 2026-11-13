<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="header-contest.jsp" %>
<div class="contest-container">
    <h1 class="page-title">评审打分 - ${contest.name}</h1>

    <c:if test="${not empty param.msg}">
        <c:choose>
            <c:when test="${param.msg eq 'submitted'}"><div class="contest-flash success">评分已保存（覆盖更新）。</div></c:when>
            <c:when test="${fn:startsWith(param.msg, 'err:')}"><div class="contest-flash error">${fn:substringAfter(param.msg, 'err:')}</div></c:when>
        </c:choose>
    </c:if>

    <div class="contest-card">
        <h2>选择参赛者</h2>
        <div class="action-bar">
            <c:forEach items="${contestants}" var="ct">
                <a class="btn ${selectedContestantId == ct.id ? 'btn-primary' : 'btn-ghost'}"
                   href="${ctx}/judge/score?contestId=${contest.id}&contestantId=${ct.id}">
                    ${ct.displayName}
                </a>
            </c:forEach>
        </div>
    </div>

    <c:if test="${selectedContestantId > 0}">
        <div class="contest-card">
            <h2>打分（0-10 分） - 评审 ${judge.displayName}</h2>
            <form class="contest-form" method="post" action="${ctx}/judge/score">
                <input type="hidden" name="contestId" value="${contest.id}">
                <input type="hidden" name="contestantId" value="${selectedContestantId}">
                <table class="contest-table">
                    <thead>
                    <tr><th>评分维度</th><th>权重</th><th>分数 (0-10)</th><th>评语</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${criteria}" var="crit">
                        <tr>
                            <td>${crit.name}</td>
                            <td>${crit.weight}%</td>
                            <td>
                                <input type="number" step="0.1" min="0" max="10"
                                       class="score-input"
                                       name="score_${crit.id}"
                                       value="${existingScores[crit.id] != null ? existingScores[crit.id].score : ''}"
                                       placeholder="-">
                            </td>
                            <td>
                                <input type="text" name="comment_${crit.id}"
                                       value="${existingScores[crit.id] != null ? existingScores[crit.id].comment : ''}"
                                       placeholder="可选评语" style="width:200px;">
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <div style="margin-top:14px;">
                    <button type="submit" class="btn btn-primary">保存评分</button>
                </div>
            </form>
        </div>
    </c:if>

    <div class="contest-card">
        <h2>竞赛排名预览</h2>
        <a class="btn btn-primary" href="${ctx}/contests/rank?contestId=${contest.id}">查看实时排名 →</a>
    </div>
</div>
</body>
</html>
