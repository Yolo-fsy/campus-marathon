package com.campus.marathon.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评审账号实体：每场竞赛独立管理评审账号，避免跨竞赛数据混淆。
 */
public class Judge implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private long contestId;
    /** 评审登录名 */
    private String username;
    /** 展示名（真名或花名） */
    private String displayName;
    /** SHA-256(salt + password) 的十六进制字符串 */
    private String credential;
    /** 加盐值（明文） */
    private String salt;
    private LocalDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getContestId() {
        return contestId;
    }

    public void setContestId(long contestId) {
        this.contestId = contestId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
