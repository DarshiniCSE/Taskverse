package com.taskverse.servlet;

import com.taskverse.util.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * HealthServlet
 * Diagnostic endpoint to verify system status, JDBC driver, and database connectivity.
 */
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        String dbStatus = DBConnection.testConnection();
        boolean isConnected = dbStatus.startsWith("SUCCESS");

        if (!isConnected) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }

        out.print("{"
            + "\"status\":\"" + (isConnected ? "UP" : "DOWN") + "\","
            + "\"app\":\"TaskVerse\","
            + "\"version\":\"1.0.0\","
            + "\"database\":{"
            + "\"connected\":" + isConnected + ","
            + "\"message\":\"" + dbStatus.replace("\"", "\\\"") + "\""
            + "}"
            + "}");
    }
}
