<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="header.jsp" %>

<main class="page">
    <c:if test="${not empty flash}">
        <div class="alert alert-${flashType}" role="status">${flash}</div>
    </c:if>

    <section class="panel" style="max-width:520px;margin:60px auto;text-align:center;padding:56px 32px;">
        <div style="font-size:52px;line-height:1;margin-bottom:16px;" aria-hidden="true">🏃</div>
        <h1 style="margin-bottom:8px;">还没有报名，快来报名吧</h1>
        <p class="muted" style="margin-bottom:28px;">填写报名信息并完成 30 元报名费支付，即可锁定参赛名额。</p>
        <a class="btn btn-primary" href="${ctx}/apply" style="display:inline-block;padding:12px 42px;">立即报名</a>
    </section>
</main>

<%@ include file="footer.jsp" %>
