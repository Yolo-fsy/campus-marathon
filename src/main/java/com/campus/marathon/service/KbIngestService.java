package com.campus.marathon.service;

import com.campus.marathon.dao.KbChunkDAO;
import com.campus.marathon.dao.KbDocumentDAO;
import com.campus.marathon.dao.impl.KbChunkDAOImpl;
import com.campus.marathon.dao.impl.KbDocumentDAOImpl;
import com.campus.marathon.model.KbChunk;
import com.campus.marathon.model.KbDocument;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.util.KbSchema;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 文档导入：提取文本 → 切片 → 文档入库 → 切片批量入库。
 */
public class KbIngestService {

    private final KbDocumentDAO documentDAO = new KbDocumentDAOImpl();
    private final KbChunkDAO chunkDAO = new KbChunkDAOImpl();
    private final KbTextExtractor extractor = new KbTextExtractor();
    private final Chunker chunker = new Chunker();

    public KbDocument ingest(SessionUser owner, String fileName, InputStream input)
            throws SQLException, IOException {
        KbSchema.init();
        String text = extractor.extract(fileName, input);
        if (text.isBlank()) {
            throw new IOException("未提取到文本，扫描版 PDF 需要先做 OCR 再上传");
        }
        List<String> pieces = chunker.split(text);
        if (pieces.isEmpty()) {
            throw new IOException("文档内容为空，未能生成切片");
        }

        String ownerId = owner.getStudentId() == null ? owner.getUsername() : owner.getStudentId();
        KbDocument document = new KbDocument();
        document.setOwnerStudentId(ownerId);
        document.setOwnerName(owner.getDisplayName());
        document.setTitle(deriveTitle(fileName));
        document.setFileName(fileName);
        document.setContentType(contentTypeOf(fileName));
        document.setCharCount(text.length());
        document.setChunkCount(pieces.size());
        document.setCreatedAt(LocalDateTime.now());
        document.setAdminVisible(owner.isAdmin());
        documentDAO.create(document);

        List<KbChunk> chunks = new ArrayList<>(pieces.size());
        for (int i = 0; i < pieces.size(); i++) {
            KbChunk chunk = new KbChunk();
            chunk.setDocId(document.getId());
            chunk.setSeq(i);
            chunk.setContent(pieces.get(i));
            chunk.setCharCount(pieces.get(i).length());
            chunks.add(chunk);
        }
        chunkDAO.batchInsert(chunks);
        return document;
    }

    public KbDocument ingest(SessionUser owner, String fileName, byte[] content)
            throws SQLException, IOException {
        try (InputStream input = new ByteArrayInputStream(content)) {
            return ingest(owner, fileName, input);
        }
    }

    static String deriveTitle(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "未命名文档";
        }
        String name = fileName;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.isBlank() ? "未命名文档" : name;
    }

    private String contentTypeOf(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "text/markdown";
        }
        return "text/plain";
    }
}
