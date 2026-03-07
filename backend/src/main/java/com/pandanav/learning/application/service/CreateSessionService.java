package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.CreateSessionRequest;
import com.pandanav.learning.api.dto.session.CreateSessionResponse;
import com.pandanav.learning.application.usecase.CreateSessionUseCase;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CreateSessionService implements CreateSessionUseCase {

    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;

    public CreateSessionService(SessionRepository sessionRepository, ConceptNodeRepository conceptNodeRepository) {
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
    }

    @Override
    public CreateSessionResponse execute(CreateSessionRequest request) {
        ConceptNode firstNode = conceptNodeRepository.findFirstByChapterIdOrderByOrderNoAsc(request.chapterId())
            .orElseThrow(() -> new NotFoundException("No concept nodes found for chapter."));

        LearningSession session = new LearningSession();
        session.setUserId(request.userId());
        session.setCourseId(request.courseId());
        session.setChapterId(request.chapterId());
        session.setGoalText(request.goalText());
        session.setCurrentStage(Stage.STRUCTURE);
        session.setCurrentNodeId(firstNode.getId());

        try {
            LearningSession saved = sessionRepository.save(session);
            return new CreateSessionResponse(saved.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Session already exists for user and chapter.");
        }
    }
}


