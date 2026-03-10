package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.enums.PracticeQuizStatus;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.PracticeQuiz;

import java.util.Optional;

public interface PracticeQuizRepository {

    PracticeQuiz save(PracticeQuiz quiz);

    Optional<PracticeQuiz> findById(Long id);

    Optional<PracticeQuiz> findLatestBySessionIdAndTaskIdAndUserPk(Long sessionId, Long taskId, Long userPk);

    void updateStatus(Long quizId, PracticeQuizStatus status, String failureReason);

    void updateGenerationState(Long quizId, TaskStatus status, String failureReason, String lastErrorCode);

    void markGenerated(Long quizId, Integer questionCount, String generationSource, String promptVersion);

    void updateAnsweredCount(Long quizId, Integer answeredCount);
}
