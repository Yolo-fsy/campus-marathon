package com.campus.marathon.dao;

import com.campus.marathon.model.Contestant;

import java.sql.SQLException;
import java.util.List;

/**
 * 参赛者数据访问对象。
 */
public interface ContestantDAO {

    /** 新增参赛者，返回生成的主键 */
    long create(Contestant contestant) throws SQLException;

    /** 按主键查询 */
    Contestant findById(long id) throws SQLException;

    /** 查询某竞赛下所有参赛者 */
    List<Contestant> listByContest(long contestId) throws SQLException;

    /** 按 id + contestId 删除（防止串场） */
    boolean delete(long id, long contestId) throws SQLException;

    /** 更新 */
    boolean update(Contestant contestant) throws SQLException;
}
