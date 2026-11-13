package com.campus.marathon.web;

import com.campus.marathon.dao.KbDocumentDAO;
import com.campus.marathon.dao.impl.KbDocumentDAOImpl;
import com.campus.marathon.model.KbChunk;
import com.campus.marathon.model.KbDocument;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.service.Bm25Retriever;
import com.campus.marathon.service.KbChatService;
import com.campus.marathon.service.LlmClient;
import com.campus.marathon.util.KbSchema;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 问答接口：以 text/event-stream 逐 token 推送，结束后推送引用片段。
 */
public class KbAskServlet extends BaseServlet {

    private final KbDocumentDAO documentDAO = new KbDocumentDAOImpl();
    private final KbChatService chatService = new KbChatService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        PrintWriter writer = response.getWriter();
        try {
            SessionUser user = currentUser(request);
            KbSchema.init();
            String question = request.getParameter("question");
            if (question == null || question.trim().isEmpty()) {
                writeEvent(writer, "error", "{\"message\":\"请输入问题\"}");
                return;
            }
            List<Long> docIds = resolveDocIds(user, request.getParameter("docId"));
            List<Bm25Retriever.ScoredChunk> hits = chatService.retrieve(docIds, question);

            writeEvent(writer, "meta", "{\"mode\":\"" + (chatService.isModelReady() ? "llm" : "retrieval")
                    + "\",\"model\":\"" + LlmClient.escapeJson(chatService.getModelName())
                    + "\",\"hits\":" + hits.size() + "}");

            chatService.generate(question, hits, token -> {
                writeData(writer, "{\"t\":\"" + LlmClient.escapeJson(token) + "\"}");
                writer.flush();
            });

            writeEvent(writer, "citations", buildCitations(hits));
            writeData(writer, "[DONE]");
        } catch (Exception e) {
            String message = e.getMessage() == null ? "问答服务异常" : e.getMessage();
            writeEvent(writer, "error", "{\"message\":\"" + LlmClient.escapeJson(message) + "\"}");
        }
    }

    private List<Long> resolveDocIds(SessionUser user, String docIdParam) throws SQLException {
        if (docIdParam != null && !docIdParam.isBlank()) {
            long docId = parseId(docIdParam);
            KbDocument document = documentDAO.findById(docId);
            if (document == null) {
                throw new IllegalArgumentException("文档不存在或已被删除");
            }
            boolean owner = user.getStudentId() != null
                    && user.getStudentId().equals(document.getOwnerStudentId());
            if (!user.isAdmin() && !owner) {
                throw new IllegalArgumentException("无权访问该文档");
            }
            return Collections.singletonList(docId);
        }
        List<KbDocument> documents = user.isAdmin()
                ? documentDAO.listAll()
                : documentDAO.listByOwner(user.getStudentId());
        List<Long> docIds = new ArrayList<>(documents.size());
        for (KbDocument document : documents) {
            docIds.add(document.getId());
        }
        return docIds;
    }

    private String buildCitations(List<Bm25Retriever.ScoredChunk> hits) {
        StringBuilder json = new StringBuilder("{\"items\":[");
        for (int i = 0; i < hits.size(); i++) {
            Bm25Retriever.ScoredChunk hit = hits.get(i);
            KbChunk chunk = hit.getChunk();
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"index\":").append(i + 1)
                    .append(",\"docId\":").append(chunk.getDocId())
                    .append(",\"title\":\"").append(LlmClient.escapeJson(chunk.getDocTitle())).append('"')
                    .append(",\"seq\":").append(chunk.getSeq() + 1)
                    .append(",\"score\":\"").append(String.format(Locale.ROOT, "%.2f", hit.getScore())).append('"')
                    .append(",\"preview\":\"").append(LlmClient.escapeJson(chunk.getPreview())).append('"')
                    .append('}');
        }
        json.append("]}");
        return json.toString();
    }

    private void writeEvent(PrintWriter writer, String event, String data) {
        writer.write("event: " + event + "\n");
        writer.write("data: " + data + "\n\n");
        writer.flush();
    }

    private void writeData(PrintWriter writer, String data) {
        writer.write("data: " + data + "\n\n");
        writer.flush();
    }
}
