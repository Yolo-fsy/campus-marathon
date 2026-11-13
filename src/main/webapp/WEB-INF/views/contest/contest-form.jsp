<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="header-contest.jsp" %>
<div class="contest-container">
    <div class="breadcrumb">
        <a href="${ctx}/contests">← 返回竞赛列表</a>
    </div>
    <h1 class="page-title">${empty contest ? '新建竞赛' : '编辑竞赛：'.concat(contest.name)}</h1>

    <div class="contest-card">
        <form class="contest-form" method="post" action="${ctx}/contests/form">
            <c:if test="${not empty contest}">
                <input type="hidden" name="id" value="${contest.id}">
            </c:if>
            <div class="form-row">
                <label>竞赛名称</label>
                <input type="text" name="name" required value="${contest.name}">
            </div>
            <div class="form-row">
                <label>竞赛描述</label>
                <textarea name="description">${contest.description}</textarea>
            </div>
            <div class="form-row">
                <button type="submit" class="btn btn-primary">${empty contest ? '创建' : '保存'}</button>
                <a class="btn btn-ghost" href="${ctx}/contests">取消</a>
            </div>
        </form>
    </div>
</div>
</body>
</html>
