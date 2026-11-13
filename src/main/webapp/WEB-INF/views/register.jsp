<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="注册参赛账号"/>
<c:set var="activeNav" value="login"/>
<%@ include file="header.jsp" %>

<style>
    .reg-page {
        min-height: calc(100vh - 64px);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 48px 20px 72px;
        background:
            radial-gradient(900px 320px at 12% -10%, rgba(255, 90, 31, .18), transparent 62%),
            radial-gradient(760px 300px at 96% 8%, rgba(64, 128, 255, .14), transparent 60%),
            #f5f7fb;
    }
    .reg-card {
        width: 100%;
        max-width: 460px;
        padding: 34px 34px 30px;
        background: #fff;
        border: 1px solid rgba(16, 24, 40, .08);
        border-radius: 22px;
        box-shadow: 0 28px 70px rgba(11, 20, 36, .13);
    }
    .reg-card h1 {
        margin: 10px 0 6px;
        font-size: 27px;
        letter-spacing: -.02em;
    }
    .reg-steps {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 18px 0 6px;
        font-size: 12px;
        color: var(--muted);
    }
    .reg-step {
        display: inline-flex;
        align-items: center;
        gap: 6px;
    }
    .reg-step b {
        width: 20px; height: 20px; border-radius: 50%;
        display: grid; place-items: center;
        background: var(--brand-soft); color: var(--brand-dark);
        font-size: 11px;
    }
    .reg-step.is-active { color: var(--ink); font-weight: 600; }
    .reg-sep { flex: 1; height: 1px; background: rgba(16, 24, 40, .08); }
</style>

<main class="reg-page">
    <section class="reg-card">
        <span class="eyebrow">SIGN UP · CAMPUS MARATHON 2025</span>
        <h1>注册参赛账号</h1>
        <p class="auth-sub">填写信息并通过图形验证码校验，创建账号后登录即可报名</p>

        <div class="reg-steps">
            <span class="reg-step is-active"><b>1</b>填写信息</span>
            <span class="reg-sep"></span>
            <span class="reg-step"><b>2</b>图形验证</span>
            <span class="reg-sep"></span>
            <span class="reg-step"><b>3</b>登录报名</span>
        </div>

        <form class="auth-form" method="post" action="${ctx}/register" novalidate>
            <div class="field ${not empty errors.name ? 'has-error' : ''}">
                <label for="regName">姓名 <span class="req">*</span></label>
                <input id="regName" name="name" type="text" maxlength="20"
                       value="${fn:escapeXml(formName)}" placeholder="与学籍一致的姓名" required>
                <p class="error-text">${errors.name}</p>
            </div>

            <div class="field ${not empty errors.studentId ? 'has-error' : ''}">
                <label for="regStudentId">学号 <span class="req">*</span></label>
                <input id="regStudentId" name="studentId" type="text" maxlength="12"
                       value="${fn:escapeXml(formStudentId)}" placeholder="8-12 位学号"
                       inputmode="numeric" required>
                <p class="error-text">${errors.studentId}</p>
            </div>

            <div class="field ${not empty errors.phone ? 'has-error' : ''}">
                <label for="regPhone">手机号 <span class="req">*</span></label>
                <input id="regPhone" name="phone" type="tel" maxlength="11"
                       value="${fn:escapeXml(formPhone)}" placeholder="11 位手机号"
                       inputmode="numeric" required>
                <p class="hint">用于赛事通知与紧急联系</p>
                <p class="error-text">${errors.phone}</p>
            </div>

            <div class="field ${not empty errors.college ? 'has-error' : ''}">
                <label for="regCollege">所属学院</label>
                <input id="regCollege" name="college" type="text" maxlength="40"
                       value="${fn:escapeXml(formCollege)}" placeholder="如：计算机科学与技术学院">
                <p class="hint">选填，方便赛后按学院统计参赛数据</p>
                <p class="error-text">${errors.college}</p>
            </div>

            <div class="field ${not empty errors.code ? 'has-error' : ''}">
                <label for="regCode">图形验证码 <span class="req">*</span></label>
                <div class="captcha-row">
                    <input id="regCode" name="code" type="text" maxlength="4"
                           placeholder="4 位数字" inputmode="numeric" autocomplete="off" required>
                    <img id="captchaImg" class="captcha-img" src="${ctx}/register/captcha"
                         alt="图形验证码" title="点击刷新验证码" width="120" height="44">
                    <button class="btn btn-outline btn-sm" type="button" id="captchaRefresh">换一张</button>
                </div>
                <p class="field-hint">看不清？点击图片或「换一张」重新生成，验证码 5 分钟内有效。</p>
                <p class="error-text">${errors.code}</p>
            </div>

            <div class="field ${not empty errors.password ? 'has-error' : ''}">
                <label for="regPassword">设置密码 <span class="req">*</span></label>
                <input id="regPassword" name="password" type="password" maxlength="32"
                       placeholder="6-32 位，登录时使用" autocomplete="new-password" required>
                <p class="error-text">${errors.password}</p>
            </div>

            <div class="field ${not empty errors.confirm ? 'has-error' : ''}">
                <label for="regConfirm">确认密码 <span class="req">*</span></label>
                <input id="regConfirm" name="confirm" type="password" maxlength="32"
                       placeholder="再次输入密码" autocomplete="new-password" required>
                <p class="error-text">${errors.confirm}</p>
            </div>

            <button class="btn btn-primary btn-block" type="submit">注册并验证</button>
        </form>

        <p class="auth-alt">已有账号？<a href="${ctx}/login?role=runner&mode=account">直接登录 →</a></p>

        <div class="auth-foot">
            注册仅用于赛事报名与通知，密码经加盐哈希存储，不会明文保存。
        </div>
    </section>
</main>

<script>
    (function () {
        var img = document.getElementById('captchaImg');
        var refresh = document.getElementById('captchaRefresh');
        var input = document.getElementById('regCode');
        if (!img) { return; }
        function reload() {
            img.src = '${ctx}/register/captcha?t=' + new Date().getTime();
            if (input) { input.value = ''; }
        }
        img.addEventListener('click', reload);
        if (refresh) { refresh.addEventListener('click', reload); }
    })();
</script>

<%@ include file="footer.jsp" %>
