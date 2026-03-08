package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.task.TaskDetailResponse;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class TaskQueryService {

    private final TaskRepository taskRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final ObjectMapper objectMapper;

    public TaskQueryService(
        TaskRepository taskRepository,
        ConceptNodeRepository conceptNodeRepository,
        ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.objectMapper = objectMapper;
    }

    public TaskDetailResponse getTaskDetail(Long taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        ConceptNode conceptNode = conceptNodeRepository.findById(task.getNodeId())
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        boolean hasOutput = hasOutput(task.getOutputJson());
        JsonNode output = hasOutput ? parseOutput(task.getOutputJson()) : null;

        return new TaskDetailResponse(
            task.getId(),
            task.getSessionId(),
            task.getNodeId(),
            conceptNode.getName(),
            task.getStage().name(),
            task.getObjective(),
            task.getStatus().name(),
            hasOutput,
            output
        );
    }

    private boolean hasOutput(String outputJson) {
        return outputJson != null && !outputJson.isBlank() && !"null".equalsIgnoreCase(outputJson.trim());
    }

    private JsonNode parseOutput(String outputJson) {
        try {
            return objectMapper.readTree(outputJson);
        } catch (Exception ex) {
            throw new InternalServerException("Stored output_json is invalid.");
        }
    }
}
