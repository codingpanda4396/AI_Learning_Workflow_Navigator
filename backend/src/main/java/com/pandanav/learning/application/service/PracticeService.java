package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeSubmission;
import java.util.List;

public interface PracticeService {

    List<PracticeItem> listPracticeItems(Long sessionId, Long taskId, Long userId);

    List<PracticeItem> generatePracticeItems(Long sessionId, Long taskId, Long userId);

    List<PracticeItem> getOrCreatePracticeItems(Long sessionId, Long taskId, Long userId);

    PracticeSubmission submitPracticeAnswer(Long sessionId, Long taskId, Long practiceItemId, Long userId, String userAnswer);

    List<PracticeSubmission> listPracticeSubmissions(Long sessionId, Long taskId, Long userId);
}
