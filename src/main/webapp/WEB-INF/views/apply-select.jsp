<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="选择参赛项目"/>
<c:set var="activeNav" value="apply"/>
<%@ include file="header.jsp" %>

<style>
    .pick-hero {
        position: relative;
        overflow: hidden;
        background:
            radial-gradient(900px 320px at 12% -10%, rgba(255, 90, 31, .38), transparent 62%),
            radial-gradient(760px 300px at 96% 8%, rgba(64, 128, 255, .30), transparent 60%),
            linear-gradient(150deg, #0a1120 0%, #122744 52%, #0b1424 100%);
        color: #fff;
        padding: 54px 0 128px;
    }
    .pick-hero::after {
        content: "";
        position: absolute;
        inset: 0;
        background-image:
            repeating-linear-gradient(90deg, rgba(255,255,255,.05) 0 1px, transparent 1px 96px),
            repeating-linear-gradient(0deg, rgba(255,255,255,.05) 0 1px, transparent 1px 96px);
        mask-image: radial-gradient(circle at 50% 30%, #000 10%, transparent 72%);
        pointer-events: none;
    }
    .pick-hero-inner { position: relative; max-width: 1120px; margin: 0 auto; padding: 0 24px; }
    .pick-eyebrow {
        display: inline-flex; align-items: center; gap: 8px;
        padding: 6px 14px; border-radius: 999px;
        background: rgba(255,255,255,.10); border: 1px solid rgba(255,255,255,.18);
        font-size: 12px; letter-spacing: .14em; text-transform: uppercase; color: #ffd9c7;
    }
    .pick-eyebrow i { width: 6px; height: 6px; border-radius: 50%; background: #ff7a45; box-shadow: 0 0 12px #ff7a45; }
    .pick-title {
        margin: 18px 0 12px; font-size: 44px; line-height: 1.16; letter-spacing: -.02em;
        font-weight: 800;
    }
    .pick-title em {
        font-style: normal;
        background: linear-gradient(96deg, #ffb08a 0%, #ff6a33 42%, #ffd7c2 100%);
        -webkit-background-clip: text; background-clip: text; color: transparent;
    }
    .pick-sub { max-width: 560px; color: rgba(233, 240, 255, .74); font-size: 15px; }
    .pick-stats { display: flex; flex-wrap: wrap; gap: 34px; margin-top: 26px; }
    .pick-stat strong { display: block; font-size: 26px; font-weight: 800; letter-spacing: -.01em; }
    .pick-stat span { font-size: 12px; color: rgba(233,240,255,.62); letter-spacing: .04em; }

    .pick-tabs {
        position: relative;
        max-width: 1120px; margin: -64px auto 0; padding: 0 24px;
    }
    .pick-tabbar {
        display: flex; gap: 8px; padding: 8px;
        background: rgba(255,255,255,.72);
        backdrop-filter: blur(18px) saturate(160%);
        border: 1px solid rgba(16,24,40,.08);
        border-radius: 18px;
        box-shadow: 0 22px 50px rgba(11, 20, 36, .18);
        overflow-x: auto;
    }
    .pick-tab {
        position: relative;
        flex: 1 0 auto;
        display: flex; align-items: center; gap: 10px;
        padding: 13px 18px; border-radius: 12px;
        color: var(--ink-soft); font-weight: 600; font-size: 14px;
        white-space: nowrap; transition: background .2s ease, color .2s ease, box-shadow .2s ease;
    }
    .pick-tab small { display: block; font-weight: 500; font-size: 11px; color: var(--muted); letter-spacing: .02em; }
    .pick-tab:hover { background: rgba(255,90,31,.08); color: var(--brand-dark); }
    .pick-tab.is-active {
        background: linear-gradient(135deg, #ff6a33, #e2470f);
        color: #fff; box-shadow: 0 10px 24px rgba(226, 71, 15, .34);
    }
    .pick-tab.is-active small { color: rgba(255,255,255,.78); }
    .pick-tab-dot { width: 9px; height: 9px; border-radius: 50%; background: currentColor; opacity: .45; }
    .pick-tab.is-active .pick-tab-dot { opacity: 1; background: #fff; box-shadow: 0 0 0 4px rgba(255,255,255,.22); }

    .pick-stage { max-width: 1120px; margin: 22px auto 72px; padding: 0 24px; }
    .pick-card {
        display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
        background: var(--surface);
        border: 1px solid rgba(16,24,40,.07);
        border-radius: 26px;
        overflow: hidden;
        box-shadow: 0 30px 80px rgba(11, 20, 36, .16);
    }
    .pick-figure { position: relative; min-height: 340px; background: #0b1424; }
    .pick-figure img { width: 100%; height: 100%; object-fit: cover; display: block; }
    .pick-figure::after {
        content: ""; position: absolute; inset: 0;
        background: linear-gradient(180deg, rgba(11,20,36,0) 46%, rgba(11,20,36,.72) 100%);
    }
    .pick-figure-tag {
        position: absolute; left: 18px; bottom: 16px; z-index: 2;
        display: inline-flex; align-items: center; gap: 8px;
        padding: 7px 13px; border-radius: 999px;
        background: rgba(255,255,255,.16); border: 1px solid rgba(255,255,255,.28);
        backdrop-filter: blur(8px); color: #fff; font-size: 12px; letter-spacing: .06em;
    }
    .pick-body { padding: 40px 42px; display: flex; flex-direction: column; }
    .pick-kicker { font-size: 12px; letter-spacing: .16em; text-transform: uppercase; color: var(--brand); font-weight: 700; }
    .pick-name { margin: 10px 0 4px; font-size: 30px; font-weight: 800; letter-spacing: -.02em; }
    .pick-distance { display: flex; align-items: baseline; gap: 10px; color: var(--ink-soft); }
    .pick-distance strong { font-size: 34px; font-weight: 800; color: var(--ink); letter-spacing: -.02em; }
    .pick-distance span { font-size: 14px; color: var(--muted); }
    .pick-features { list-style: none; margin: 22px 0 0; padding: 0; display: grid; gap: 11px; }
    .pick-features li { display: flex; align-items: flex-start; gap: 10px; font-size: 14px; color: var(--ink-soft); }
    .pick-features li b {
        flex: 0 0 auto; width: 20px; height: 20px; margin-top: 2px; border-radius: 50%;
        background: var(--brand-soft); color: var(--brand-dark);
        font-size: 11px; font-weight: 700; display: grid; place-items: center;
    }
    .pick-capacity {
        margin-top: 24px; padding: 14px 16px; border-radius: 14px;
        background: linear-gradient(180deg, #f7f9fc, #eef2f8);
        border: 1px solid rgba(16,24,40,.06);
        display: flex; align-items: center; justify-content: space-between; gap: 14px;
    }
    .pick-capacity span { font-size: 13px; color: var(--muted); }
    .pick-capacity strong { font-size: 15px; color: var(--ink); }
    .pick-actions { margin-top: auto; padding-top: 28px; display: flex; align-items: center; gap: 14px; }
    .pick-actions .btn { padding: 14px 26px; font-size: 15px; border-radius: 13px; }
    .pick-actions .back { font-size: 13px; color: var(--muted); }
    .pick-foot { max-width: 1120px; margin: 0 auto 80px; padding: 0 24px; display: grid; gap: 14px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .pick-tip {
        padding: 18px 20px; border-radius: 16px; background: var(--surface);
        border: 1px solid rgba(16,24,40,.07); box-shadow: var(--shadow-sm);
    }
    .pick-tip b { display: block; font-size: 13px; color: var(--ink); margin-bottom: 4px; }
    .pick-tip p { margin: 0; font-size: 12.5px; color: var(--muted); }

    @media (max-width: 900px) {
        .pick-title { font-size: 32px; }
        .pick-card { grid-template-columns: 1fr; }
        .pick-figure { min-height: 220px; }
        .pick-body { padding: 26px 22px; }
        .pick-tab small { display: none; }
        .pick-foot { grid-template-columns: 1fr; }
        .pick-hero { padding: 38px 0 110px; }
    }
</style>

<section class="pick-hero">
    <div class="pick-hero-inner">
        <span class="pick-eyebrow"><i></i>2025 校园马拉松 · 报名通道</span>
        <h1 class="pick-title">选一条赛道，<em>跑出你的主场</em></h1>
        <p class="pick-sub">三个项目同步开放，名额实时校验。先选定参赛项目，再填写参赛人信息，提交后完成 30 元报名费支付即可锁定名额。</p>
        <div class="pick-stats">
            <div class="pick-stat"><strong>3</strong><span>参赛项目</span></div>
            <div class="pick-stat"><strong>1600</strong><span>总名额</span></div>
            <div class="pick-stat"><strong>¥30</strong><span>报名费 / 人</span></div>
        </div>
    </div>
</section>

<div class="pick-tabs">
    <div class="pick-tabbar" role="tablist" aria-label="参赛项目">
        <c:forEach var="category" items="${categories}">
            <a class="pick-tab ${category.code eq activeCode ? 'is-active' : ''}"
               role="tab" aria-selected="${category.code eq activeCode}"
               href="${ctx}/apply?category=${category.code}">
                <span class="pick-tab-dot" aria-hidden="true"></span>
                <span>
                    ${category.label}
                    <small>${category.distance}</small>
                </span>
            </a>
        </c:forEach>
    </div>
</div>

<div class="pick-stage">
    <c:forEach var="category" items="${categories}">
        <c:if test="${category.code eq activeCode}">
            <article class="pick-card">
                <div class="pick-figure">
                    <c:choose>
                        <c:when test="${category.code eq 'HALF'}">
                            <img alt="半程马拉松" src="${ctx}/assets/img/pick-half.jpg">
                        </c:when>
                        <c:when test="${category.code eq 'MINI'}">
                            <img alt="迷你马拉松" src="${ctx}/assets/img/pick-mini.jpg">
                        </c:when>
                        <c:otherwise>
                            <img alt="欢乐跑" src="${ctx}/assets/img/pick-fun.jpg">
                        </c:otherwise>
                    </c:choose>
                    <span class="pick-figure-tag">赛道实景 · ${category.distance}</span>
                </div>

                <div class="pick-body">
                    <span class="pick-kicker">Step 01 · 选择项目</span>
                    <h2 class="pick-name">${category.label}</h2>
                    <div class="pick-distance">
                        <strong>${category.distance}</strong>
                        <span>全程距离 · 限额 ${category.capacity} 人</span>
                    </div>

                    <ul class="pick-features">
                        <c:choose>
                            <c:when test="${category.code eq 'HALF'}">
                                <li><b>1</b>环湖主赛道，全程封路，配速员分 1:45 / 2:00 / 2:15 三组领跑</li>
                                <li><b>2</b>沿途 5 个补给站，完赛获定制奖牌与官方计时芯片</li>
                                <li><b>3</b>适合有半马完赛经验或训练基础的同学</li>
                            </c:when>
                            <c:when test="${category.code eq 'MINI'}">
                                <li><b>1</b>穿越校园核心区，途经樱花大道与地标广场</li>
                                <li><b>2</b>3 公里处设趣味打卡点，完赛可在奖牌墙留名</li>
                                <li><b>3</b>首次参赛友好，平均完赛时长 35 分钟</li>
                            </c:when>
                            <c:otherwise>
                                <li><b>1</b>趣味打卡 + 社团方阵，边跑边玩不设配速要求</li>
                                <li><b>2</b>支持好友组队、亲子同行，完赛人人有纪念徽章</li>
                                <li><b>3</b>零门槛，步行与慢跑均可完成</li>
                            </c:otherwise>
                        </c:choose>
                    </ul>

                    <div class="pick-capacity">
                        <span>名额状态</span>
                        <strong>剩余 ${category.capacity} 席 · 提交后实时校验</strong>
                    </div>

                    <div class="pick-actions">
                        <a class="btn btn-primary" href="${ctx}/apply/form?category=${category.code}">
                            下一步 · 填写参赛人信息
                        </a>
                        <a class="back" href="${ctx}/login">已有报名？登录查看</a>
                    </div>
                </div>
            </article>
        </c:if>
    </c:forEach>
</div>

<div class="pick-foot">
    <div class="pick-tip"><b>名额实时校验</b><p>提交与支付到账双重校验，满额自动退款，绝不超卖。</p></div>
    <div class="pick-tip"><b>先选后填</b><p>选定项目后表单将锁定该赛道，如需更改可一键返回重选。</p></div>
    <div class="pick-tip"><b>支持随时取消</b><p>取消报名立即释放名额，已缴的 30 元报名费原路退回。</p></div>
</div>

<%@ include file="footer.jsp" %>
