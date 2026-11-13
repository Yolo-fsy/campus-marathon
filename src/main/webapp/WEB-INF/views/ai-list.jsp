<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="AI 知识库"/>
<c:set var="activeNav" value="ai"/>
<%@ include file="header.jsp" %>

<main class="page ai-page">
    <section class="hero">
        <div class="hero-copy">
            <span class="eyebrow">KNOWLEDGE BASE</span>
            <h1>AI 知识库<br><em>问答助手</em></h1>
            <p>上传课程资料、赛事手册或笔记，系统自动切片入库；提问时先做相关度检索，再由大模型流式作答，并标注引用片段。</p>
            <div class="hero-actions">
                <a class="btn btn-primary" href="${ctx}/ai/chat">开始提问</a>
                <a class="btn btn-ghost" href="#kbDocs">查看文档</a>
            </div>
        </div>
        <div class="hero-side">
            <div class="hero-event">
                <span class="event-date">${fn:length(documents)} 份文档</span>
                <span class="event-name">检索增强问答</span>
                <span class="event-meta">中文 bigram 分词 · BM25 排序 · SSE 流式输出</span>
            </div>
        </div>
    </section>

    <c:if test="${not empty msg}">
        <div class="alert alert-success kb-alert" role="status">${fn:escapeXml(msg)}</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-error kb-alert" role="alert">${fn:escapeXml(error)}</div>
    </c:if>

    <section class="panel upload-panel">
        <form class="upload-form" id="kbUploadForm" method="post" action="${ctx}/ai/upload"
              enctype="multipart/form-data">
            <label class="upload-drop" id="kbDrop" for="kbFile">
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path d="M12 16V4m0 0l-4 4m4-4l4 4" stroke="#ff5a1f" stroke-width="2"
                          stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M4 17v2a2 2 0 002 2h12a2 2 0 002-2v-2" stroke="#ff5a1f"
                          stroke-width="2" stroke-linecap="round"/>
                </svg>
                <strong>拖拽或点击上传文档</strong>
                <span>支持 .txt / .md / .pdf，单个文件不超过 5MB</span>
            </label>
            <input id="kbFile" class="upload-input" type="file" name="file"
                   accept=".txt,.md,.markdown,.pdf" required>
            <div class="upload-actions">
                <span class="upload-name" id="kbFileName">尚未选择文件</span>
                <button class="btn btn-primary" type="submit">上传并切片</button>
            </div>
        </form>
    </section>

    <section class="panel kb-docs" id="kbDocs">
        <div class="roster-head">
            <div class="roster-title">
                <h2>我的文档</h2>
                <span>共 ${fn:length(documents)} 份</span>
            </div>
            <span class="kb-hint">切片窗口 500 字 · 重叠 100 字</span>
        </div>

        <c:choose>
            <c:when test="${empty documents}">
                <div class="empty-state">
                    <div class="empty-icon" aria-hidden="true">
                        <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                            <path d="M6 3h9l4 4v14a1 1 0 01-1 1H6a1 1 0 01-1-1V4a1 1 0 011-1z"
                                  stroke="#e2470f" stroke-width="1.8"/>
                            <path d="M15 3v5h4M8 13h8M8 17h5" stroke="#e2470f" stroke-width="1.8"
                                  stroke-linecap="round"/>
                        </svg>
                    </div>
                    <h3>还没有上传任何文档</h3>
                    <p>先上传一份 .md 或 .txt 资料，系统会自动切片，随后就可以围绕它提问。</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="kb-grid">
                    <c:forEach var="doc" items="${documents}">
                        <article class="kb-card">
                            <div class="kb-card-top">
                                <span class="kb-type">${doc.fileType}</span>
                                <span class="kb-time">${doc.createdAtText}</span>
                            </div>
                            <h3>${fn:escapeXml(doc.title)}</h3>
                            <p class="kb-file">${fn:escapeXml(doc.fileName)}</p>
                            <div class="kb-meta">
                                <span>${doc.charCount} 字</span>
                                <span>${doc.chunkCount} 个切片</span>
                                <c:if test="${sessionScope.currentUser.admin}">
                                    <span>${fn:escapeXml(doc.ownerName)}</span>
                                </c:if>
                            </div>
                            <div class="kb-card-actions">
                                <a class="btn btn-outline btn-sm" href="${ctx}/ai/chat?docId=${doc.id}">提问</a>
                                <form method="post" action="${ctx}/ai/delete"
                                      onsubmit="return confirm('删除后该文档的切片与引用会一并移除，确定删除？');">
                                    <input type="hidden" name="id" value="${doc.id}">
                                    <button class="link-danger" type="submit">删除</button>
                                </form>
                            </div>
                        </article>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</main>

<script src="${ctx}/assets/js/ai.js?v=1" charset="UTF-8" defer></script>
<%@ include file="footer.jsp" %>
