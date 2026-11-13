/* =========================================================
   赛事智能客服（纯前端，无接口依赖）
   ========================================================= */
(function () {
    'use strict';

    if (window.__csBooted) {
        return;
    }
    window.__csBooted = true;

    var HUMAN_PHONE = '13677042040';

    /* ---------------- 内置知识库 ---------------- */
    var FAQ = [
        {
            keys: ['你好', '您好', '在吗', 'hi', 'hello', '嗨'],
            a: '你好呀，我是赛事智能客服小马。\n报名、缴费、名额、审核等问题都可以直接问我，也可以在下方点击常见问题快速提问。'
        },
        {
            keys: ['怎么报名', '如何报名', '报名流程', '报名步骤', '怎么参加', '如何参加', '报名方法'],
            a: '报名四步走：\n1. 注册参赛账号（填写姓名、学号、手机号、所属学院，并输入 4 位图形验证码）；\n2. 使用学号 + 密码登录；\n3. 进入「在线报名」选择参赛项目并提交表单；\n4. 等待审核通过后，在「我的报名」里完成 30 元报名费支付，缴费成功即报名成功。'
        },
        {
            keys: ['报名费', '多少钱', '费用', '价格', '收费', '30元', '缴费'],
            a: '本次赛事报名费统一为 30 元 / 人，三个项目价格一致。\n审核通过后在「我的报名」中点击支付，通过支付宝完成缴费即可。'
        },
        {
            keys: ['半程', '半马', 'HALF', '21公里', '21.0975'],
            a: '半程马拉松：21.0975 km，名额 300 人。\n适合有一定长跑基础、能完成 20 km 以上训练量的同学。'
        },
        {
            keys: ['迷你', 'MINI', '5公里', '五公里'],
            a: '迷你马拉松：5 km，名额 500 人。\n强度适中，适合大多数首次参赛的同学。'
        },
        {
            keys: ['欢乐跑', 'FUN', '3公里', '三公里'],
            a: '欢乐跑：3 km，名额 800 人。\n不计竞技排名，适合结伴参与、体验赛事氛围。'
        },
        {
            keys: ['项目', '组别', '里程', '距离', '几公里', '都有什么', '哪些项目'],
            a: '本届赛事设三个项目：\n· 半程马拉松 21.0975 km，名额 300 人；\n· 迷你马拉松 5 km，名额 500 人；\n· 欢乐跑 3 km，名额 800 人。\n可在「在线报名」页面按自身情况选择其一。'
        },
        {
            keys: ['名额', '容量', '多少人', '人数', '报满', '满了'],
            a: '三个项目名额合计 1600 人：\n· 半程马拉松 300 人\n· 迷你马拉松 500 人\n· 欢乐跑 800 人\n名额以审核通过并完成缴费的顺序为准，额满即止。'
        },
        {
            keys: ['审核', '多久', '什么时候通过', '待审核', 'PENDING', '通过'],
            a: '报名提交后为「待审核」状态，由赛事管理员人工审核，通常 1 个工作日内处理完成。\n审核通过后状态变为「已确认」，请在「我的报名」中及时完成缴费；只有审核通过并缴费成功的报名才真正占用名额。'
        },
        {
            keys: ['取消', '退款', '退费', '退钱', '不参加', '撤销'],
            a: '报名可在「我的报名」中取消。\n· 未缴费：取消后不产生任何费用；\n· 已缴费：取消后报名费将原路退回支付宝账户，一般 1-3 个工作日到账；\n· 已确认名额取消后会释放给其他同学。'
        },
        {
            keys: ['支付', '付款', '支付宝', '怎么交钱', '缴费方式', '沙箱'],
            a: '支付方式为支付宝网页支付。\n点击「去支付」后会跳转到支付宝收银台完成付款，支付成功会同步回到系统并更新报名状态。\n注意：当前系统接入的是支付宝沙箱环境，属于测试通道，不会真实扣款。'
        },
        {
            keys: ['修改', '更改', '改信息', '改项目', '换项目', '信息填错', '填错'],
            a: '已提交的报名可以在「我的报名」中修改参赛项目与备注信息。\n学号作为唯一身份标识不可修改；如姓名、手机号有误，请携带学生证联系人工客服处理。'
        },
        {
            keys: ['验证码', '图形验证码', '看不清', '验证码错误', '收不到'],
            a: '注册页面使用的是 4 位数字图形验证码：\n· 有效期 5 分钟，提交一次后即失效，需要重新获取；\n· 请区分数字，避免与背景线条混淆；\n· 若始终提示错误，可刷新页面重新生成后再试。'
        },
        {
            keys: ['登录不了', '登录失败', '忘记密码', '密码错误', '账号', '注册不了', '重复注册'],
            a: '学号即登录账号，一个学号只能注册一次。\n忘记密码暂不支持自助重置，请提供学号与姓名联系人工客服协助处理。',
            human: true
        },
        {
            keys: ['时间', '地点', '在哪', '什么时候比赛', '领物', '装备', '参赛包'],
            a: '具体的比赛日期、起终点与领物安排由赛事组委会统一发布，为避免信息误差，请直接联系人工客服确认最新安排。',
            human: true
        },
        {
            keys: ['人工', '客服', '电话', '联系', '人工客服', '转人工'],
            a: '好的，正在为您转接人工客服。'
        }
    ];

    var GREETING = '你好，我是赛事智能客服小马。\n报名、缴费、名额、审核等问题都可以直接问我。';
    var FALLBACK = '这个问题我暂时还回答不了，建议联系人工客服处理。';

    var CHIPS = ['怎么报名', '报名费多少', '有哪些项目', '怎么取消退款', '如何支付', '转人工服务'];

    /* ---------------- 文本处理 ---------------- */
    function norm(text) {
        return String(text || '')
            .toLowerCase()
            .replace(/[^\u4e00-\u9fa5a-z0-9]/g, '');
    }

    function pickAnswer(raw) {
        var q = norm(raw);
        if (!q) {
            return { text: '请简单描述一下你的问题，比如「怎么报名」「报名费多少」。' };
        }
        var best = null;
        var bestScore = 0;
        for (var i = 0; i < FAQ.length; i++) {
            var item = FAQ[i];
            var score = 0;
            for (var j = 0; j < item.keys.length; j++) {
                var key = norm(item.keys[j]);
                if (key && q.indexOf(key) !== -1) {
                    score += key.length;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = item;
            }
        }
        if (best && bestScore >= 2) {
            var hit = best.human === true || best.keys.some(function (k) {
                var nk = norm(k);
                return nk === '人工' || nk === '转人工' || nk === '客服';
            });
            return { text: best.a, human: hit };
        }
        return { text: FALLBACK, human: true };
    }

    /* ---------------- DOM 构建 ---------------- */
    function el(tag, cls, html) {
        var node = document.createElement(tag);
        if (cls) {
            node.className = cls;
        }
        if (html != null) {
            node.innerHTML = html;
        }
        return node;
    }

    var ICON_CHAT = '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true">' +
        '<path d="M21 12a8.5 8.5 0 0 1-8.5 8.5H6L3 23V12A8.5 8.5 0 0 1 11.5 3.5h1A8.5 8.5 0 0 1 21 12z" ' +
        'stroke="#fff" stroke-width="2" stroke-linejoin="round"/></svg>';

    var ICON_PHONE = '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true">' +
        '<path d="M6.6 3.5h2.2l1.6 4-1.9 1.3a11 11 0 0 0 5.4 5.4l1.3-1.9 4 1.6v2.2a2.6 2.6 0 0 1-2.7 2.6A15.3 15.3 0 0 1 4 6.2 2.6 2.6 0 0 1 6.6 3.5z" ' +
        'fill="#e2470f"/></svg>';

    var ICON_COPY = '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true">' +
        '<rect x="8.5" y="8.5" width="10" height="10" rx="2" stroke="#e2470f" stroke-width="2"/>' +
        '<path d="M6 15.5H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h8.5a2 2 0 0 1 2 2v1" stroke="#e2470f" stroke-width="2" stroke-linecap="round"/></svg>';

    var root = el('div', 'cs-root');

    var launcher = el('button', 'cs-launcher');
    launcher.type = 'button';
    launcher.setAttribute('aria-expanded', 'false');
    launcher.setAttribute('aria-label', '打开在线客服');
    launcher.innerHTML = ICON_CHAT + '<span>在线客服</span><i class="cs-launcher-dot"></i>';

    var panel = el('div', 'cs-panel');
    panel.hidden = true;
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', '赛事智能客服');

    var head = el('div', 'cs-head');
    head.innerHTML =
        '<span class="cs-head-avatar">AI</span>' +
        '<span class="cs-head-meta"><strong>赛事智能客服</strong>' +
        '<small>常见问题自动应答</small></span>';
    var closeBtn = el('button', 'cs-close', '&times;');
    closeBtn.type = 'button';
    closeBtn.setAttribute('aria-label', '关闭客服窗口');
    head.appendChild(closeBtn);

    var body = el('div', 'cs-body');

    var form = el('form', 'cs-form');
    var input = el('input', 'cs-input');
    input.type = 'text';
    input.placeholder = '请输入你的问题…';
    input.setAttribute('autocomplete', 'off');
    input.setAttribute('maxlength', '80');
    var sendBtn = el('button', 'cs-send', '发送');
    sendBtn.type = 'submit';
    form.appendChild(input);
    form.appendChild(sendBtn);

    panel.appendChild(head);
    panel.appendChild(body);
    panel.appendChild(form);
    root.appendChild(panel);
    root.appendChild(launcher);

    /* ---------------- 渲染逻辑 ---------------- */
    function scrollBottom() {
        body.scrollTop = body.scrollHeight;
    }

    function appendUser(text) {
        var wrap = el('div', 'cs-msg user');
        wrap.appendChild(el('div', 'cs-bubble', escapeHtml(text)));
        body.appendChild(wrap);
        scrollBottom();
    }

    function humanActions() {
        var actions = el('div', 'cs-actions');

        var call = el('a', 'cs-action');
        call.href = 'tel:' + HUMAN_PHONE;
        call.innerHTML = ICON_PHONE + '<span>拨打 ' + HUMAN_PHONE + '</span>';

        var copy = el('button', 'cs-action');
        copy.type = 'button';
        copy.innerHTML = ICON_COPY + '<span>复制号码</span>';
        copy.addEventListener('click', function () {
            var ok = function () {
                copy.lastChild.textContent = '已复制';
                setTimeout(function () {
                    copy.lastChild.textContent = '复制号码';
                }, 1600);
            };
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(HUMAN_PHONE).then(ok, ok);
            } else {
                var tmp = el('input');
                tmp.value = HUMAN_PHONE;
                document.body.appendChild(tmp);
                tmp.select();
                try {
                    document.execCommand('copy');
                } catch (e) {
                    /* 忽略不支持的环境 */
                }
                document.body.removeChild(tmp);
                ok();
            }
        });

        actions.appendChild(call);
        actions.appendChild(copy);
        return actions;
    }

    function appendBot(text, human) {
        var wrap = el('div', 'cs-msg bot');
        var bubble = el('div', 'cs-bubble', escapeHtml(text));
        var block = el('div');
        block.appendChild(bubble);
        if (human) {
            block.appendChild(humanActions());
        }
        wrap.appendChild(block);
        body.appendChild(wrap);
        scrollBottom();
    }

    function escapeHtml(text) {
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function showChips() {
        var chips = el('div', 'cs-chips');
        CHIPS.forEach(function (label) {
            var chip = el('button', 'cs-chip', label);
            chip.type = 'button';
            chip.addEventListener('click', function () {
                chips.remove();
                ask(label);
            });
            chips.appendChild(chip);
        });
        body.appendChild(chips);
        scrollBottom();
    }

    var typing = null;
    var busy = false;

    function ask(raw) {
        if (busy) {
            return;
        }
        var text = String(raw || '').trim();
        if (!text) {
            return;
        }
        busy = true;
        sendBtn.disabled = true;
        appendUser(text);

        typing = el('div', 'cs-msg bot');
        var t = el('div', 'cs-typing', '<span></span><span></span><span></span>');
        var holder = el('div');
        holder.appendChild(t);
        typing.appendChild(holder);
        body.appendChild(typing);
        scrollBottom();

        setTimeout(function () {
            if (typing && typing.parentNode) {
                typing.parentNode.removeChild(typing);
            }
            typing = null;
            var res = pickAnswer(text);
            appendBot(res.text, res.human);
            busy = false;
            sendBtn.disabled = false;
        }, 520);
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        var text = input.value;
        input.value = '';
        ask(text);
        input.focus();
    });

    function openPanel() {
        panel.hidden = false;
        launcher.setAttribute('aria-expanded', 'true');
        input.focus();
        scrollBottom();
    }

    function closePanel() {
        panel.hidden = true;
        launcher.setAttribute('aria-expanded', 'false');
        launcher.focus();
    }

    launcher.addEventListener('click', function () {
        if (panel.hidden) {
            openPanel();
        } else {
            closePanel();
        }
    });

    closeBtn.addEventListener('click', closePanel);

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !panel.hidden) {
            closePanel();
        }
    });

    if (document.body) {
        document.body.appendChild(root);
    } else {
        document.addEventListener('DOMContentLoaded', function () {
            document.body.appendChild(root);
        });
    }

    appendBot(GREETING, false);
    showChips();
})();
