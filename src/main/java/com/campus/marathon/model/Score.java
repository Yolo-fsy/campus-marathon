package com.campus.marathon.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评分记录实体：同一评审对同一参赛者的同一评分维度只能有一条记录（覆盖更新）。
 */
public class Score implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private long contestantId;
    private long judgeId;
    private long criterionId;
    /** 0-10 分 */
    private BigDecimal score;
    private String comment;
    private LocalDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getContestantId() {
        return contestantId;
    }

    public void setContestantId(long contestantId) {
        this.contestantId = contestantId;
    }

    public long getJudgeId() {
        return judgeId;
    }

    public void setJudgeId(long judgeId) {
        this.judgeId = judgeId;
    }

    public long getCriterionId() {
        return criterionId;
    }

    public void setCriterionId(long criterionId) {
        this.criterionId = criterionId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
