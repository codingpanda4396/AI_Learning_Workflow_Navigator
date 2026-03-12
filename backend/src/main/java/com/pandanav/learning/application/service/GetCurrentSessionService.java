package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.CurrentSessionInfoResponse;
import com.pandanav.learning.api.dto.session.CurrentSessionResponse;
import com.pandanav.learning.application.usecase.GetCurrentSessionUseCase;
import com.pandanav.learning.domain.repository.SessionRepository;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentSessionService implements GetCurrentSessionUseCase {

    private final SessionRepository sessionRepository;

    public GetCurrentSessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public CurrentSessionResponse execute(Long userId) {
        return sessionRepository.findLatestActiveByUserPk(userId)
            .map(session -> new CurrentSessionResponse(
                true,
                new CurrentSessionInfoResponse(
                    session.getId(),
                    session.getUserPk() == null ? null : String.valueOf(session.getUserPk()),
                    session.getCourseId(),
                    session.getChapterId(),
                    session.getGoalText(),
                    session.getCurrentNodeId(),
                    session.getCurrentStage() == null ? null : session.getCurrentStage().name(),
                    session.getStatus() == null ? null : session.getStatus().name()
                )
            ))
            .orElseGet(() -> new CurrentSessionResponse(false, null));
    }
}
