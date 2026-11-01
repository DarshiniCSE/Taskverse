package com.taskverse.dao;

import com.taskverse.model.MonthlyProductivity;
import com.taskverse.model.MonthlyProductivity.DayStat;
import com.taskverse.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ProductivityDAO Implementation
 * Executes targeted, indexed aggregation queries in MySQL to calculate
 * monthly productivity overview cards, daily completion chart distribution,
 * most productive day analytics, and month-over-month comparisons.
 */
public class ProductivityDAOImpl implements ProductivityDAO {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @Override
    public MonthlyProductivity getMonthlyProductivity(int year, int month) throws SQLException {
        YearMonth ym = YearMonth.of(year, month);
        int daysInMonth = ym.lengthOfMonth();
        LocalDate startOfMonth = ym.atDay(1);
        LocalDate startOfNextMonth = ym.plusMonths(1).atDay(1);

        YearMonth prevYm = ym.minusMonths(1);
        LocalDate startOfPrevMonth = prevYm.atDay(1);

        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + year;
        String prevMonthShort = prevYm.getMonth().getDisplayName(TextStyle.SHORT, Locale.US);

        Timestamp tsStartOfMonth = Timestamp.valueOf(startOfMonth.atStartOfDay());
        Timestamp tsStartOfNextMonth = Timestamp.valueOf(startOfNextMonth.atStartOfDay());
        Timestamp tsStartOfPrevMonth = Timestamp.valueOf(startOfPrevMonth.atStartOfDay());

        Date dStartOfMonth = Date.valueOf(startOfMonth);
        Date dStartOfNextMonth = Date.valueOf(startOfNextMonth);

        int tasksCompleted = 0;
        int tasksCreated = 0;
        int tasksDue = 0;
        int previousMonthCompleted = 0;
        Map<Integer, Integer> dayCountsMap = new HashMap<>();

        try (Connection conn = DBConnection.getConnection()) {

            // 1. Tasks Completed in selected month
            String completedSql = "SELECT COUNT(*) FROM tasks WHERE completed_at >= ? AND completed_at < ?";
            try (PreparedStatement ps = conn.prepareStatement(completedSql)) {
                ps.setTimestamp(1, tsStartOfMonth);
                ps.setTimestamp(2, tsStartOfNextMonth);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tasksCompleted = rs.getInt(1);
                    }
                }
            }

            // 2. Tasks Created in selected month
            String createdSql = "SELECT COUNT(*) FROM tasks WHERE created_at >= ? AND created_at < ?";
            try (PreparedStatement ps = conn.prepareStatement(createdSql)) {
                ps.setTimestamp(1, tsStartOfMonth);
                ps.setTimestamp(2, tsStartOfNextMonth);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tasksCreated = rs.getInt(1);
                    }
                }
            }

            // 3. Tasks Due in selected month
            String dueSql = "SELECT COUNT(*) FROM tasks WHERE due_date >= ? AND due_date < ?";
            try (PreparedStatement ps = conn.prepareStatement(dueSql)) {
                ps.setDate(1, dStartOfMonth);
                ps.setDate(2, dStartOfNextMonth);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tasksDue = rs.getInt(1);
                    }
                }
            }

            // 4. Daily completion distribution for chart
            String dailySql = "SELECT DAY(completed_at) AS day_num, COUNT(*) AS count " +
                              "FROM tasks " +
                              "WHERE completed_at >= ? AND completed_at < ? " +
                              "GROUP BY DAY(completed_at)";
            try (PreparedStatement ps = conn.prepareStatement(dailySql)) {
                ps.setTimestamp(1, tsStartOfMonth);
                ps.setTimestamp(2, tsStartOfNextMonth);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        dayCountsMap.put(rs.getInt("day_num"), rs.getInt("count"));
                    }
                }
            }

            // 5. Tasks Completed in previous month (for comparison)
            try (PreparedStatement ps = conn.prepareStatement(completedSql)) {
                ps.setTimestamp(1, tsStartOfPrevMonth);
                ps.setTimestamp(2, tsStartOfMonth);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        previousMonthCompleted = rs.getInt(1);
                    }
                }
            }
        }

        // Completion Rate: (completed / created) * 100. If 0 tasks created, 0%
        double completionRate = tasksCreated > 0
                ? ((double) tasksCompleted / tasksCreated) * 100.0
                : 0.0;

        // Populate daily completions for every calendar day in the month
        List<DayStat> dailyCompletions = new ArrayList<>();
        int maxDailyCount = 0;
        List<Integer> peakDays = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            int count = dayCountsMap.getOrDefault(day, 0);
            LocalDate dayDate = ym.atDay(day);
            dailyCompletions.add(new DayStat(day, dayDate.format(DATE_FMT), count));

            if (count > maxDailyCount) {
                maxDailyCount = count;
                peakDays.clear();
                peakDays.add(day);
            } else if (count == maxDailyCount && count > 0) {
                peakDays.add(day);
            }
        }

        // Formulate Most Productive Day summary
        List<String> mostProductiveDaysList = new ArrayList<>();
        String mostProductiveSummary;

        if (maxDailyCount == 0) {
            mostProductiveSummary = "No completed tasks this month";
        } else {
            List<String> formattedPeakDates = new ArrayList<>();
            for (int day : peakDays) {
                LocalDate d = ym.atDay(day);
                mostProductiveDaysList.add(d.format(DATE_FMT));
                formattedPeakDates.add(d.format(DISPLAY_FMT));
            }

            if (peakDays.size() == 1) {
                mostProductiveSummary = formattedPeakDates.get(0) + " (" + maxDailyCount + " task" + (maxDailyCount > 1 ? "s" : "") + " completed)";
            } else {
                mostProductiveSummary = "Tied: " + String.join(", ", formattedPeakDates) + " (" + maxDailyCount + " task" + (maxDailyCount > 1 ? "s" : "") + " each)";
            }
        }

        // Month Comparison Calculation
        int completedDifference = tasksCompleted - previousMonthCompleted;
        Double percentageChange = null;
        String comparisonMessage;

        if (previousMonthCompleted > 0) {
            percentageChange = ((double) completedDifference / previousMonthCompleted) * 100.0;
            if (completedDifference > 0) {
                comparisonMessage = String.format(Locale.US, "+%.1f%% vs last month (%d vs %d)", percentageChange, tasksCompleted, previousMonthCompleted);
            } else if (completedDifference < 0) {
                comparisonMessage = String.format(Locale.US, "%.1f%% vs last month (%d vs %d)", percentageChange, tasksCompleted, previousMonthCompleted);
            } else {
                comparisonMessage = String.format(Locale.US, "Same as last month (%d completed)", tasksCompleted);
            }
        } else {
            // Previous month had 0 completed tasks
            if (tasksCompleted > 0) {
                comparisonMessage = "+" + tasksCompleted + " vs last month (0 in " + prevMonthShort + ")";
            } else {
                comparisonMessage = "No completions (0 in both months)";
            }
        }

        return new MonthlyProductivity(
            year,
            month,
            monthName,
            daysInMonth,
            tasksCompleted,
            tasksCreated,
            tasksDue,
            completionRate,
            dailyCompletions,
            maxDailyCount,
            mostProductiveDaysList,
            mostProductiveSummary,
            previousMonthCompleted,
            completedDifference,
            percentageChange,
            comparisonMessage
        );
    }
}
