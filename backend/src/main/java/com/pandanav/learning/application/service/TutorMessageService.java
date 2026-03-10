package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.tutor.TutorMessageListResponse;
import com.pandanav.learning.api.dto.tutor.TutorMessageResponse;
import com.pandanav.learning.api.dto.tutor.TutorSendMessageResponse;
import com.pandanav.learning.application.service.tutor.TutorProvider;
import com.pandanav.learning.application.service.tutor.TutorProviderReply;
import com.pandanav.learning.application.service.tutor.TutorProviderRequest;
import com.pandanav.learning.domain.enums.TutorMessageRole;
import com.pandanav.learning.domain.llm.model.TutorReplyMode;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.model.TutorMessage;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.domain.repository.TutorMessageRepository;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TutorMessageService {

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final TaskRepository taskRepository;
    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final TutorMessageRepository tutorMessageRepository;
    private final TutorProvider tutorProvider;

    public TutorMessageService(
        TaskRepository taskRepository,
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        TutorMessageRepository tutorMessageRepository,
        TutorProvider tutorProvider
    ) {
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.tutorMessageRepository = tutorMessageRepository;
        this.tutorProvider = tutorProvider;
    }

    public TutorMessageListResponse listMessages(Long sessionId, Long taskId, Long userId) {
        ensureTaskOwnership(sessionId, taskId, userId);
        List<TutorMessageResponse> messages = tutorMessageRepository
            .findBySessionIdAndTaskIdAndUserId(sessionId, taskId, userId)
            .stream()
            .map(this::toResponse)
            .toList();

        return new TutorMessageListResponse(sessionId, taskId, messages);
    }

    @Transactional
    public TutorSendMessageResponse sendMessage(Long sessionId, Long taskId, Long userId, String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new BadRequestException("Tutor message content must not be blank.");
        }

        Task task = ensureTaskOwnership(sessionId, taskId, userId);
        LearningSession session = sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException("Session or task not found."));
        ConceptNode node = conceptNodeRepository.findById(task.getNodeId()).orElse(null);
        sessionRepository.touchLastActive(sessionId);

        TutorMessage userMessage = new TutorMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setTaskId(taskId);
        userMessage.setUserId(userId);
        userMessage.setRole(TutorMessageRole.USER);
        userMessage.setContent(normalizedContent);
        TutorMessage savedUserMessage = tutorMessageRepository.save(userMessage);

        List<TutorMessage> history = tutorMessageRepository.findRecentBySessionIdAndTaskIdAndUserId(
            sessionId,
            taskId,
            userId,
            MAX_HISTORY_MESSAGES
        );
        TutorProviderReply reply = tutorProvider.generateReply(
            new TutorProviderRequest(
                sessionId,
                taskId,
                userId,
                task.getStage() == null ? null : task.getStage().name(),
                task.getObjective(),
                node == null ? null : node.getName(),
                session.getGoalText(),
                normalizedContent,
                TutorReplyMode.AUTO,
                TutorReplyMode.AUTO,
                history
            )
        );

        TutorMessage assistantMessage = new TutorMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setTaskId(taskId);
        assistantMessage.setUserId(userId);
        assistantMessage.setRole(TutorMessageRole.ASSISTANT);
        assistantMessage.setContent(reply.content());
        assistantMessage.setLlmProvider(reply.provider());
        assistantMessage.setLlmModel(reply.model());
        TutorMessage savedAssistantMessage = tutorMessageRepository.save(assistantMessage);

        return new TutorSendMessageResponse(
            sessionId,
            taskId,
            toResponse(savedUserMessage),
            toResponse(savedAssistantMessage)
        );
    }

    private Task ensureTaskOwnership(Long sessionId, Long taskId, Long userId) {
        Task task = taskRepository.findByIdAndUserPk(taskId, userId)
            .orElseThrow(() -> new NotFoundException("Session or task not found."));
        if (!sessionId.equals(task.getSessionId())) {
            throw new NotFoundException("Session or task not found.");
        }
        return task;
    }

    private TutorMessageResponse toResponse(TutorMessage message) {
        return new TutorMessageResponse(
            message.getId(),
            message.getSessionId(),
            message.getTaskId(),
            message.getRole().toApiRole(),
            message.getContent(),
            message.getCreatedAt()
        );
    }
}
