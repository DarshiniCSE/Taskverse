package com.taskverse.dao;

import com.taskverse.model.Note;
import com.taskverse.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * NoteDAO Implementation
 * Executes JDBC PreparedStatement queries scoped strictly to the authenticated user.
 */
public class NoteDAOImpl implements NoteDAO {

    @Override
    public Note createNote(Note note, int userId) throws SQLException {
        String sql = "INSERT INTO notes (user_id, title, content, color, is_pinned) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setString(4, note.getColor());
            ps.setBoolean(5, note.isPinned());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating note failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    note.setNoteId(generatedKeys.getInt(1));
                    note.setUserId(userId);
                }
            }
        }

        Note created = getNoteById(note.getNoteId(), userId);
        return created != null ? created : note;
    }

    @Override
    public List<Note> getAllNotes(int userId) throws SQLException {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT note_id, user_id, title, content, color, is_pinned, created_at, updated_at " +
                     "FROM notes WHERE user_id = ? ORDER BY is_pinned DESC, updated_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapRowToNote(rs));
                }
            }
        }
        return notes;
    }

    @Override
    public Note getNoteById(int noteId, int userId) throws SQLException {
        String sql = "SELECT note_id, user_id, title, content, color, is_pinned, created_at, updated_at " +
                     "FROM notes WHERE note_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, noteId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToNote(rs);
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateNote(Note note, int userId) throws SQLException {
        String sql = "UPDATE notes SET title = ?, content = ?, color = ?, is_pinned = ? WHERE note_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setString(3, note.getColor());
            ps.setBoolean(4, note.isPinned());
            ps.setInt(5, note.getNoteId());
            ps.setInt(6, userId);

            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateNotePin(int noteId, boolean isPinned, int userId) throws SQLException {
        String sql = "UPDATE notes SET is_pinned = ? WHERE note_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isPinned);
            ps.setInt(2, noteId);
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateNoteColor(int noteId, String color, int userId) throws SQLException {
        String sql = "UPDATE notes SET color = ? WHERE note_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, color);
            ps.setInt(2, noteId);
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteNote(int noteId, int userId) throws SQLException {
        String sql = "DELETE FROM notes WHERE note_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, noteId);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;
        }
    }

    private Note mapRowToNote(ResultSet rs) throws SQLException {
        Note note = new Note(
            rs.getInt("note_id"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getString("color"),
            rs.getBoolean("is_pinned"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : null,
            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toString() : null
        );
        note.setUserId(rs.getInt("user_id"));
        return note;
    }
}
