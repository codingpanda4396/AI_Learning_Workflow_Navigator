package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.enums.DiagnosisStatus;
import com.pandanav.learning.domain.model.DiagnosisSession;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface DiagnosisSessionRepository {

    DiagnosisSession save(DiagnosisSession session);

    Optional<DiagnosisSession> findById(Long id);

    Optional<DiagnosisSession> findByIdAndUserPk(Long id, Long userPk);

    void updateStatus(Long id, DiagnosisStatus status, OffsetDateTime completedAt);
}
