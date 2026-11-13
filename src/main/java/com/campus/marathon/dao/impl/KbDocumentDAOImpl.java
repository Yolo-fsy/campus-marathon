package com.campus.marathon.dao.impl;

import com.campus.marathon.dao.KbDocumentDAO;
import com.campus.marathon.model.KbDocument;
import com.campus.marathon.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库文档 JDBC 实现。
 */
public class KbDocumentDAOImpl implements KbDocumentDAO {

    private static final String SELECT_BASE =
            "SELECT id, owner_student_id, owner_name, title, file_name, content_type, "
                    + "char_count, chunk_count, created_at, is_admin_visible FROM kb_document ";

    @Override
    public void create(KbDocument document) throws SQLException {
        String sql = "INSERT INTO kb_document (owner_student_id, owner_name, title, file_name, content_type, "
                + "char_count, chunk_count, created_at, is_admin_visible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"})) {
            statement.setString(1, document.getOwnerStudentId());
            statement.setString(2, document.getOwnerName());
            statement.setString(3, document.getTitle());
            statement.setString(4, document.getFileName());
            statement.setString(5, document.getContentType());
            statement.setInt(6, document.getCharCount());
            statement.setInt(7, document.getChunkCount());
            statement.setTimestamp(8, Timestamp.valueOf(
                    document.getCreatedAt() == null ? LocalDateTime.now() : document.getCreatedAt()));
            statement.setInt(9, document.isAdminVisible() ? 1 : 0);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    document.setId(keys.getLong(1));
                }
            }
        }
    }

    @Override
    public List<KbDocument> listByOwner(String ownerStudentId) throws SQLException {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_BASE + "WHERE owner_student_id = ? ORDER BY created_at DESC, id DESC")) {
            statement.setString(1, ownerStudentId);
            try (ResultSet rs = statement.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    @Override
    public List<KbDocument> listAll() throws SQLException {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_BASE + "ORDER BY created_at DESC, id DESC");
             ResultSet rs = statement.executeQuery()) {
            return mapRows(rs);
        }
    }

    @Override
    public KbDocument findById(long id) throws SQLException {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BASE + "WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public boolean delete(long id, String ownerStudentId) throws SQLException {
        String sql = "DELETE FROM kb_document WHERE id = ?"
                + (ownerStudentId == null ? "" : " AND owner_student_id = ?");
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (ownerStudentId != null) {
                statement.setString(2, ownerStudentId);
            }
            return statement.executeUpdate() > 0;
        }
    }

    private List<KbDocument> mapRows(ResultSet rs) throws SQLException {
        List<KbDocument> documents = new ArrayList<>();
        while (rs.next()) {
            documents.add(mapRow(rs));
        }
        return documents;
    }

    private KbDocument mapRow(ResultSet rs) throws SQLException {
        KbDocument document = new KbDocument();
        document.setId(rs.getLong("id"));
        document.setOwnerStudentId(rs.getString("owner_student_id"));
        document.setOwnerName(rs.getString("owner_name"));
        document.setTitle(rs.getString("title"));
        document.setFileName(rs.getString("file_name"));
        document.setContentType(rs.getString("content_type"));
        document.setCharCount(rs.getInt("char_count"));
        document.setChunkCount(rs.getInt("chunk_count"));
        Timestamp created = rs.getTimestamp("created_at");
        document.setCreatedAt(created == null ? null : created.toLocalDateTime());
        document.setAdminVisible(rs.getInt("is_admin_visible") != 0);
        return document;
    }
}
