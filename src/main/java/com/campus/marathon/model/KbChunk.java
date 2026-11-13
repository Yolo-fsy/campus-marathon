package com.campus.marathon.model;

/**
 * 知识库切片：文档按窗口切分后的最小检索单元。
 */
public class KbChunk {

    private Long id;
    private Long docId;
    private String docTitle;
    private int seq;
    private String content;
    private int charCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getCharCount() {
        return charCount;
    }

    public void setCharCount(int charCount) {
        this.charCount = charCount;
    }

    /** 引用卡片用的短预览，避免页面被整段资料撑开。 */
    public String getPreview() {
        if (content == null) {
            return "";
        }
        String compact = content.replace('\n', ' ').trim();
        return compact.length() <= 120 ? compact : compact.substring(0, 120) + "…";
    }
}
