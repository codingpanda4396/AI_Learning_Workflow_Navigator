package com.pandanav.learning.application.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PracticeQuizAsyncServiceImpl implements PracticeQuizAsyncService {

    private final PracticeServiceImpl practiceService;

    public PracticeQuizAsyncServiceImpl(@Lazy PracticeServiceImpl practiceService) {
        this.practiceService = practiceService;
    }

    @Override
    @Async("practiceQuizExecutor")
    public void generateQuizAsync(Long quizId, Long sessionId, Long taskId, Long userId) {
        practiceService.generateQuizInternal(quizId, sessionId, taskId, userId);
    }
}
