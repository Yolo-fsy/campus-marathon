<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="出错了"/>
<%@ include file="header.jsp" %>

<main class="error-page">
    <div class="error-code">${requestScope['javax.servlet.error.status_code']}</div>
    <h1>页面暂时不可用</h1>
    <p>
        <c:choose>
            <c:when test="${requestScope['javax.servlet.error.status_code'] eq 404}">
                你访问的页面不存在，可能已被移动或删除。
            </c:when>
            <c:otherwise>
                系统处理请求时出现异常，请稍后重试或联系赛事管理员。
            </c:otherwise>
        </c:choose>
    </p>
    <a class="btn btn-primary" href="${ctx}/">返回首页</a>
</main>

<%@ include file="footer.jsp" %>
