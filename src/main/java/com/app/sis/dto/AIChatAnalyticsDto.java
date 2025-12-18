package com.app.sis.dto;

import java.util.List;

public class AIChatAnalyticsDto {
    private int totalQuestions;
    private int totalUsers;
    private double avgResponseTime;
    private double totalCost;
    private int percentChange;
    private List<DailyChartData> dailyChats;
    private List<TopQuestion> topQuestions;
    private List<UnansweredQuestion> unansweredQuestions;

    public AIChatAnalyticsDto() {
    }

    public AIChatAnalyticsDto(int totalQuestions, int totalUsers, double avgResponseTime, 
                             double totalCost, int percentChange, List<DailyChartData> dailyChats,
                             List<TopQuestion> topQuestions, List<UnansweredQuestion> unansweredQuestions) {
        this.totalQuestions = totalQuestions;
        this.totalUsers = totalUsers;
        this.avgResponseTime = avgResponseTime;
        this.totalCost = totalCost;
        this.percentChange = percentChange;
        this.dailyChats = dailyChats;
        this.topQuestions = topQuestions;
        this.unansweredQuestions = unansweredQuestions;
    }

    // Getters and Setters
    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public double getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public int getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(int percentChange) {
        this.percentChange = percentChange;
    }

    public List<DailyChartData> getDailyChats() {
        return dailyChats;
    }

    public void setDailyChats(List<DailyChartData> dailyChats) {
        this.dailyChats = dailyChats;
    }

    public List<TopQuestion> getTopQuestions() {
        return topQuestions;
    }

    public void setTopQuestions(List<TopQuestion> topQuestions) {
        this.topQuestions = topQuestions;
    }

    public List<UnansweredQuestion> getUnansweredQuestions() {
        return unansweredQuestions;
    }

    public void setUnansweredQuestions(List<UnansweredQuestion> unansweredQuestions) {
        this.unansweredQuestions = unansweredQuestions;
    }

    // Nested classes
    public static class DailyChartData {
        private String date;
        private int count;

        public DailyChartData() {
        }

        public DailyChartData(String date, int count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public static class TopQuestion {
        private String question;
        private int count;
        private double satisfactionRate;

        public TopQuestion() {
        }

        public TopQuestion(String question, int count, double satisfactionRate) {
            this.question = question;
            this.count = count;
            this.satisfactionRate = satisfactionRate;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public double getSatisfactionRate() {
            return satisfactionRate;
        }

        public void setSatisfactionRate(double satisfactionRate) {
            this.satisfactionRate = satisfactionRate;
        }
    }

    public static class UnansweredQuestion {
        private String question;
        private int attempts;

        public UnansweredQuestion() {
        }

        public UnansweredQuestion(String question, int attempts) {
            this.question = question;
            this.attempts = attempts;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }
    }
}
