package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.CreateSessionRequest;
import com.pandanav.learning.api.dto.session.CreateSessionResponse;
import com.pandanav.learning.application.usecase.CreateSessionUseCase;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningEvent;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.enums.SessionStatus;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CreateSessionService implements CreateSessionUseCase {

    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final LearningEventRepository learningEventRepository;

    public CreateSessionService(
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        LearningEventRepository learningEventRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.learningEventRepository = learningEventRepository;
    }

    @Override
    public CreateSessionResponse execute(CreateSessionRequest request, Long userId) {
        String normalizedCourseId = normalizeRequired(request.courseId(), "course_id");
        String normalizedChapterId = normalizeRequired(request.chapterId(), "chapter_id");
        String normalizedGoal = normalizeGoal(request.goalText());

        ConceptNode firstNode = conceptNodeRepository.findFirstByChapterIdOrderByOrderNoAsc(normalizedChapterId)
            .orElseGet(() -> bootstrapConceptNodes(normalizedChapterId, normalizedCourseId, normalizedGoal).get(0));

        LearningSession session = new LearningSession();
        session.setUserId("user-" + userId);
        session.setUserPk(userId);
        session.setCourseId(normalizedCourseId);
        session.setChapterId(normalizedChapterId);
        session.setGoalText(normalizedGoal);
        session.setCurrentStage(Stage.STRUCTURE);
        session.setCurrentNodeId(firstNode.getId());
        session.setStatus(SessionStatus.ANALYZING);

        try {
            LearningSession saved = sessionRepository.save(session);
            LearningEvent event = new LearningEvent();
            event.setSessionId(saved.getId());
            event.setUserId(userId);
            event.setEventType("SESSION_CREATED");
            event.setEventData("{}");
            learningEventRepository.save(event);
            return new CreateSessionResponse(saved.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Session already exists for user and chapter.");
        }
    }

    private List<ConceptNode> bootstrapConceptNodes(String chapterId, String courseId, String goalText) {
        List<ConceptNode> nodes = new ArrayList<>();
        nodes.add(saveNode(chapterId, "Foundation of " + chapterId, 10,
            "Course=" + courseId + "; Goal=" + goalText + "; focus=core definitions and boundaries."));
        nodes.add(saveNode(chapterId, "Mechanism of " + chapterId, 20,
            "Course=" + courseId + "; Goal=" + goalText + "; focus=causal chain and key steps."));
        nodes.add(saveNode(chapterId, "Practice of " + chapterId, 30,
            "Course=" + courseId + "; Goal=" + goalText + "; focus=application, variation, and common errors."));
        return nodes;
    }

    private ConceptNode saveNode(String chapterId, String name, int orderNo, String outline) {
        ConceptNode node = new ConceptNode();
        node.setChapterId(chapterId);
        node.setName(truncate(name, 128));
        node.setOutline(truncate(outline, 2000));
        node.setOrderNo(orderNo);
        return conceptNodeRepository.save(node);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required.");
        }
        return truncate(value.trim(), 64);
    }

    private String normalizeGoal(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return truncate(value.trim(), 1000);
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}


