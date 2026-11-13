<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="header-contest.jsp" %>
<div class="contest-container">
    <div class="breadcrumb">
        <a href="${ctx}/contests">← 返回竞赛列表</a>
    </div>
    <h1 class="page-title">竞赛排名 - ${contest.name}
        <span class="status-tag status-${contest.status eq 'DRAFT' ? 'draft' : (contest.status eq 'JUDGING' ? 'judging' : 'finished')}">${contest.statusLabel}</span>
    </h1>

    <div class="contest-card">
        <c:if test="${empty ranks}">
            <div class="contest-empty">暂无参赛者，无法排名。</div>
        </c:if>
        <c:if test="${not empty ranks}">
            <table class="contest-table">
                <thead>
                <tr><th>名次</th><th>参赛者 / 团队</th><th>加权总分</th><th>平均分</th><th>有效评审数</th></tr>
                </thead>
                <tbody>
                <c:forEach items="${ranks}" var="r" varStatus="loop">
                    <tr class="rank-row ${loop.index > 0 && ranks[loop.index].rank eq ranks[loop.index-1].rank ? '' : ''}">
                        <td>
                            <span class="rank-badge ${r.rank == 1 ? 'rank-1' : (r.rank == 2 ? 'rank-2' : (r.rank == 3 ? 'rank-3' : 'rank-default'))}">
                                ${r.rank}
                            </span>
                            <c:if test="${loop.index < fn:length(ranks) - 1 && r.totalScore == ranks[loop.index + 1].totalScore}">
                                <span style="margin-left:6px;color:#b45309;font-size:12px;">并列</span>
                            </c:if>
                        </td>
                        <td>
                            <strong>${r.contestant.displayName}</strong>
                            <c:if test="${not empty r.contestant.studentId}">
                                <div style="font-size:12px;color:#64748b;">${r.contestant.studentId}</div>
                            </c:if>
                        </td>
                        <td class="rank-total">${r.totalScore}</td>
                        <td>${r.averagePerJudge}</td>
                        <td>${r.judgeCount}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:if>
    </div>
</div>
</body>
</html>
