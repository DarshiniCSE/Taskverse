package com.taskverse.dao;

import com.taskverse.model.Note;
import com.taskverse.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * NoteDAO Implementation
 * Executes JDBC PreparedStatement queries against MySQL database.
 * 
 * Demonstrates:
 * - Polymorphism: implements NoteDAO interface.
 * - Resource safety: try-with-resources.
 * - SQL Injection prevention: PreparedStatement with bind parameters.
 */
public class NoteDAOImpl implements NoteDAO {

    @Override
    public Note createNote(Note note) throws SQLException {
        String sql = "INSERT INTO notes (title, content, color, is_pinned) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setString(3, note.getColor());
            ps.setBoolean(4, note.isPinned());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating note failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    note.setNoteId(generatedKeys.getInt(1));
                }
            }
        }

        // Fetch back populated timestamps
        Note created = getNoteById(note.getNoteId());
        return created != null ? created : note;
    }

    @Override
    public List<Note> getAllNotes() throws SQLException {
        List<Note> notes = new ArrayList<>();
        // Pinned notes appear first, then by most recently updated
        String sql = "SELECT note_id, title, content, color, is_pinned, created_at, updated_at " +
                     "FROM notes ORDER BY is_pinned DESC, updated_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                notes.add(mapRowToNote(rs));
            }
        }
        return notes;
    }

    @Override
    public Note getNoteById(int noteId) throws SQLException {
        String sql = "SELECT note_id, title, content, color, is_pinned, created_at, updated_at " +
                     "FROM notes WHERE note_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToNote(rs);
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateNote(Note note) throws SQLException {
        String sql = "UPDATE notes SET title = ?, content = ?, color = ?, is_pinned = ? WHERE note_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setString(3, note.getColor());
            ps.setBoolean(4, note.isPinned());
            ps.setInt(5, note.getNoteId());

            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateNotePin(int noteId, boolean isPinned) throws SQLException {
        String sql = "UPDATE notes SET is_pinned = ? WHERE note_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isPinned);
            ps.setInt(2, noteId);

            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateNoteColor(int noteId, String color) throws SQLException {
        String sql = "UPDATE notes SET color = ? WHERE note_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, color != null ? color.toLowerCase().trim() : "yellow");
            ps.setInt(2, noteId);

            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteNote(int noteId) throws SQLException {
        String sql = "DELETE FROM notes WHERE note_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, noteId);
            return ps.executeUpdate() > 0;
        }
    }

    private Note mapRowToNote(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return new Note(
            rs.getInt("note_id"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getString("color"),
            rs.getBoolean("is_pinned"),
            createdAt != null ? createdAt.toString() : null,
            updatedAt != null ? updatedAt.toString() : null
        );
    }
}
