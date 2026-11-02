package com.taskverse.servlet;

import com.taskverse.dao.TaskDAO;
import com.taskverse.dao.TaskDAOImpl;
import com.taskverse.model.TaskStats;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * DashboardServlet
 * Controller providing real-time dashboard summary metrics scoped strictly to the authenticated user.
 */
public class DashboardServlet extends HttpServlet {

    private TaskDAO taskDAO;

    @Override
    public void init() throws ServletException {
        this.taskDAO = new TaskDAOImpl();
    }

    public void setTaskDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");

        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\":false,\"message\":\"Authentication required.\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("USER_ID");

        try {
            TaskStats stats = taskDAO.getTaskStats(userId);
            out.print("{\"success\":true,\"stats\":" + stats.toJSON() + "}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database service temporarily unavailable.\"}");
        }
    }
}
