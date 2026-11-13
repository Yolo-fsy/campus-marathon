package com.campus.marathon.dao;

import com.campus.marathon.model.Contest;

import java.sql.SQLException;
import java.util.List;

/**
 * 竞赛数据访问对象。
 */
public interface ContestDAO {

    /** 新增竞赛，返回生成的主键 */
    long create(Contest contest) throws SQLException;

    /** 按主键查询，不存在返回 null */
    Contest findById(long id) throws SQLException;

    /** 查询全部竞赛 */
    List<Contest> listAll() throws SQLException;

    /** 按状态筛选 */
    List<Contest> listByStatus(String status) throws SQLException;

    /** 更新竞赛状态 */
    boolean updateStatus(long id, String status) throws SQLException;

    /** 更新基本信息 */
    boolean update(Contest contest) throws SQLException;

    /** 按主键删除 */
    boolean delete(long id) throws SQLException;
}
