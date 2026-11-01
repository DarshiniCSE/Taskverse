package com.taskverse.servlet;

import com.taskverse.dao.TaskDAO;
import com.taskverse.dao.TaskDAOImpl;
import com.taskverse.model.PointResult;
import com.taskverse.model.Task;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * TaskServlet
 * Controller handling CRUD operations for Tasks.
 * 
 * Demonstrates:
 * - Jakarta Servlet API (Tomcat 10.1 compliant)
 * - Separation of Concerns: Servlet handles HTTP protocols and JSON serialization,
 *   delegating all database operations to TaskDAO.
 */
public class TaskServlet extends HttpServlet {

    private TaskDAO taskDAO;

    @Override
    public void init() throws ServletException {
        // Initialize DAO implementation through its interface
        this.taskDAO = new TaskDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setupResponseHeaders(response);
        PrintWriter out = response.getWriter();

        String idParam = request.getParameter("id");

        try {
            if (idParam != null && !idParam.trim().isEmpty()) {
                // Get single task by ID
                int taskId = Integer.parseInt(idParam.trim());
                Task task = taskDAO.getTaskById(taskId);
                if (task != null) {
                    out.print("{\"success\":true,\"task\":" + task.toJSON() + "}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Task not found with ID: " + taskId + "\"}");
                }
            } else {
                // Get all tasks
                List<Task> tasks = taskDAO.getAllTasks();
                StringBuilder json = new StringBuilder();
                json.append("{\"success\":true,\"count\":").append(tasks.size()).append(",\"tasks\":[");
                for (int i = 0; i < tasks.size(); i++) {
                    json.append(tasks.get(i).toJSON());
                    if (i < tasks.size() - 1) {
                        json.append(",");
                    }
                }
                json.append("]}");
                out.print(json.toString());
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Invalid task ID format.\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setupResponseHeaders(response);
        PrintWriter out = response.getWriter();

        // Check if this is an action override (e.g. action=delete, action=status, action=update)
        String action = request.getParameter("action");
        if (action == null) {
            action = "create";
        }

        try {
            switch (action.toLowerCase()) {
                case "delete":
                    handleDelete(request, response, out);
                    break;
                case "status":
                    handleStatusUpdate(request, response, out);
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
            out.print("{\"success\":false,\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Request error: " + escapeJson(e.getMessage()) + "\"}");
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
            out.print("{\"success\":false,\"message\":\"Update failed: " + escapeJson(e.getMessage()) + "\"}");
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
            out.print("{\"success\":false,\"message\":\"Delete failed: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws SQLException, IOException {
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priority = request.getParameter("priority");
        String status = request.getParameter("status");
        String category = request.getParameter("category");
        String dueDate = request.getParameter("dueDate");

        // Server-side validation
        if (title == null || title.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Task title is required.\"}");
            return;
        }

        Task newTask = new Task(title.trim(), description, priority, status, category, dueDate);
        Task created = taskDAO.createTask(newTask);
        int totalPoints = taskDAO.getTotalPoints();

        response.setStatus(HttpServletResponse.SC_CREATED);
        out.print("{\"success\":true,\"message\":\"Task created successfully.\",\"task\":" + created.toJSON() + ",\"pointsAwarded\":0,\"totalPoints\":" + totalPoints + "}");
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws SQLException, IOException {
        String idParam = request.getParameter("taskId");
        if (idParam == null || idParam.trim().isEmpty()) {
            idParam = request.getParameter("id");
        }

        if (idParam == null || idParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Task ID is required for update.\"}");
            return;
        }

        int taskId = Integer.parseInt(idParam.trim());
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priority = request.getParameter("priority");
        String status = request.getParameter("status");
        String category = request.getParameter("category");
        String dueDate = request.getParameter("dueDate");

        if (title == null || title.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Task title cannot be empty.\"}");
            return;
        }

        Task task = new Task(taskId, title.trim(), description, priority, status, category, dueDate, null, null);
        PointResult result = taskDAO.updateTaskWithPoints(task);

        if (result.isSuccess()) {
            out.print(result.toJSON());
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"success\":false,\"message\":\"" + escapeJson(result.getMessage()) + "\"}");
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws SQLException, IOException {
        String idParam = request.getParameter("taskId");
        if (idParam == null || idParam.trim().isEmpty()) {
            idParam = request.getParameter("id");
        }

        if (idParam == null || idParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Task ID is required for deletion.\"}");
            return;
        }

        int taskId = Integer.parseInt(idParam.trim());
        boolean deleted = taskDAO.deleteTask(taskId);
        int totalPoints = taskDAO.getTotalPoints();

        if (deleted) {
            out.print("{\"success\":true,\"message\":\"Task deleted successfully.\",\"pointsAwarded\":0,\"totalPoints\":" + totalPoints + "}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"success\":false,\"message\":\"Task not found with ID: " + taskId + "\"}");
        }
    }

    private void handleStatusUpdate(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws SQLException, IOException {
        String idParam = request.getParameter("taskId");
        if (idParam == null || idParam.trim().isEmpty()) {
            idParam = request.getParameter("id");
        }
        String status = request.getParameter("status");

        if (idParam == null || status == null || status.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Task ID and new status are required.\"}");
            return;
        }

        int taskId = Integer.parseInt(idParam.trim());
        PointResult result = taskDAO.updateTaskStatusWithPoints(taskId, status.trim());

        if (result.isSuccess()) {
            out.print(result.toJSON());
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"success\":false,\"message\":\"" + escapeJson(result.getMessage()) + "\"}");
        }
    }

    private void setupResponseHeaders(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
