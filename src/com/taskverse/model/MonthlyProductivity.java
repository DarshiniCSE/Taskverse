package com.taskverse.model;

import java.util.ArrayList;
import java.util.List;

/**
 * MonthlyProductivity Model
 * Encapsulates monthly productivity metrics, daily completion distributions,
 * most productive day analytics, and month-over-month comparisons.
 */
public class MonthlyProductivity {

    public static class DayStat {
        private final int day;
        private final String date;
        private final int count;

        public DayStat(int day, String date, int count) {
            this.day = day;
            this.date = date;
            this.count = count;
        }

        public int getDay() {
            return day;
        }

        public String getDate() {
            return date;
        }

        public int getCount() {
            return count;
        }
    }

    private int year;
    private int month;
    private String monthName;
    private int daysInMonth;
    private int tasksCompleted;
    private int tasksCreated;
    private int tasksDue;
    private double completionRate;
    private List<DayStat> dailyCompletions = new ArrayList<>();
    private int mostProductiveCount;
    private List<String> mostProductiveDays = new ArrayList<>();
    private String mostProductiveSummary;
    private int previousMonthCompleted;
    private int completedDifference;
    private Double percentageChange;
    private String comparisonMessage;

    public MonthlyProductivity() {}

    public MonthlyProductivity(int year, int month, String monthName, int daysInMonth,
                               int tasksCompleted, int tasksCreated, int tasksDue, double completionRate,
                               List<DayStat> dailyCompletions, int mostProductiveCount,
                               List<String> mostProductiveDays, String mostProductiveSummary,
                               int previousMonthCompleted, int completedDifference,
                               Double percentageChange, String comparisonMessage) {
        this.year = year;
        this.month = month;
        this.monthName = monthName;
        this.daysInMonth = daysInMonth;
        this.tasksCompleted = tasksCompleted;
        this.tasksCreated = tasksCreated;
        this.tasksDue = tasksDue;
        this.completionRate = completionRate;
        this.dailyCompletions = dailyCompletions != null ? dailyCompletions : new ArrayList<>();
        this.mostProductiveCount = mostProductiveCount;
        this.mostProductiveDays = mostProductiveDays != null ? mostProductiveDays : new ArrayList<>();
        this.mostProductiveSummary = mostProductiveSummary;
        this.previousMonthCompleted = previousMonthCompleted;
        this.completedDifference = completedDifference;
        this.percentageChange = percentageChange;
        this.comparisonMessage = comparisonMessage;
    }

    // Getters and Setters
    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public int getDaysInMonth() {
        return daysInMonth;
    }

    public void setDaysInMonth(int daysInMonth) {
        this.daysInMonth = daysInMonth;
    }

    public int getTasksCompleted() {
        return tasksCompleted;
    }

    public void setTasksCompleted(int tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }

    public int getTasksCreated() {
        return tasksCreated;
    }

    public void setTasksCreated(int tasksCreated) {
        this.tasksCreated = tasksCreated;
    }

    public int getTasksDue() {
        return tasksDue;
    }

    public void setTasksDue(int tasksDue) {
        this.tasksDue = tasksDue;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public List<DayStat> getDailyCompletions() {
        return dailyCompletions;
    }

    public void setDailyCompletions(List<DayStat> dailyCompletions) {
        this.dailyCompletions = dailyCompletions;
    }

    public int getMostProductiveCount() {
        return mostProductiveCount;
    }

    public void setMostProductiveCount(int mostProductiveCount) {
        this.mostProductiveCount = mostProductiveCount;
    }

    public List<String> getMostProductiveDays() {
        return mostProductiveDays;
    }

    public void setMostProductiveDays(List<String> mostProductiveDays) {
        this.mostProductiveDays = mostProductiveDays;
    }

    public String getMostProductiveSummary() {
        return mostProductiveSummary;
    }

    public void setMostProductiveSummary(String mostProductiveSummary) {
        this.mostProductiveSummary = mostProductiveSummary;
    }

    public int getPreviousMonthCompleted() {
        return previousMonthCompleted;
    }

    public void setPreviousMonthCompleted(int previousMonthCompleted) {
        this.previousMonthCompleted = previousMonthCompleted;
    }

    public int getCompletedDifference() {
        return completedDifference;
    }

    public void setCompletedDifference(int completedDifference) {
        this.completedDifference = completedDifference;
    }

    public Double getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(Double percentageChange) {
        this.percentageChange = percentageChange;
    }

    public String getComparisonMessage() {
        return comparisonMessage;
    }

    public void setComparisonMessage(String comparisonMessage) {
        this.comparisonMessage = comparisonMessage;
    }

    /**
     * Serializes MonthlyProductivity model into clean JSON format.
     */
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"year\":").append(year).append(",");
        sb.append("\"month\":").append(month).append(",");
        sb.append("\"monthName\":\"").append(escapeJson(monthName)).append("\",");
        sb.append("\"daysInMonth\":").append(daysInMonth).append(",");
        sb.append("\"tasksCompleted\":").append(tasksCompleted).append(",");
        sb.append("\"tasksCreated\":").append(tasksCreated).append(",");
        sb.append("\"tasksDue\":").append(tasksDue).append(",");
        sb.append("\"completionRate\":").append(String.format(java.util.Locale.US, "%.1f", completionRate)).append(",");

        // Daily completions array
        sb.append("\"dailyCompletions\":[");
        for (int i = 0; i < dailyCompletions.size(); i++) {
            DayStat ds = dailyCompletions.get(i);
            sb.append("{\"day\":").append(ds.getDay())
              .append(",\"date\":\"").append(escapeJson(ds.getDate()))
              .append("\",\"count\":").append(ds.getCount()).append("}");
            if (i < dailyCompletions.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("],");

        // Most productive day stats
        sb.append("\"mostProductiveCount\":").append(mostProductiveCount).append(",");
        sb.append("\"mostProductiveDays\":[");
        for (int i = 0; i < mostProductiveDays.size(); i++) {
            sb.append("\"").append(escapeJson(mostProductiveDays.get(i))).append("\"");
            if (i < mostProductiveDays.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("],");
        sb.append("\"mostProductiveSummary\":\"").append(escapeJson(mostProductiveSummary)).append("\",");

        // Comparison metrics
        sb.append("\"previousMonthCompleted\":").append(previousMonthCompleted).append(",");
        sb.append("\"completedDifference\":").append(completedDifference).append(",");
        if (percentageChange != null) {
            sb.append("\"percentageChange\":").append(String.format(java.util.Locale.US, "%.1f", percentageChange)).append(",");
        } else {
            sb.append("\"percentageChange\":null,");
        }
        sb.append("\"comparisonMessage\":\"").append(escapeJson(comparisonMessage)).append("\"");

        sb.append("}");
        return sb.toString();
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
