package com.taskverse.servlet;

import com.taskverse.dao.NoteDAO;
import com.taskverse.dao.NoteDAOImpl;
import com.taskverse.model.Note;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * NoteServlet
 * Controller handling REST API operations for Sticky Notes with strict per-user authorization.
 */
public class NoteServlet extends HttpServlet {

    private NoteDAO noteDAO;

    @Override
    public void init() throws ServletException {
        this.noteDAO = new NoteDAOImpl();
    }

    public void setNoteDAO(NoteDAO noteDAO) {
        this.noteDAO = noteDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setupResponseHeaders(response);
        PrintWriter out = response.getWriter();

        int userId = getAuthenticatedUserId(request);
        String idParam = request.getParameter("id");
        if (idParam == null) idParam = request.getParameter("noteId");

        try {
            if (idParam != null && !idParam.trim().isEmpty()) {
                int noteId = Integer.parseInt(idParam.trim());
                Note note = noteDAO.getNoteById(noteId, userId);
                if (note != null) {
                    out.print("{\"success\":true,\"note\":" + note.toJSON() + "}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Note not found with ID: " + noteId + "\"}");
                }
            } else {
                List<Note> notes = noteDAO.getAllNotes(userId);
                StringBuilder json = new StringBuilder();
                json.append("{\"success\":true,\"count\":").append(notes.size()).append(",\"notes\":[");
                for (int i = 0; i < notes.size(); i++) {
                    json.append(notes.get(i).toJSON());
                    if (i < notes.size() - 1) {
                        json.append(",");
                    }
                }
                json.append("]}");
                out.print(json.toString());
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Invalid note ID format.\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database service temporarily unavailable.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setupResponseHeaders(response);
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "create";
        }

        try {
            switch (action.toLowerCase().trim()) {
                case "delete":
                    handleDelete(request, response, out);
                    break;
                case "pin":
                    handlePin(request, response, out);
                    break;
                case "color":
                    handleColor(request, response, out);
                    break;
                case "update":
                    handleUpdate(request, response, out);
                    break;
                case "create":
                default:
                    handleCreate(request, response, out);
                    break;
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database operation failed.\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Invalid request format.\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setupResponseHeaders(response);
        PrintWriter out = response.getWriter();
        try {
            handleUpdate(request, response, out);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Update request could not be processed.\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setupResponseHeaders(response);
        PrintWriter out = response.getWriter();
        try {
            handleDelete(request, response, out);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Delete request could not be processed.\"}");
        }
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws SQLException {
        int userId = getAuthenticatedUserId(request);
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String color = request.getParameter("color");
        String isPinnedParam = request.getParameter("isPinned");

        if (title == null || title.trim().isEmpty()) {
            title = "New Note";
        }
        if (content == null) {
            content = "";
        }
        if (color == null || color.trim().isEmpty()) {
            color = "yellow";
        }
        boolean isPinned = Boolean.parseBoolean(isPinnedParam);

        Note newNote = new Note(title, content, color, isPinned);
        Note created = noteDAO.createNote(newNote, userId);

        response.setStatus(HttpServletResponse.SC_CREATED);
        out.print("{\"success\":true,\"message\":\"Sticky note created.\",\"note\":" + created.toJSON() + "}");
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws SQLException {
        int userId = getAuthenticatedUserId(request);
        String idParam = request.getParameter("noteId");
        if (idParam == null) idParam = request.getParameter("id");

        if (idParam == null || idParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Note ID is required for update.\"}");
            return;
        }

        int noteId = Integer.parseInt(idParam.trim());
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String color = request.getParameter("color");
        String isPinnedParam = request.getParameter("isPinned");

        Note existing = noteDAO.getNoteById(noteId, userId);
        if (existing == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"success\":false,\"message\":\"Note not found with ID: " + noteId + "\"}");
            return;
        }

        if (title != null) existing.setTitle(title.trim().isEmpty() ? "Untitled Note" : title.trim());
        if (content != null) existing.setContent(content);
        if (color != null && !color.trim().isEmpty()) existing.setColor(color.trim().toLowerCase());
        if (isPinnedParam != null) existing.setPinned(Boolean.parseBoolean(isPinnedParam));

        boolean updated = noteDAO.updateNote(existing, userId);
        if (updated) {
            Note fresh = noteDAO.getNoteById(noteId, userId);
            out.print("{\"success\":true,\"message\":\"Note saved.\",\"note\":" + (fresh != null ? fresh.toJSON() : existing.toJSON()) + "}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Could not update note.\"}");
        }
    }

    private void handlePin(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws SQLException {
        int userId = getAuthenticatedUserId(request);
        String idParam = request.getParameter("noteId");
        if (idParam == null) idParam = request.getParameter("id");
        String isPinnedParam = request.getParameter("isPinned");

        if (idParam == null || idParam.trim().isEmpty() || isPinnedParam == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"noteId and isPinned parameters required.\"}");
            return;
        }

        int noteId = Integer.parseInt(idParam.trim());
        boolean isPinned = Boolean.parseBoolean(isPinnedParam.trim());

        boolean updated = noteDAO.updateNotePin(noteId, isPinned, userId);
        if (updated) {
            out.print("{\"success\":true,\"message\":\"Note " + (isPinned ? "pinned" : "unpinned") + ".\",\"isPinned\":" + isPinned + "}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"success\":false,\"message\":\"Note not found.\"}");
        }
    }

    private void handleColor(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws SQLException {
        int userId = getAuthenticatedUserId(request);
        String idParam = request.getParameter("noteId");
        if (idParam == null) idParam = request.getParameter("id");
        String color = request.getParameter("color");

        if (idParam == null || color == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"noteId and color parameters required.\"}");
            return;
        }

        int noteId = Integer.parseInt(idParam.trim());
        boolean updated = noteDAO.updateNoteColor(noteId, color.trim(), userId);

        if (updated) {
            out.print("{\"success\":true,\"message\":\"Color updated.\",\"color\":\"" + escapeJson(color) + "\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"success\":false,\"message\":\"Note not found.\"}");
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws SQLException {
        int userId = getAuthenticatedUserId(request);
        String idParam = request.getParameter("noteId");
        if (idParam == null) idParam = request.getParameter("id");

        if (idParam == null || idParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Note ID is required for deletion.\"}");
            return;
        }

        int noteId = Integer.parseInt(idParam.trim());
        boolean deleted = noteDAO.deleteNote(noteId, userId);

        if (deleted) {
            out.print("{\"success\":true,\"message\":\"Note deleted successfully.\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"success\":false,\"message\":\"Note not found with ID: " + noteId + "\"}");
        }
    }

    private int getAuthenticatedUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("USER_ID") != null) {
            return (Integer) session.getAttribute("USER_ID");
        }
        throw new IllegalStateException("Unauthenticated request reached NoteServlet.");
    }

    private void setupResponseHeaders(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
