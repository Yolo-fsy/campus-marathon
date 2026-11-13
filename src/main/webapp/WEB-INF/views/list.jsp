<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="报名总览"/>
<%@ include file="header.jsp" %>

<main class="page">
    <section class="hero">
        <div class="hero-copy">
            <span class="eyebrow">CAMPUS MARATHON 2025</span>
            <h1>校园马拉松<br><em>报名管理台</em></h1>
            <p>赛事发布、在线报名、信息核验与名额控制，全部集中在同一份名单里完成。</p>
            <div class="hero-actions">
                <a class="btn btn-primary" href="${ctx}/admin/registrations/form">新建报名</a>
                <a class="btn btn-ghost" href="#roster">查看名单</a>
            </div>
        </div>
        <div class="hero-side">
            <div class="hero-event">
                <span class="event-date">2025.09.15 · 07:30</span>
                <span class="event-name">第三届校园马拉松</span>
                <span class="event-meta">东校区环湖赛道 · 半程 / 迷你 / 欢乐跑</span>
            </div>
            <div class="hero-stats">
                <a class="stat is-accent" href="${ctx}/admin/registrations"
                   title="查看全部报名记录">
                    <span class="stat-value">${stats.total}</span>
                    <span class="stat-label">累计报名</span>
                </a>
                <a class="stat" href="${ctx}/admin/registrations?status=CONFIRMED"
                   title="只看报名成功的记录">
                    <span class="stat-value">${stats.confirmed}</span>
                    <span class="stat-label">报名成功</span>
                </a>
                <a class="stat" href="${ctx}/admin/registrations?status=PENDING"
                   title="只看待审核的记录">
                    <span class="stat-value">${stats.pending}</span>
                    <span class="stat-label">待审核</span>
                </a>
                <a class="stat" href="${ctx}/admin/registrations?status=REJECTED"
                   title="只看已驳回的记录">
                    <span class="stat-value">${stats.rejected}</span>
                    <span class="stat-label">已驳回</span>
                </a>
                <a class="stat is-wide" href="${ctx}/admin/registrations?payStatus=UNPAID"
                   title="只看尚未支付报名费的记录">
                    <span class="stat-value">${stats.unpaid}</span>
                    <span class="stat-label">未支付 · 点击查看待催缴名单</span>
                </a>
            </div>
        </div>
    </section>

    <section class="capacity-grid" aria-label="各项目名额使用情况">
        <c:forEach var="category" items="${categories}">
            <c:set var="used"
                   value="${empty stats.confirmedByCategory[category.code] ? 0 : stats.confirmedByCategory[category.code]}"/>
            <div class="capacity-card">
                <div class="capacity-top">
                    <span class="capacity-name"><i></i>${category.label}</span>
                    <strong>${used}<span> / ${category.capacity}</span></strong>
                </div>
                <div class="meter" aria-hidden="true">
                    <span style="width: ${used * 100 / category.capacity}%"></span>
                </div>
                <div class="capacity-foot">
                    <span>${category.distance}</span>
                    <span>剩余 ${category.capacity - used} 个名额</span>
                </div>
            </div>
        </c:forEach>
    </section>

    <section class="panel roster-panel" id="roster">
        <div class="roster-head">
            <div class="roster-title">
                <h2>报名名单</h2>
                <span>共 ${fn:length(runners)} 条记录</span>
            </div>
            <form class="filter-form" method="get" action="${ctx}/admin/registrations">
                <label class="search-box">
                    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <circle cx="11" cy="11" r="6.5" stroke="#98a2b3" stroke-width="1.8"/>
                        <path d="M16 16l4.2 4.2" stroke="#98a2b3" stroke-width="1.8" stroke-linecap="round"/>
                    </svg>
                    <input type="search" name="keyword" value="${fn:escapeXml(keyword)}"
                           placeholder="搜索姓名 / 学号 / 手机号" aria-label="搜索参赛者">
                </label>
                <select name="category" aria-label="按参赛项目筛选">
                    <option value="">全部项目</option>
                    <c:forEach var="category" items="${categories}">
                        <option value="${category.code}" ${selectedCategory eq category.code ? 'selected' : ''}>${category.label}</option>
                    </c:forEach>
                </select>
                <select name="status" aria-label="按报名状态筛选">
                    <option value="">全部状态</option>
                    <option value="PENDING" ${selectedStatus eq 'PENDING' ? 'selected' : ''}>待审核</option>
                    <option value="CONFIRMED" ${selectedStatus eq 'CONFIRMED' ? 'selected' : ''}>报名成功</option>
                    <option value="REJECTED" ${selectedStatus eq 'REJECTED' ? 'selected' : ''}>驳回</option>
                    <option value="CANCELLED" ${selectedStatus eq 'CANCELLED' ? 'selected' : ''}>已取消</option>
                </select>
                <select name="payStatus" aria-label="按支付状态筛选">
                    <option value="">全部支付状态</option>
                    <option value="UNPAID" ${selectedPayStatus eq 'UNPAID' ? 'selected' : ''}>未支付</option>
                    <option value="PAID" ${selectedPayStatus eq 'PAID' ? 'selected' : ''}>已支付</option>
                    <option value="REFUNDED" ${selectedPayStatus eq 'REFUNDED' ? 'selected' : ''}>已退款</option>
                </select>
                <button class="btn btn-primary" type="submit">筛选</button>
                <a class="link-reset" href="${ctx}/admin/registrations">重置</a>
            </form>
        </div>

        <c:if test="${not empty flash}">
            <div class="alert alert-${flashType} roster-alert" role="status">${flash}</div>
        </c:if>
        <c:set var="returnTo"
               value="/admin/registrations${empty pageContext.request.queryString ? '' : '?'}${pageContext.request.queryString}"/>

        <c:choose>
            <c:when test="${empty runners}">
                <div class="empty-state">
                    <div class="empty-icon" aria-hidden="true">
                        <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                            <circle cx="16.4" cy="4.6" r="2.1" fill="#e2470f"/>
                            <path d="M13.2 8.4l-3.1 4 3.2 2.2 1.2 5" stroke="#e2470f" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            <path d="M10.1 12.4l-3.9.9M13.3 10.6l3.6 1.9 1.7 4.3" stroke="#e2470f" stroke-width="2" stroke-linecap="round"/>
                        </svg>
                    </div>
                    <h3>没有匹配的报名记录</h3>
                    <p>换个筛选条件试试，或者直接为参赛者录入一条新报名。</p>
                    <a class="btn btn-primary btn-sm" href="${ctx}/admin/registrations/form">新建报名</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="roster-table">
                        <thead>
                        <tr>
                            <th>参赛者</th>
                            <th>学号</th>
                            <th>联系电话</th>
                            <th>参赛项目</th>
                            <th>报名状态</th>
                            <th>报名时间</th>
                            <th>操作</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="runner" items="${runners}">
                            <tr>
                                <td>
                                    <div class="runner-cell">
                                        <span class="avatar">${runner.initial}</span>
                                        <span>
                                            <span class="runner-name">${fn:escapeXml(runner.name)}</span>
                                            <span class="runner-college">${empty runner.college ? '未填写学院' : fn:escapeXml(runner.college)}</span>
                                        </span>
                                    </div>
                                </td>
                                <td class="mono">${runner.studentId}</td>
                                <td class="mono">${runner.phone}</td>
                                <td class="event-cell">
                                    <strong>${runner.eventCategoryLabel}</strong>
                                    <span>${runner.eventCategoryDistance}</span>
                                </td>
                                <td><span class="status ${runner.statusClass}">${runner.statusLabel}</span></td>
                                <td class="mono" style="font-size:12px;color:#667085;">${runner.createdAtText}</td>
                                <td>
                                    <div class="row-actions">
                                        <a href="${ctx}/admin/registrations/detail?id=${runner.id}">详情</a>
                                        <a href="${ctx}/admin/registrations/form?id=${runner.id}">编辑</a>
                                        <c:choose>
                                            <c:when test="${runner.status eq 'PENDING'}">
                                                <form method="post" action="${ctx}/admin/registrations/review">
                                                    <input type="hidden" name="id" value="${runner.id}"><input type="hidden" name="returnTo" value="${fn:escapeXml(returnTo)}">
                                                    <input type="hidden" name="status" value="CONFIRMED">
                                                    <button type="submit" class="link-success">通过</button>
                                                </form>
                                                <form method="post" action="${ctx}/admin/registrations/review">
                                                    <input type="hidden" name="id" value="${runner.id}"><input type="hidden" name="returnTo" value="${fn:escapeXml(returnTo)}">
                                                    <input type="hidden" name="status" value="REJECTED">
                                                    <button type="submit" class="link-danger">驳回</button>
                                                </form>
                                            </c:when>
                                            <c:when test="${runner.status eq 'CONFIRMED'}">
                                                <form method="post" action="${ctx}/admin/registrations/review">
                                                    <input type="hidden" name="id" value="${runner.id}"><input type="hidden" name="returnTo" value="${fn:escapeXml(returnTo)}">
                                                    <input type="hidden" name="status" value="REJECTED">
                                                    <button type="submit" class="link-danger">驳回</button>
                                                </form>
                                            </c:when>
                                            <c:when test="${runner.status eq 'REJECTED'}">
                                                <form method="post" action="${ctx}/admin/registrations/review">
                                                    <input type="hidden" name="id" value="${runner.id}"><input type="hidden" name="returnTo" value="${fn:escapeXml(returnTo)}">
                                                    <input type="hidden" name="status" value="CONFIRMED">
                                                    <button type="submit" class="link-success">通过</button>
                                                </form>
                                            </c:when>
                                        </c:choose>
                                        <form method="post" action="${ctx}/admin/registrations/delete"
                                              onsubmit="return confirm('确认删除该报名记录？删除后不可恢复。');">
                                            <input type="hidden" name="id" value="${runner.id}"><input type="hidden" name="returnTo" value="${fn:escapeXml(returnTo)}">
                                            <button type="submit" class="link-danger">删除</button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</main>

<script>
(function () {
    var KEY = 'registrationListScrollY';
    // 审核/删除后回到同一页，恢复操作前的滚动位置
    window.addEventListener('load', function () {
        var saved = sessionStorage.getItem(KEY);
        if (saved !== null) {
            sessionStorage.removeItem(KEY);
            window.scrollTo(0, parseInt(saved, 10) || 0);
        }
    });
    document.addEventListener('submit', function (event) {
        if (event.target.closest && event.target.closest('.row-actions')) {
            sessionStorage.setItem(KEY, String(window.scrollY));
        }
    });
})();
</script>

<%@ include file="footer.jsp" %>
