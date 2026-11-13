<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="header-contest.jsp" %>
<div class="contest-container">
    <div class="breadcrumb">
        <a href="${ctx}/contests">← 返回竞赛列表</a>
    </div>
    <h1 class="page-title">评分维度管理 - ${contest.name}</h1>

    <c:if test="${not empty param.msg}">
        <c:choose>
            <c:when test="${param.msg eq 'critAdded'}"><div class="contest-flash success">维度已添加。</div></c:when>
            <c:when test="${param.msg eq 'critUpdated'}"><div class="contest-flash success">维度已更新。</div></c:when>
            <c:when test="${param.msg eq 'critDeleted'}"><div class="contest-flash success">维度已删除。</div></c:when>
            <c:when test="${fn:startsWith(param.msg, 'err:')}"><div class="contest-flash error">${fn:substringAfter(param.msg, 'err:')}</div></c:when>
        </c:choose>
    </c:if>

    <div class="contest-card">
        <div class="weight-sum ${weightSum == 100 ? 'ok' : 'bad'}">
            当前权重和：<strong>${weightSum}</strong> / 100
            <c:if test="${weightSum != 100}">（权重之和必须为 100 才能启动评审）</c:if>
            <c:if test="${contest.status eq 'JUDGING'}">（评审进行中，维度已锁定，不可修改）</c:if>
        </div>

        <h2>添加维度</h2>
        <form class="contest-form" method="post" action="${ctx}/contests/criteria">
            <input type="hidden" name="contestId" value="${contest.id}">
            <input type="hidden" name="action" value="add">
            <div style="display:flex;gap:12px;flex-wrap:wrap;">
                <div class="form-row" style="flex:2;min-width:150px;">
                    <label>维度名称</label>
                    <input type="text" name="name" required placeholder="如：技术难度、创新性、完成度">
                </div>
                <div class="form-row" style="flex:1;min-width:100px;">
                    <label>权重 (0-100)</label>
                    <input type="number" class="weight-input" name="weight" min="0" max="100" value="30" required>
                </div>
                <div class="form-row" style="flex:2;min-width:150px;">
                    <label>说明（可选）</label>
                    <input type="text" name="description" placeholder="维度评估要点">
                </div>
                <div class="form-row" style="align-self:flex-end;">
                    <button class="btn btn-primary" ${contest.status != 'DRAFT' ? 'disabled style="opacity:.5;cursor:not-allowed;"' : ''}>添加</button>
                </div>
            </div>
        </form>
    </div>

    <div class="contest-card">
        <h2>当前维度</h2>
        <c:if test="${empty criteria}">
            <div class="contest-empty">暂无评分维度。</div>
        </c:if>
        <c:if test="${not empty criteria}">
            <table class="contest-table">
                <thead>
                <tr><th>ID</th><th>名称</th><th>权重</th><th>说明</th><th>操作</th></tr>
                </thead>
                <tbody>
                <c:forEach items="${criteria}" var="crit">
                    <tr>
                        <td>${crit.id}</td>
                        <td>${crit.name}</td>
                        <td><strong>${crit.weight}%</strong></td>
                        <td>${crit.description}</td>
                        <td>
                            <c:if test="${contest.status eq 'DRAFT'}">
                                <form method="post" action="${ctx}/contests/criteria" style="display:inline;">
                                    <input type="hidden" name="contestId" value="${contest.id}">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="criterionId" value="${crit.id}">
                                    <input type="text" name="name" value="${crit.name}" required style="width:120px;">
                                    <input type="number" class="weight-input" name="weight" min="0" max="100" value="${crit.weight}">
                                    <input type="text" name="description" value="${crit.description}" style="width:160px;">
                                    <button class="btn btn-sm btn-primary">保存</button>
                                </form>
                                <form method="post" action="${ctx}/contests/criteria" style="display:inline;" onsubmit="return confirm('删除维度 ${crit.name}？关联评分也会一起删除');">
                                    <input type="hidden" name="contestId" value="${contest.id}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="criterionId" value="${crit.id}">
                                    <button class="btn btn-sm btn-danger">删除</button>
                                </form>
                            </c:if>
                            <c:if test="${contest.status != 'DRAFT'}">
                                <span style="color:#64748b;font-size:12px;">已锁定</span>
                            </c:if>
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
