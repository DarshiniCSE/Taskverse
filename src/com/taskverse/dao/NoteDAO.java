package com.taskverse.dao;

import com.taskverse.model.Note;
import java.sql.SQLException;
import java.util.List;

/**
 * NoteDAO Interface
 * Contract defining database operations for Sticky Notes.
 * 
 * Demonstrates OOP Concept: Abstraction
 */
public interface NoteDAO {

    /**
     * Inserts a new sticky note into MySQL.
     * @param note Note object containing title, content, color, pinned status
     * @return the Note object with generated noteId and timestamps
     * @throws SQLException on database error
     */
    Note createNote(Note note) throws SQLException;

    /**
     * Retrieves all sticky notes ordered by pinned notes first (is_pinned DESC),
     * then by most recently updated (updated_at DESC).
     * @return List of Note objects
     * @throws SQLException on database error
     */
    List<Note> getAllNotes() throws SQLException;

    /**
     * Retrieves a single sticky note by its ID.
     * @param noteId ID of note to find
     * @return Note object or null if not found
     * @throws SQLException on database error
     */
    Note getNoteById(int noteId) throws SQLException;

    /**
     * Updates an existing note's title, content, color, and pinned status.
     * @param note Note object with updated fields
     * @return true if updated, false if note was not found
     * @throws SQLException on database error
     */
    boolean updateNote(Note note) throws SQLException;

    /**
     * Toggles or updates the pinned status of a note.
     * @param noteId ID of the note
     * @param isPinned true to pin, false to unpin
     * @return true if updated, false otherwise
     * @throws SQLException on database error
     */
    boolean updateNotePin(int noteId, boolean isPinned) throws SQLException;

    /**
     * Updates the color of a note.
     * @param noteId ID of the note
     * @param color one of yellow, pink, blue, green, purple
     * @return true if updated, false otherwise
     * @throws SQLException on database error
     */
    boolean updateNoteColor(int noteId, String color) throws SQLException;

    /**
     * Permanently deletes a sticky note from the database.
     * @param noteId ID of the note to delete
     * @return true if deleted, false if note was not found
     * @throws SQLException on database error
     */
    boolean deleteNote(int noteId) throws SQLException;
}
