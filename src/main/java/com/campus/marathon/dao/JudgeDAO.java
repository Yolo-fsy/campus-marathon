package com.campus.marathon.dao;

import com.campus.marathon.model.Judge;

import java.sql.SQLException;
import java.util.List;

/**
 * 评审账号数据访问对象。
 */
public interface JudgeDAO {

    /** 新增评审，返回生成的主键 */
    long create(Judge judge) throws SQLException;

    /** 按用户名查询（同 username 可能在不同 contestId 各有一条） */
    Judge findByUsername(String username) throws SQLException;

    /** 按 username + contestId 查询（精确到某竞赛） */
    Judge findByUsernameAndContest(String username, long contestId) throws SQLException;

    /** 列出某竞赛的所有评审 */
    List<Judge> listByContest(long contestId) throws SQLException;

    /** 添加评审（contestId + username + displayName + 密码） */
    long addJudge(long contestId, String username, String displayName, String credential, String salt)
            throws SQLException;

    /** 移除评审（contestId + judgeId） */
    boolean removeJudge(long contestId, long judgeId) throws SQLException;

    /** 更新 credential */
    boolean updateCredential(long id, String credential, String salt) throws SQLException;
}
