package com.campus.marathon.dao;

import com.campus.marathon.model.RunnerAccount;

import java.sql.SQLException;

/**
 * 参赛者账号数据访问。
 */
public interface AccountDAO {

    RunnerAccount findByStudentId(String studentId) throws SQLException;

    void create(RunnerAccount account) throws SQLException;
}
