<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="header.jsp" %>

<main class="page">
    <nav class="breadcrumb" aria-label="面包屑">
        <c:choose>
            <c:when test="${viewer eq 'admin'}">
                <a href="${ctx}/admin/registrations">报名总览</a>
                <span>/</span>
                <span>报名详情</span>
            </c:when>
            <c:otherwise>
                <a href="${ctx}/my">我的报名</a>
                <span>/</span>
                <span>报名详情</span>
            </c:otherwise>
        </c:choose>
    </nav>

    <c:if test="${viewer eq 'runner'}">
        <div class="my-banner">
            <span class="dot" aria-hidden="true"></span>
            当前仅展示本人报名信息，其他参赛者的数据不可见
        </div>
    </c:if>

    <c:if test="${not empty flash}">
        <div class="alert alert-${flashType}" role="status">${flash}</div>
    </c:if>

    <section class="detail-hero">
        <div class="detail-person">
            <span class="avatar avatar-lg">${runner.initial}</span>
            <div>
                <div class="detail-name-row">
                    <h1>${fn:escapeXml(runner.name)}</h1>
                    <span class="status ${runner.statusClass}">${runner.statusLabel}</span>
                </div>
                <p class="detail-sub">
                    ${empty runner.college ? '未填写学院' : fn:escapeXml(runner.college)}
                    · 学号 ${runner.studentId}
                </p>
            </div>
        </div>
        <div class="detail-actions">
            <c:choose>
                <c:when test="${viewer eq 'admin'}">
                    <a class="btn btn-primary btn-sm"
                       href="${ctx}/admin/registrations/form?id=${runner.id}">编辑报名</a>
                    <form method="post" action="${ctx}/admin/registrations/delete"
                          onsubmit="return confirm('确认删除该报名记录？删除后不可恢复。');">
                        <input type="hidden" name="id" value="${runner.id}">
                        <button class="btn btn-outline-danger btn-sm" type="submit">删除记录</button>
                    </form>
                    <a class="btn btn-outline btn-sm" href="${ctx}/admin/registrations">返回名单</a>
                </c:when>
                <c:otherwise>
                    <c:if test="${runner.payStatus eq 'UNPAID'}">
                        <a class="btn btn-success btn-sm" href="${ctx}/pay">去支付 ¥30</a>
                    </c:if>
                    <a class="btn btn-primary btn-sm" href="${ctx}/my/edit">修改我的报名</a>
                    <c:if test="${runner.status eq 'PENDING' or runner.status eq 'CONFIRMED'}">
                        <form method="post" action="${ctx}/my/cancel"
                              onsubmit="return confirm('${runner.payStatus eq 'PAID'
                                  ? '确认取消本次报名？30 元报名费将自动原路退回，名额随之释放。'
                                  : '确认取消本次报名？取消后名额将被释放，可随时重新报名。'}');">
                            <button class="btn btn-outline-danger btn-sm" type="submit">取消报名</button>
                        </form>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </div>
    </section>

    <div class="detail-layout">
        <section class="panel detail-info">
            <h2>报名信息</h2>
            <div class="info-grid">
                <div class="info-item">
                    <span>学号</span>
                    <strong class="mono">${runner.studentId}</strong>
                </div>
                <div class="info-item">
                    <span>联系电话</span>
                    <strong class="mono">${runner.phone}</strong>
                </div>
                <div class="info-item">
                    <span>参赛项目</span>
                    <strong>${runner.eventCategoryLabel}<em>${runner.eventCategoryDistance}</em></strong>
                </div>
                <div class="info-item">
                    <span>报名状态</span>
                    <strong><span class="status ${runner.statusClass}">${runner.statusLabel}</span></strong>
                </div>
                <div class="info-item">
                    <span>支付状态</span>
                    <strong>${runner.payStatusLabel}
                        <c:if test="${not empty runner.outTradeNo}">
                            <em class="mono" style="font-size:12px;color:#667085;">${runner.outTradeNo}</em>
                        </c:if>
                    </strong>
                </div>
                <div class="info-item">
                    <span>报名时间</span>
                    <strong class="mono">${runner.createdAtText}</strong>
                </div>
                <div class="info-item">
                    <span>最近更新</span>
                    <strong class="mono">${runner.updatedAtText}</strong>
                </div>
            </div>
            <div class="remark-box">
                <h3>备注说明</h3>
                <p>${empty runner.remark ? '未填写备注信息。' : fn:escapeXml(runner.remark)}</p>
            </div>
        </section>

        <aside class="bib-card" aria-label="参赛凭证">
            <div class="bib-head">CAMPUS MARATHON 2025</div>
            <div class="bib-main">
                <span class="bib-label">BIB NUMBER</span>
                <strong class="bib-number">${runner.bibNumber}</strong>
                <span class="bib-name">${fn:escapeXml(runner.name)}</span>
                <span class="bib-event">${runner.eventCategoryLabel} · ${runner.eventCategoryDistance}</span>
            </div>
            <div class="bib-foot">
                <span>东校区环湖赛道</span>
                <span>2025.09.15</span>
            </div>
        </aside>
    </div>

    <c:if test="${viewer eq 'admin'}">
        <section class="panel audit-panel">
            <div>
                <h2>审核操作</h2>
                <p>通过审核会占用该项目名额；驳回后参赛者可修改信息并重新提交审核。</p>
            </div>
            <div class="audit-actions">
                <form method="post" action="${ctx}/admin/registrations/review">
                    <input type="hidden" name="id" value="${runner.id}">
                    <input type="hidden" name="returnTo" value="/admin/registrations/detail?id=${runner.id}">
                    <input type="hidden" name="status" value="CONFIRMED">
                    <button type="submit" class="btn btn-success btn-sm"
                            ${runner.status eq 'CONFIRMED' ? 'disabled' : ''}>通过审核</button>
                </form>
                <form method="post" action="${ctx}/admin/registrations/review">
                    <input type="hidden" name="id" value="${runner.id}">
                    <input type="hidden" name="returnTo" value="/admin/registrations/detail?id=${runner.id}">
                    <input type="hidden" name="status" value="REJECTED">
                    <button type="submit" class="btn btn-outline-danger btn-sm"
                            ${runner.status eq 'REJECTED' ? 'disabled' : ''}>驳回报名</button>
                </form>
            </div>
        </section>
    </c:if>

    <c:if test="${viewer eq 'runner'}">
        <c:choose>
            <c:when test="${runner.status eq 'PENDING' and runner.payStatus eq 'UNPAID'}">
                <section class="status-tip is-pending">
                    <h2>待支付报名费</h2>
                    <p>报名已提交，请尽快完成 30 元报名费支付。支付成功后报名立即生效并锁定名额。</p>
                </section>
            </c:when>
            <c:when test="${runner.status eq 'PENDING'}">
                <section class="status-tip is-pending">
                    <h2>审核进行中</h2>
                    <p>管理员会在 24 小时内完成信息核验，请保持手机畅通。审核期间你仍可以修改报名信息。</p>
                </section>
            </c:when>
            <c:when test="${runner.status eq 'CONFIRMED'}">
                <section class="status-tip is-success">
                    <h2>报名成功，名额已锁定</h2>
                    <p>请于 11 月 10 日至 14 日到体育中心领取参赛包，比赛当日提前 40 分钟到场检录。</p>
                </section>
            </c:when>
            <c:when test="${runner.status eq 'CANCELLED'}">
                <section class="status-tip is-muted">
                    <h2>报名已取消</h2>
                    <p>名额已释放，报名记录仍保留用于赛后统计。如需再次参赛，请点击「重新报名」补充信息并提交，状态会重新进入待审核队列。</p>
                </section>
            </c:when>
            <c:otherwise>
                <section class="status-tip is-danger">
                    <h2>报名未通过审核</h2>
                    <p>请核对姓名、学号与手机号后点击「修改我的报名」重新提交，保存后将自动回到待审核队列。</p>
                    <c:if test="${runner.payStatus eq 'REFUNDED'}">
                        <p>已缴纳的 30 元报名费已原路退回，通常 1-3 个工作日到账；可点击「修改我的报名」重新提交并再次缴费。</p>
                    </c:if>
                </section>
            </c:otherwise>
        </c:choose>
    </c:if>
</main>

<%@ include file="footer.jsp" %>
