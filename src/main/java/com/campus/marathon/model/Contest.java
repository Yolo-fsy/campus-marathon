package com.campus.marathon.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 竞赛实体：描述一场竞赛活动及其生命周期状态。
 */
public class Contest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 草稿：仅管理员可编辑维度和参赛者 */
    public static final String STATUS_DRAFT = "DRAFT";
    /** 评审中：维度不可再改，评审可登录评分 */
    public static final String STATUS_JUDGING = "JUDGING";
    /** 已结束：排名锁定 */
    public static final String STATUS_FINISHED = "FINISHED";

    private long id;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatusLabel() {
        if (status == null) {
            return "";
        }
        switch (status) {
            case STATUS_DRAFT:
                return "草稿";
            case STATUS_JUDGING:
                return "评审中";
            case STATUS_FINISHED:
                return "已结束";
            default:
                return status;
        }
    }

    public boolean isDraft() {
        return STATUS_DRAFT.equals(status);
    }

    public boolean isJudging() {
        return STATUS_JUDGING.equals(status);
    }

    public boolean isFinished() {
        return STATUS_FINISHED.equals(status);
    }
}
