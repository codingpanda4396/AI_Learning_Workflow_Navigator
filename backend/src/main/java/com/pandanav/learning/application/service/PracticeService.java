package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeSubmission;

import java.util.List;

public interface PracticeService {

    PracticeItem createPracticeItem(PracticeItem item);

    List<PracticeItem> listPracticeItems(Long sessionId, Long taskId);

    PracticeSubmission submitPractice(Long practiceItemId, PracticeSubmission submission);

    PracticeTaskStats summarizeTask(Long sessionId, Long taskId);
}
