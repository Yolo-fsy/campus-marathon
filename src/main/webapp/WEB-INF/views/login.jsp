<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="登录"/>
<%@ include file="header.jsp" %>

<main class="auth-page">
    <section class="auth-intro">
        <div>
            <span class="eyebrow">CAMPUS MARATHON 2025</span>
            <h1>跑过校园的<br>每一条<em>赛道</em></h1>
            <p>第三届校园马拉松面向全校开放报名，半程、迷你与欢乐跑三个项目，名额有限，先到先得。</p>
            <div class="auth-facts">
                <div class="auth-fact">
                    <strong>3</strong>
                    <span>参赛项目</span>
                </div>
                <div class="auth-fact">
                    <strong>1600</strong>
                    <span>开放名额</span>
                </div>
                <div class="auth-fact">
                    <strong>24h</strong>
                    <span>审核反馈</span>
                </div>
            </div>
        </div>
        <div class="auth-intro-foot">
            <i aria-hidden="true"></i>
            2025.09.15 · 东校区环湖赛道 · 07:30 起跑
        </div>
    </section>

    <section class="auth-card">
        <div class="auth-switch">
            <a class="${activeTab eq 'runner' ? 'is-active' : ''}" href="${ctx}/login?role=runner">参赛者</a>
            <a class="${activeTab eq 'admin' ? 'is-active' : ''}" href="${ctx}/login?role=admin">管理员</a>
        </div>

        <c:if test="${not empty flash}">
            <div class="alert alert-${flashType}" role="status">${flash}</div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-error" role="alert">${error}</div>
        </c:if>

        <c:choose>
            <c:when test="${activeTab eq 'admin'}">
                <h2>管理员登录</h2>
                <p class="auth-sub">审核报名、维护名单与实时名额控制</p>
                <form class="auth-form" method="post" action="${ctx}/login">
                    <input type="hidden" name="role" value="admin">
                    <div class="field">
                        <label for="username">管理员账号</label>
                        <input id="username" name="username" type="text"
                               value="${fn:escapeXml(loginUsername)}" placeholder="请输入管理员账号" autocomplete="username" required>
                    </div>
                    <div class="field">
                        <label for="password">登录密码</label>
                        <input id="password" name="password" type="password"
                               placeholder="请输入登录密码" autocomplete="current-password" required>
                    </div>
                    <button class="btn btn-primary btn-block" type="submit">进入管理后台</button>
                </form>
                <p class="auth-alt">管理员凭据由 <span class="mono">auth.properties</span> 配置</p>
            </c:when>
            <c:otherwise>
                <h2>参赛者登录</h2>
                <div class="auth-subswitch">
                    <a class="${activeMode eq 'account' ? 'is-active' : ''}"
                       href="${ctx}/login?role=runner&mode=account">学号 + 密码</a>
                    <a class="${activeMode eq 'phone' ? 'is-active' : ''}"
                       href="${ctx}/login?role=runner&mode=phone">学号 + 手机号</a>
                </div>

                <c:choose>
                    <c:when test="${activeMode eq 'phone'}">
                        <p class="auth-sub">使用报名时填写的学号与手机号核验身份（未注册账号的老用户通道）</p>
                        <form class="auth-form" method="post" action="${ctx}/login">
                            <input type="hidden" name="role" value="runner">
                            <input type="hidden" name="mode" value="phone">
                            <div class="field">
                                <label for="loginStudentId">学号</label>
                                <input id="loginStudentId" name="studentId" type="text" maxlength="12"
                                       value="${fn:escapeXml(loginStudentId)}" placeholder="8-12 位学号"
                                       inputmode="numeric" autocomplete="off" required>
                            </div>
                            <div class="field">
                                <label for="loginPhone">手机号</label>
                                <input id="loginPhone" name="phone" type="tel" maxlength="11"
                                       value="${fn:escapeXml(loginPhone)}" placeholder="报名时填写的手机号"
                                       inputmode="numeric" autocomplete="tel" required>
                            </div>
                            <button class="btn btn-primary btn-block" type="submit">查看我的报名</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <p class="auth-sub">使用注册时设置的学号与密码登录，登录后即可报名</p>
                        <form class="auth-form" method="post" action="${ctx}/login">
                            <input type="hidden" name="role" value="runner">
                            <input type="hidden" name="mode" value="account">
                            <div class="field">
                                <label for="accountStudentId">学号</label>
                                <input id="accountStudentId" name="studentId" type="text" maxlength="12"
                                       value="${fn:escapeXml(loginStudentId)}" placeholder="注册时使用的学号"
                                       inputmode="numeric" autocomplete="username" required>
                            </div>
                            <div class="field">
                                <label for="accountPassword">登录密码</label>
                                <input id="accountPassword" name="password" type="password" maxlength="32"
                                       placeholder="请输入登录密码" autocomplete="current-password" required>
                            </div>
                            <button class="btn btn-primary btn-block" type="submit">登录并进入报名</button>
                        </form>
                        <p class="auth-alt">还没有账号？<a href="${ctx}/register">注册参赛账号 →</a></p>
                    </c:otherwise>
                </c:choose>

                <c:if test="${activeMode ne 'phone'}">
                    <p class="auth-alt">老用户也可<a href="${ctx}/login?role=runner&mode=phone">用学号 + 手机号登录</a></p>
                </c:if>
            </c:otherwise>
        </c:choose>

        <div class="auth-foot">
            登录即表示同意赛事安全承诺书与个人信息使用说明。参赛者仅能查看与修改本人的报名记录。
        </div>
    </section>
</main>

<%@ include file="footer.jsp" %>
