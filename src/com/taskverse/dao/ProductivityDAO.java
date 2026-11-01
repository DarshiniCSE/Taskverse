package com.taskverse.dao;

import com.taskverse.model.MonthlyProductivity;
import java.sql.SQLException;

/**
 * Data Access Object Interface for Monthly Productivity Metrics
 */
public interface ProductivityDAO {

    /**
     * Calculates monthly productivity overview, daily completion distribution,
     * most productive day, and month-over-month comparison for the specified year and month.
     * 
     * @param year 4-digit calendar year (e.g. 2026)
     * @param month 1-indexed month (1 = January, 12 = December)
     * @return MonthlyProductivity model with calculated database metrics
     * @throws SQLException if a database query fails
     */
    MonthlyProductivity getMonthlyProductivity(int year, int month) throws SQLException;
}
