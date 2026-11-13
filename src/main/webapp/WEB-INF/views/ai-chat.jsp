<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="AI 知识库问答"/>
<c:set var="activeNav" value="ai"/>
<%@ include file="header.jsp" %>

<main class="page ai-page">
    <section class="ai-chat-shell">
        <aside class="panel ai-chat-side">
            <div class="scope-head">
                <span class="eyebrow">SCOPE</span>
                <h2>${empty doc ? '全部文档' : fn:escapeXml(doc.title)}</h2>
                <p>${empty doc ? '在你有权限查看的全部文档中检索。' : '只在这份文档的切片中检索。'}</p>
            </div>
            <nav class="scope-list" aria-label="选择检索范围">
                <a class="scope-item ${empty docId ? 'is-active' : ''}" href="${ctx}/ai/chat">
                    <span>全部文档</span>
                    <small>${fn:length(documents)} 份</small>
                </a>
                <c:forEach var="item" items="${documents}">
                    <a class="scope-item ${docId eq item.id ? 'is-active' : ''}"
                       href="${ctx}/ai/chat?docId=${item.id}">
                        <span>${fn:escapeXml(item.title)}</span>
                        <small>${item.chunkCount} 个切片</small>
                    </a>
                </c:forEach>
            </nav>
            <a class="btn btn-ghost btn-sm scope-back" href="${ctx}/ai">返回文档库</a>
        </aside>

        <section class="panel ai-chat-main">
            <div class="chat-log" id="chatLog" aria-live="polite">
                <div class="chat-bubble is-bot">
                    你好，我是知识库助手。选择左侧的检索范围后直接提问，我会先找最相关的片段，再据此回答并标注引用。
                </div>
            </div>
            <p class="chat-mode" id="chatMode"></p>
            <form class="chat-form" id="chatForm">
                <textarea id="chatInput" rows="2" maxlength="500"
                          placeholder="例如：报名流程和缴费方式是什么？"></textarea>
                <button class="btn btn-primary" id="chatSend" type="submit">发送</button>
            </form>
            <p class="chat-note">回答只依据已上传的资料，资料不足时会直接说明。</p>
            <input type="hidden" id="chatDocId" value="${docId}">
        </section>
    </section>
</main>

<script>window.KB_CTX = '${ctx}';</script>
<script src="${ctx}/assets/js/ai.js?v=1" charset="UTF-8" defer></script>
<%@ include file="footer.jsp" %>
