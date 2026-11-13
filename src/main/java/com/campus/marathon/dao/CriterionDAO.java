package com.campus.marathon.dao;

import com.campus.marathon.model.Criterion;

import java.sql.SQLException;
import java.util.List;

/**
 * 评分维度数据访问对象。
 */
public interface CriterionDAO {

    /** 新增维度，返回生成的主键 */
    long create(Criterion criterion) throws SQLException;

    /** 查询某竞赛的所有维度 */
    List<Criterion> listByContest(long contestId) throws SQLException;

    /** 更新权重 */
    boolean updateWeight(long id, long contestId, int weight) throws SQLException;

    /** 更新名称/描述/权重（整体更新） */
    boolean update(Criterion criterion) throws SQLException;

    /** 按 id + contestId 删除 */
    boolean delete(long id, long contestId) throws SQLException;

    /** 按 contestId 删除（切换到评审阶段前清理旧数据用） */
    boolean deleteByContest(long contestId) throws SQLException;
}
