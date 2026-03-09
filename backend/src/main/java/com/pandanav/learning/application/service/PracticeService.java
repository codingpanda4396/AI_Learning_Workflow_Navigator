package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.PracticeItem;
import java.util.List;

public interface PracticeService {

    List<PracticeItem> listPracticeItems(Long sessionId, Long taskId, Long userId);

    List<PracticeItem> generatePracticeItems(Long sessionId, Long taskId, Long userId);

    List<PracticeItem> getOrCreatePracticeItems(Long sessionId, Long taskId, Long userId);
}
