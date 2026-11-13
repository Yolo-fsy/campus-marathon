package com.campus.marathon.dao;

import com.campus.marathon.model.KbDocument;

import java.sql.SQLException;
import java.util.List;

/**
 * 知识库文档 DAO。
 */
public interface KbDocumentDAO {

    void create(KbDocument document) throws SQLException;

    List<KbDocument> listByOwner(String ownerStudentId) throws SQLException;

    List<KbDocument> listAll() throws SQLException;

    KbDocument findById(long id) throws SQLException;

    /**
     * 删除文档。ownerStudentId 为 null 表示管理员无条件删除，否则只能删自己的。
     */
    boolean delete(long id, String ownerStudentId) throws SQLException;
}
