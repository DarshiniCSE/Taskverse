package com.taskverse.dao;

import com.taskverse.model.Note;
import java.sql.SQLException;
import java.util.List;

/**
 * NoteDAO Interface
 * Contract defining database operations for Sticky Notes with per-user scoping.
 */
public interface NoteDAO {

    /**
     * Inserts a new sticky note for the authenticated user.
     * @param note Note object
     * @param userId authenticated user ID
     * @return the Note object with generated noteId, userId, and timestamps
     * @throws SQLException on database error
     */
    Note createNote(Note note, int userId) throws SQLException;

    /**
     * Retrieves all sticky notes belonging to the authenticated user.
     * @param userId authenticated user ID
     * @return List of Note objects
     * @throws SQLException on database error
     */
    List<Note> getAllNotes(int userId) throws SQLException;

    /**
     * Retrieves a single sticky note by its ID, scoped to the authenticated user.
     * @param noteId ID of note to find
     * @param userId authenticated user ID
     * @return Note object or null if not found
     * @throws SQLException on database error
     */
    Note getNoteById(int noteId, int userId) throws SQLException;

    /**
     * Updates an existing note owned by the authenticated user.
     * @param note Note object with updated fields
     * @param userId authenticated user ID
     * @return true if updated, false if note was not found or not owned
     * @throws SQLException on database error
     */
    boolean updateNote(Note note, int userId) throws SQLException;

    /**
     * Toggles or updates the pinned status of a note owned by the authenticated user.
     * @param noteId ID of the note
     * @param isPinned true to pin, false to unpin
     * @param userId authenticated user ID
     * @return true if updated, false otherwise
     * @throws SQLException on database error
     */
    boolean updateNotePin(int noteId, boolean isPinned, int userId) throws SQLException;

    /**
     * Updates the color of a note owned by the authenticated user.
     * @param noteId ID of the note
     * @param color color string
     * @param userId authenticated user ID
     * @return true if updated, false otherwise
     * @throws SQLException on database error
     */
    boolean updateNoteColor(int noteId, String color, int userId) throws SQLException;

    /**
     * Permanently deletes a sticky note owned by the authenticated user.
     * @param noteId ID of the note to delete
     * @param userId authenticated user ID
     * @return true if deleted, false if note was not found or not owned
     * @throws SQLException on database error
     */
    boolean deleteNote(int noteId, int userId) throws SQLException;
}
