<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="报名数据分析"/>
<c:set var="activeNav" value="analytics"/>
<%@ include file="header.jsp" %>

<main class="page ax-page">

    <section class="ax-hero">
        <div class="ax-hero-copy">
            <span class="ax-eyebrow">LIVE ANALYTICS</span>
            <h1>报名数据 <em>实时看板</em></h1>
            <p>后端 Servlet 直接对报名业务库执行 GROUP BY 聚合并输出 JSON 接口，
               页面动态拉取渲染；报名、审核、支付一旦变化，30 秒内自动反映到图表。</p>
        </div>
        <div class="ax-hero-side">
            <button id="axRefresh" class="btn btn-primary btn-sm" type="button">立即刷新</button>
            <span class="ax-meta" id="axMeta">数据加载中…</span>
        </div>
    </section>

    <div id="axEmpty" class="ax-empty" hidden>
        暂无报名数据：完成一笔报名或导入数据后，图表将自动出现。
    </div>

    <section class="ax-kpis">
        <div class="ax-kpi"><div class="ax-num" id="axTotal">-</div><div class="ax-label">总报名人数</div></div>
        <div class="ax-kpi"><div class="ax-num ax-num--blue" id="axConfirmed">-</div><div class="ax-label">报名成功（已支付）</div></div>
        <div class="ax-kpi"><div class="ax-num" id="axConfirmRate">-</div><div class="ax-label">确认率（已支付 / 全部报名）</div></div>
        <div class="ax-kpi"><div class="ax-num ax-num--blue" id="axPayRate">-</div><div class="ax-label">支付转化率（已付 / 全部报名）</div></div>
    </section>

    <section class="ax-card">
        <h2>参赛项目分布</h2>
        <div class="ax-grid2">
            <div class="ax-chart"><canvas id="chartEventShare"></canvas></div>
            <div class="ax-chart"><canvas id="chartEventCapacity"></canvas></div>
        </div>
    </section>

    <section class="ax-card">
        <h2>报名状态与支付漏斗</h2>
        <div class="ax-grid2">
            <div class="ax-chart"><canvas id="chartStatus"></canvas></div>
            <div class="ax-chart"><canvas id="chartPay"></canvas></div>
        </div>
    </section>

    <section class="ax-card">
        <h2>每日报名趋势</h2>
        <div class="ax-chart ax-chart--tall"><canvas id="chartDaily"></canvas></div>
    </section>

    <section class="ax-card">
        <h2>学院报名 TOP8</h2>
        <div class="ax-chart ax-chart--tall"><canvas id="chartCollege"></canvas></div>
    </section>

    <section class="ax-card">
        <h2>学院 × 项目交叉矩阵</h2>
        <div class="ax-heat" id="axHeat"></div>
    </section>

    <section class="ax-card">
        <h2>财务汇总</h2>
        <table class="ax-revenue">
            <tbody>
            <tr><td>已收报名费</td><td id="axRevenuePaid">-</td></tr>
            <tr><td>已退款</td><td id="axRevenueRefunded">-</td></tr>
            <tr class="ax-revenue-net"><td>净收入</td><td id="axRevenueNet">-</td></tr>
            </tbody>
        </table>
    </section>

</main>

<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<script>window.AX_CTX = '${ctx}';</script>
<script src="${ctx}/assets/js/analytics.js?v=2"></script>
