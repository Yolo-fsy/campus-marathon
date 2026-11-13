package com.campus.marathon.dao.impl;

import com.campus.marathon.dao.KbChunkDAO;
import com.campus.marathon.model.KbChunk;
import com.campus.marathon.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库切片 JDBC 实现。
 */
public class KbChunkDAOImpl implements KbChunkDAO {

    @Override
    public void batchInsert(List<KbChunk> chunks) throws SQLException {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO kb_chunk (doc_id, seq, content, char_count) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (KbChunk chunk : chunks) {
                statement.setLong(1, chunk.getDocId());
                statement.setInt(2, chunk.getSeq());
                statement.setString(3, chunk.getContent());
                statement.setInt(4, chunk.getCharCount());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    @Override
    public List<KbChunk> listByDocIds(List<Long> docIds) throws SQLException {
        List<KbChunk> chunks = new ArrayList<>();
        if (docIds == null || docIds.isEmpty()) {
            return chunks;
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < docIds.size(); i++) {
            if (i > 0) {
                placeholders.append(',');
            }
            placeholders.append('?');
        }
        String sql = "SELECT c.id, c.doc_id, c.seq, c.content, c.char_count, d.title AS doc_title "
                + "FROM kb_chunk c JOIN kb_document d ON c.doc_id = d.id "
                + "WHERE c.doc_id IN (" + placeholders + ") ORDER BY c.doc_id, c.seq";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < docIds.size(); i++) {
                statement.setLong(i + 1, docIds.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    KbChunk chunk = new KbChunk();
                    chunk.setId(rs.getLong("id"));
                    chunk.setDocId(rs.getLong("doc_id"));
                    chunk.setSeq(rs.getInt("seq"));
                    chunk.setContent(rs.getString("content"));
                    chunk.setCharCount(rs.getInt("char_count"));
                    chunk.setDocTitle(rs.getString("doc_title"));
                    chunks.add(chunk);
                }
            }
        }
        return chunks;
    }

    @Override
    public void deleteByDoc(long docId) throws SQLException {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM kb_chunk WHERE doc_id = ?")) {
            statement.setLong(1, docId);
            statement.executeUpdate();
        }
    }
}
