package com.taskverse.servlet;

import com.taskverse.dao.ProductivityDAO;
import com.taskverse.dao.ProductivityDAOImpl;
import com.taskverse.model.MonthlyProductivity;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * ProductivityServlet
 * Controller providing real-time Monthly Productivity Tracker analytics directly from MySQL.
 * 
 * Endpoints:
 * - GET /api/productivity (defaults to current month and year)
 * - GET /api/productivity?year=2026&month=10
 */
public class ProductivityServlet extends HttpServlet {

    private ProductivityDAO productivityDAO;

    @Override
    public void init() throws ServletException {
        this.productivityDAO = new ProductivityDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setupResponseHeaders(response);
        PrintWriter out = response.getWriter();

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        String yearParam = request.getParameter("year");
        String monthParam = request.getParameter("month");

        if (yearParam != null && !yearParam.trim().isEmpty()) {
            try {
                year = Integer.parseInt(yearParam.trim());
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Invalid year parameter format.\"}");
                return;
            }
        }

        if (monthParam != null && !monthParam.trim().isEmpty()) {
            try {
                month = Integer.parseInt(monthParam.trim());
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Invalid month parameter format.\"}");
                return;
            }
        }

        // Validate range
        if (year < 1970 || year > 2100 || month < 1 || month > 12) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Invalid calendar range. Year must be between 1970 and 2100, month between 1 and 12.\"}");
            return;
        }

        try {
            MonthlyProductivity productivity = productivityDAO.getMonthlyProductivity(year, month);
            out.print("{\"success\":true,\"productivity\":" + productivity.toJSON() + "}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Server error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void setupResponseHeaders(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
