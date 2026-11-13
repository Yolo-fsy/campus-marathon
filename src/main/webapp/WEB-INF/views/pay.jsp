<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="header.jsp" %>

<main class="page">
    <c:if test="${not empty error}">
        <div class="alert alert-warning" role="status">${error}</div>
    </c:if>

    <section class="panel" style="max-width:560px;margin:40px auto;text-align:center;">
        <h1 style="margin-bottom:8px;">报名费支付</h1>
        <p class="muted" style="margin-bottom:24px;">请完成报名费支付，支付成功后报名立即生效并锁定名额。</p>

        <div class="info-grid" style="text-align:left;margin-bottom:24px;">
            <div class="info-item">
                <span>参赛者</span>
                <strong>${fn:escapeXml(runner.name)}</strong>
            </div>
            <div class="info-item">
                <span>学号</span>
                <strong class="mono">${runner.studentId}</strong>
            </div>
            <div class="info-item">
                <span>参赛项目</span>
                <strong>${runner.eventCategoryLabel}<em>${runner.eventCategoryDistance}</em></strong>
            </div>
            <div class="info-item">
                <span>支付金额</span>
                <strong style="color:var(--primary,#ff5a1f);font-size:22px;">¥${amount}</strong>
            </div>
        </div>

        <form method="post" action="${ctx}/pay">
            <button type="submit" class="btn btn-primary" style="width:100%;">
                使用支付宝支付 ¥${amount}
            </button>
        </form>
        <a class="btn btn-outline" style="width:100%;padding:12px;margin-top:10px;display:block;"
           href="${ctx}${backUrl}">返回报名信息</a>
        <c:choose>
            <c:when test="${mock}">
                <p class="hint" style="margin-top:12px;">当前为演示模式（未配置沙箱密钥），点击支付将模拟支付成功。</p>
            </c:when>
            <c:otherwise>
                <p class="hint" style="margin-top:12px;">将跳转至支付宝沙箱收银台，请使用沙箱买家账号完成支付。</p>
            </c:otherwise>
        </c:choose>
        <p class="hint" style="margin-top:8px;">
            支付未完成前报名为「待支付」状态，不会占用名额；取消报名将自动退回已支付的报名费。
        </p>
    </section>
</main>

<%@ include file="footer.jsp" %>
