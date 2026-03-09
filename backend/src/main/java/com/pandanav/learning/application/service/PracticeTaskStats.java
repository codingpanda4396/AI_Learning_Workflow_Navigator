package com.pandanav.learning.application.service;

public class PracticeTaskStats {

    private final int totalItems;
    private final int answeredItems;
    private final Double averageScore;
    private final Double correctRate;

    public PracticeTaskStats(int totalItems, int answeredItems, Double averageScore, Double correctRate) {
        this.totalItems = totalItems;
        this.answeredItems = answeredItems;
        this.averageScore = averageScore;
        this.correctRate = correctRate;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getAnsweredItems() {
        return answeredItems;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public Double getCorrectRate() {
        return correctRate;
    }
}
