package com.campus.marathon.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 知识库文档实体：一份上传的资料，切片后的记录挂在 {@link KbChunk} 上。
 */
public class KbDocument {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;
    private String ownerStudentId;
    private String ownerName;
    private String title;
    private String fileName;
    private String contentType;
    private int charCount;
    private int chunkCount;
    private LocalDateTime createdAt;
    private boolean adminVisible;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerStudentId() {
        return ownerStudentId;
    }

    public void setOwnerStudentId(String ownerStudentId) {
        this.ownerStudentId = ownerStudentId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getCharCount() {
        return charCount;
    }

    public void setCharCount(int charCount) {
        this.charCount = charCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdminVisible() {
        return adminVisible;
    }

    public void setAdminVisible(boolean adminVisible) {
        this.adminVisible = adminVisible;
    }

    /* ---------- 供 JSP 直接展示的派生属性 ---------- */

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(DATE_TIME_FORMATTER);
    }

    /** 文件类型标签，用于列表卡片角标。 */
    public String getFileType() {
        if (fileName == null) {
            return "FILE";
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "PDF";
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "MD";
        }
        return "TXT";
    }
}
