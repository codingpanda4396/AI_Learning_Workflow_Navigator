package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeFeedbackReport;
import com.pandanav.learning.domain.model.PracticeQuiz;
import com.pandanav.learning.domain.model.PracticeSubmission;
import java.util.List;

public interface PracticeService {

    PracticeQuiz requestQuizGeneration(Long sessionId, Long taskId, Long userId);

    PracticeQuiz getQuiz(Long sessionId, Long taskId, Long userId);

    PracticeFeedbackReport getFeedbackReport(Long sessionId, Long taskId, Long userId);

    PracticeQuiz applyFeedbackAction(Long sessionId, Long taskId, Long userId, String action);

    List<PracticeItem> listPracticeItems(Long sessionId, Long taskId, Long userId);

    List<PracticeItem> generatePracticeItems(Long sessionId, Long taskId, Long userId);

    List<PracticeItem> getOrCreatePracticeItems(Long sessionId, Long taskId, Long userId);

    PracticeSubmission submitPracticeAnswer(Long sessionId, Long taskId, Long practiceItemId, Long userId, String userAnswer);

    List<PracticeSubmission> listPracticeSubmissions(Long sessionId, Long taskId, Long userId);
}
