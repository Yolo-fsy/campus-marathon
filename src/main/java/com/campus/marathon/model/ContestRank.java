package com.campus.marathon.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 竞赛排名 DTO：由 ScoringService 实时计算得出，不持久化。
 */
public class ContestRank implements Serializable {

    private static final long serialVersionUID = 1L;

    private Contestant contestant;
    /** 加权总分（满分 10 × 权重%） */
    private BigDecimal totalScore;
    /** 名次，从 1 开始，同分同名次、跳过后续 */
    private int rank;
    /** 每位评审的平均分（所有评分加总除以评审数） */
    private BigDecimal averagePerJudge;
    /** 实际参与打分的评审数 */
    private int judgeCount;

    public ContestRank(Contestant contestant, BigDecimal totalScore, int rank,
                       BigDecimal averagePerJudge, int judgeCount) {
        this.contestant = contestant;
        this.totalScore = totalScore;
        this.rank = rank;
        this.averagePerJudge = averagePerJudge;
        this.judgeCount = judgeCount;
    }

    public Contestant getContestant() {
        return contestant;
    }

    public void setContestant(Contestant contestant) {
        this.contestant = contestant;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public BigDecimal getAveragePerJudge() {
        return averagePerJudge;
    }

    public void setAveragePerJudge(BigDecimal averagePerJudge) {
        this.averagePerJudge = averagePerJudge;
    }

    public int getJudgeCount() {
        return judgeCount;
    }

    public void setJudgeCount(int judgeCount) {
        this.judgeCount = judgeCount;
    }

    /** 是否为同分并列（下一排名者 totalScore 相同） */
    public boolean tiedWith(ContestRank other) {
        if (other == null || totalScore == null || other.totalScore == null) {
            return false;
        }
        return totalScore.compareTo(other.totalScore) == 0;
    }
}
