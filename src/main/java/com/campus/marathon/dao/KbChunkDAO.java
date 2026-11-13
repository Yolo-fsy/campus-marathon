package com.campus.marathon.dao;

import com.campus.marathon.model.KbChunk;

import java.sql.SQLException;
import java.util.List;

/**
 * 知识库切片 DAO。
 */
public interface KbChunkDAO {

    void batchInsert(List<KbChunk> chunks) throws SQLException;

    List<KbChunk> listByDocIds(List<Long> docIds) throws SQLException;

    void deleteByDoc(long docId) throws SQLException;
}
