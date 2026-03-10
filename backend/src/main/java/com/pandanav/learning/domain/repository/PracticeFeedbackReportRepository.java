package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.PracticeFeedbackReport;

import java.util.Optional;

public interface PracticeFeedbackReportRepository {

    PracticeFeedbackReport save(PracticeFeedbackReport report);

    Optional<PracticeFeedbackReport> findByQuizId(Long quizId);
}
