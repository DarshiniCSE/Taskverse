package com.taskverse.servlet;

import com.taskverse.dao.TaskDAO;
import com.taskverse.dao.TaskDAOImpl;
import com.taskverse.model.TaskStats;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * DashboardServlet
 * Controller providing real-time dashboard summary metrics directly from MySQL records.
 */
public class DashboardServlet extends HttpServlet {

    private TaskDAO taskDAO;

    @Override
    public void init() throws ServletException {
        this.taskDAO = new TaskDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();

        try {
            TaskStats stats = taskDAO.getTaskStats();
            out.print("{\"success\":true,\"stats\":" + stats.toJSON() + "}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
}
