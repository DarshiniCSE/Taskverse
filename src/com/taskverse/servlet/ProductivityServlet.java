package com.taskverse.servlet;

import com.taskverse.dao.ProductivityDAO;
import com.taskverse.dao.ProductivityDAOImpl;
import com.taskverse.model.MonthlyProductivity;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * ProductivityServlet
 * Controller providing real-time Monthly Productivity Tracker analytics scoped strictly to the authenticated user.
 */
public class ProductivityServlet extends HttpServlet {

    private ProductivityDAO productivityDAO;

    @Override
    public void init() throws ServletException {
        this.productivityDAO = new ProductivityDAOImpl();
    }

    public void setProductivityDAO(ProductivityDAO productivityDAO) {
        this.productivityDAO = productivityDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setupResponseHeaders(response);
        PrintWriter out = response.getWriter();

        int userId = getAuthenticatedUserId(request);

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
            out.print("{\"success\":false,\"message\":\"Invalid calendar range.\"}");
            return;
        }

        try {
            MonthlyProductivity productivity = productivityDAO.getMonthlyProductivity(year, month, userId);
            out.print("{\"success\":true,\"productivity\":" + productivity.toJSON() + "}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database service temporarily unavailable.\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Analytics request could not be processed.\"}");
        }
    }

    private int getAuthenticatedUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("USER_ID") != null) {
            return (Integer) session.getAttribute("USER_ID");
        }
        throw new IllegalStateException("Unauthenticated request reached ProductivityServlet.");
    }

    private void setupResponseHeaders(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
