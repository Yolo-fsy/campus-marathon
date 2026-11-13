<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:choose>
    <c:when test="${formMode eq 'runner-edit'}">
        <c:set var="formAction" value="${ctx}/my/save"/>
        <c:set var="cancelUrl" value="${ctx}/my"/>
        <c:set var="headline" value="修改我的报名"/>
        <c:set var="subline" value="仅可修改本人信息，学号与审核状态由系统维护"/>
        <c:set var="modeLabel" value="参赛者中心"/>
        <c:set var="submitLabel" value="保存修改"/>
        <c:set var="pageTitle" value="修改我的报名"/>
    </c:when>
    <c:when test="${formMode eq 'apply'}">
        <c:set var="formAction" value="${ctx}/apply"/>
        <c:set var="cancelUrl" value="${ctx}/apply"/>
        <c:set var="headline" value="填写参赛人信息"/>
        <c:set var="subline" value="已选择${form.eventCategoryLabel}，提交后进入 30 元报名费支付与审核"/>
        <c:set var="modeLabel" value="第 2 步"/>
        <c:set var="submitLabel" value="提交并支付报名费"/>
        <c:set var="pageTitle" value="填写参赛人信息"/>
    </c:when>
    <c:when test="${formMode eq 'edit'}">
        <c:set var="formAction" value="${ctx}/admin/registrations/save"/>
        <c:set var="cancelUrl" value="${ctx}/admin/registrations"/>
        <c:set var="headline" value="编辑报名"/>
        <c:set var="subline" value="修改后立即生效，可同时调整审核状态"/>
        <c:set var="modeLabel" value="管理端"/>
        <c:set var="submitLabel" value="保存报名"/>
        <c:set var="pageTitle" value="编辑报名"/>
    </c:when>
    <c:otherwise>
        <c:set var="formAction" value="${ctx}/admin/registrations/save"/>
        <c:set var="cancelUrl" value="${ctx}/admin/registrations"/>
        <c:set var="headline" value="新建报名"/>
        <c:set var="subline" value="管理员录入报名，可直接设置审核状态"/>
        <c:set var="modeLabel" value="管理端"/>
        <c:set var="submitLabel" value="保存报名"/>
        <c:set var="pageTitle" value="新建报名"/>
    </c:otherwise>
</c:choose>
<%@ include file="header.jsp" %>

<style>
    .locked-pick {
        display: flex; align-items: center; gap: 14px;
        padding: 16px 18px; border-radius: 16px;
        background: linear-gradient(135deg, #fff6f1, #fdf1ea);
        border: 1px solid rgba(255, 90, 31, .28);
        box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .6);
    }
    .locked-pick-dot {
        flex: 0 0 auto; width: 11px; height: 11px; border-radius: 50%;
        background: var(--brand); box-shadow: 0 0 0 5px rgba(255, 90, 31, .16);
    }
    .locked-pick-text { flex: 1 1 auto; min-width: 0; }
    .locked-pick-text strong { display: block; font-size: 17px; color: var(--ink); letter-spacing: -.01em; }
    .locked-pick-text span { display: block; font-size: 12.5px; color: var(--muted); margin-top: 2px; }
    .locked-pick .btn { flex: 0 0 auto; }
</style>

<main class="page">
    <div class="form-head">
        <div>
            <h1>${headline}</h1>
            <p>${subline}</p>
        </div>
        <span class="mode-tag">${modeLabel}</span>
    </div>

    <c:if test="${not empty errors}">
        <div class="alert alert-error" role="alert">提交未通过校验，请检查下方标红的字段后重新提交。</div>
    </c:if>

    <form id="registrationForm" method="post" action="${formAction}" novalidate>
        <c:if test="${formMode eq 'edit'}">
            <input type="hidden" name="id" value="${form.id}">
        </c:if>

        <div class="form-layout">
            <section class="form-card">
                <div class="card-head">
                    <span class="step-num">01</span>
                    <div>
                        <h2>参赛者信息</h2>
                        <p>信息将用于赛事核验与紧急联系，请与校园卡保持一致</p>
                    </div>
                </div>

                <div class="field ${not empty errors.name ? 'has-error' : ''}">
                    <label for="name">参赛者姓名 <span class="req">*</span></label>
                    <input id="name" name="name" type="text" maxlength="20"
                           value="${fn:escapeXml(form.name)}" placeholder="请输入真实姓名"
                           autocomplete="name" required>
                    <p class="hint">2-20 个字符，支持中英文</p>
                    <p class="error-text">${errors.name}</p>
                </div>

                <div class="field-row">
                    <div class="field ${not empty errors.studentId ? 'has-error' : ''}">
                        <label for="studentId">学号 <span class="req">*</span></label>
                        <input id="studentId" name="studentId" type="text" maxlength="12"
                               value="${fn:escapeXml(form.studentId)}" placeholder="8-12 位数字"
                               inputmode="numeric" autocomplete="off"
                               ${(formMode eq 'runner-edit' or formMode eq 'apply') ? 'readonly' : ''} required>
                        <c:choose>
                            <c:when test="${formMode eq 'runner-edit'}">
                                <p class="hint">学号用于身份核验，不可修改</p>
                            </c:when>
                            <c:when test="${formMode eq 'apply'}">
                                <p class="hint">学号取自当前登录账号，如需更换请重新注册</p>
                            </c:when>
                            <c:otherwise>
                                <p class="hint">8-12 位数字，作为参赛身份唯一标识</p>
                            </c:otherwise>
                        </c:choose>
                        <p class="error-text">${errors.studentId}</p>
                    </div>

                    <div class="field ${not empty errors.phone ? 'has-error' : ''}">
                        <label for="phone">联系电话 <span class="req">*</span></label>
                        <input id="phone" name="phone" type="tel" maxlength="11"
                               value="${fn:escapeXml(form.phone)}" placeholder="11 位手机号"
                               inputmode="numeric" autocomplete="tel" required>
                        <p class="hint">用于审核通知与赛事紧急联系</p>
                        <p class="error-text">${errors.phone}</p>
                    </div>
                </div>

                <div class="field ${not empty errors.college ? 'has-error' : ''}">
                    <label for="college">所属学院</label>
                    <input id="college" name="college" type="text" maxlength="40"
                           value="${fn:escapeXml(form.college)}" placeholder="如：计算机科学与技术学院">
                    <p class="hint">选填，方便赛后按学院统计参赛数据</p>
                    <p class="error-text">${errors.college}</p>
                </div>
            </section>

            <section class="form-card">
                <div class="card-head">
                    <span class="step-num">02</span>
                    <div>
                        <c:choose>
                            <c:when test="${formMode eq 'apply'}">
                                <h2>参赛项目</h2>
                                <p>已锁定你选择的赛道，如需更换请返回上一步重选</p>
                            </c:when>
                            <c:otherwise>
                                <h2>参赛项目与审核</h2>
                                <p>名额实时校验，报名成功即锁定对应项目名额</p>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <div class="field ${not empty errors.eventCategory ? 'has-error' : ''}">
                    <label>参赛项目 <span class="req">*</span></label>
                    <c:choose>
                        <c:when test="${formMode eq 'apply'}">
                            <div class="locked-pick">
                                <span class="locked-pick-dot" aria-hidden="true"></span>
                                <div class="locked-pick-text">
                                    <strong>${form.eventCategoryLabel}</strong>
                                    <span>${form.eventCategoryDistance} · 名额由系统实时校验</span>
                                </div>
                                <a class="btn btn-outline btn-sm" href="${ctx}/apply">更换项目</a>
                            </div>
                            <input type="hidden" name="eventCategory" value="${form.eventCategory}">
                            <p class="choice-note">项目在第一步已选定；若选错可点击「更换项目」返回选择页。</p>
                        </c:when>
                        <c:otherwise>
                            <div class="choice-grid" role="radiogroup" aria-label="参赛项目">
                                <c:forEach var="category" items="${categories}">
                                    <label class="choice">
                                        <input type="radio" name="eventCategory" value="${category.code}"
                                               ${form.eventCategory eq category.code ? 'checked' : ''}>
                                        <span class="choice-body">
                                            <span class="choice-title">${category.label}</span>
                                            <span class="choice-meta">${category.distance} · 限额 ${category.capacity} 人</span>
                                            <span class="choice-check" aria-hidden="true"></span>
                                        </span>
                                    </label>
                                </c:forEach>
                            </div>
                            <p class="choice-note">通过审核后名额才会被占用；满额项目无法再通过新的报名。</p>
                        </c:otherwise>
                    </c:choose>
                    <p class="error-text">${errors.eventCategory}</p>
                </div>

                <c:choose>
                    <c:when test="${formMode eq 'runner-edit' or formMode eq 'apply'}">
                        <div class="field">
                            <label>报名状态</label>
                            <div class="readonly-status">
                                <span>状态由管理员审核维护</span>
                                <span class="status ${form.statusClass}">${form.statusLabel}</span>
                            </div>
                            <c:choose>
                                <c:when test="${resubmit}">
                                    <p class="hint">原报名已驳回或已取消，保存后将重新进入待审核队列。</p>
                                </c:when>
                                <c:otherwise>
                                    <p class="hint">修改信息不会改变当前审核状态。</p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="field ${not empty errors.status ? 'has-error' : ''}">
                            <label for="status">报名状态</label>
                            <select id="status" name="status">
                                <option value="PENDING" ${empty form.status or form.status eq 'PENDING' ? 'selected' : ''}>待审核（不占用名额）</option>
                                <option value="CONFIRMED" ${form.status eq 'CONFIRMED' ? 'selected' : ''}>报名成功（占用名额）</option>
                                <option value="REJECTED" ${form.status eq 'REJECTED' ? 'selected' : ''}>驳回</option>
                            </select>
                            <p class="hint">管理员可直接调整审核结果，通过时系统会校验剩余名额</p>
                            <p class="error-text">${errors.status}</p>
                        </div>
                    </c:otherwise>
                </c:choose>

                <div class="field ${not empty errors.remark ? 'has-error' : ''}">
                    <label for="remark">
                        备注说明
                        <span class="counter" id="remarkCount">${fn:length(form.remark)}/120</span>
                    </label>
                    <textarea id="remark" name="remark" maxlength="120"
                              placeholder="如健康状况、无障碍需求或团队方阵信息（选填）"><c:out value="${form.remark}"/></textarea>
                    <p class="error-text">${errors.remark}</p>
                </div>
            </section>
        </div>

        <div class="form-actions">
            <a class="btn btn-cancel" href="${cancelUrl}">取消返回</a>
            <button class="btn btn-primary" type="submit">${submitLabel}</button>
        </div>
    </form>
</main>

<script>
    (function () {
        var form = document.getElementById('registrationForm');
        if (!form) {
            return;
        }

        var rules = {
            name: function (value) {
                if (!value) {
                    return '请填写参赛者姓名';
                }
                return /^[\u4e00-\u9fa5A-Za-z·\s]{2,20}$/.test(value)
                    ? '' : '姓名为 2-20 个字符，仅支持中英文字母';
            },
            studentId: function (value) {
                if (!value) {
                    return '请填写学号';
                }
                return /^\d{8,12}$/.test(value) ? '' : '学号应为 8-12 位数字';
            },
            phone: function (value) {
                if (!value) {
                    return '请填写联系电话';
                }
                return /^1[3-9]\d{9}$/.test(value) ? '' : '请输入 11 位有效手机号';
            },
            college: function (value) {
                return value.length <= 40 ? '' : '学院名称不超过 40 个字符';
            },
            remark: function (value) {
                return value.length <= 120 ? '' : '备注不超过 120 个字符';
            }
        };

        function validateField(input) {
            var rule = rules[input.name];
            if (!rule) {
                return true;
            }
            var field = input.closest('.field');
            var errorText = field ? field.querySelector('.error-text') : null;
            var message = rule(input.value.trim());
            if (field) {
                field.classList.toggle('has-error', !!message);
            }
            if (errorText) {
                errorText.textContent = message;
            }
            return !message;
        }

        Object.keys(rules).forEach(function (name) {
            var input = form.elements[name];
            if (!input) {
                return;
            }
            input.addEventListener('blur', function () {
                validateField(input);
            });
            input.addEventListener('input', function () {
                if (input.closest('.field').classList.contains('has-error')) {
                    validateField(input);
                }
            });
        });

        var remark = form.elements['remark'];
        var counter = document.getElementById('remarkCount');
        if (remark && counter) {
            var updateCounter = function () {
                counter.textContent = remark.value.length + '/120';
            };
            remark.addEventListener('input', updateCounter);
            updateCounter();
        }

        form.addEventListener('submit', function (event) {
            var firstInvalid = null;
            Object.keys(rules).forEach(function (name) {
                var input = form.elements[name];
                if (input && !validateField(input) && !firstInvalid) {
                    firstInvalid = input;
                }
            });
            if (firstInvalid) {
                event.preventDefault();
                firstInvalid.focus();
            }
        });
    })();
</script>

<%@ include file="footer.jsp" %>
