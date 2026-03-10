package com.pandanav.learning.application.service;

public interface PracticeQuizAsyncService {

    void generateQuizAsync(Long quizId, Long sessionId, Long taskId, Long userId);
}
