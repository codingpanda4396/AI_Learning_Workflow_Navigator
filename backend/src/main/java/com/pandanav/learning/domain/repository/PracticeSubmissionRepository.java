package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.PracticeSubmission;

import java.util.List;
import java.util.Optional;

public interface PracticeSubmissionRepository {

    PracticeSubmission save(PracticeSubmission submission);

    List<PracticeSubmission> findByPracticeItemId(Long practiceItemId);

    List<PracticeSubmission> findBySessionIdAndTaskId(Long sessionId, Long taskId);

    List<PracticeSubmission> findBySessionIdAndTaskIdAndUserPk(Long sessionId, Long taskId, Long userPk);

    List<PracticeSubmission> findBySessionIdAndUserPk(Long sessionId, Long userPk);

    Optional<PracticeSubmission> findLatestByPracticeItemIdAndUserPk(Long practiceItemId, Long userPk);
}
