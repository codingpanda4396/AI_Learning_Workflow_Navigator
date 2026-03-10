package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.enums.PracticeItemStatus;

import java.util.List;
import java.util.Optional;

public interface PracticeRepository {

    PracticeItem save(PracticeItem item);

    Optional<PracticeItem> findById(Long id);

    Optional<PracticeItem> findByIdAndUserPk(Long id, Long userPk);

    List<PracticeItem> findBySessionIdAndTaskId(Long sessionId, Long taskId);

    List<PracticeItem> findBySessionIdAndTaskIdAndUserPk(Long sessionId, Long taskId, Long userPk);

    List<PracticeItem> findByQuizId(Long quizId);

    void updateStatus(Long id, PracticeItemStatus status);
}
