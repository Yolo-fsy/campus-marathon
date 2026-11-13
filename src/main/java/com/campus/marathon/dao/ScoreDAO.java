package com.campus.marathon.dao;

import com.campus.marathon.model.Score;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * 评分数据访问对象。
 */
public interface ScoreDAO {

    /**
     * 提交评分（同一 judge + contestant + criterion 唯一，覆盖更新）。
     * MySQL 用 INSERT ... ON DUPLICATE KEY UPDATE；H2/其他 DB 不支持时回退为先查再删再插。
     */
    void submit(long judgeId, long contestantId, long criterionId, BigDecimal score, String comment)
            throws SQLException;

    /** 查询某参赛者的所有评分 */
    List<Score> listByContestant(long contestantId) throws SQLException;

    /** 查询某竞赛的所有评分（用于排名计算） */
    List<Score> listByContest(long contestId) throws SQLException;

    /** 查询某评审对某参赛者的评分（表单回显用） */
    List<Score> listByJudgeAndContestant(long judgeId, long contestantId) throws SQLException;

    /** 删除某参赛者的所有评分 */
    boolean deleteByContestant(long contestantId) throws SQLException;
}
