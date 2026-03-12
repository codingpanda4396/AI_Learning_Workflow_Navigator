package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.plan.ConfirmLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsRequest;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanSummaryResponse;
import com.pandanav.learning.api.dto.plan.PlanPathNodeResponse;
import com.pandanav.learning.api.dto.plan.PlanReasonResponse;
import com.pandanav.learning.api.dto.plan.PlanTaskPreviewResponse;
import com.pandanav.learning.application.command.ConfirmLearningPlanCommand;
import com.pandanav.learning.application.command.PreviewLearningPlanCommand;
import com.pandanav.learning.domain.enums.LearningPlanStatus;
import com.pandanav.learning.domain.enums.SessionStatus;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningPlan;
import com.pandanav.learning.domain.model.LearningPlanAggregate;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.model.PlanPathNode;
import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningPlanRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LearningPlanService {

    private final PlanningContextAssembler planningContextAssembler;
    private final LearningPlanOrchestrator learningPlanOrchestrator;
    private final LearningPlanRepository learningPlanRepository;
    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy;
    private final ObjectMapper objectMapper;

    public LearningPlanService(
        PlanningContextAssembler planningContextAssembler,
        LearningPlanOrchestrator learningPlanOrchestrator,
        LearningPlanRepository learningPlanRepository,
        SessionRepository sessionRepository,
        TaskRepository taskRepository,
        ConceptNodeRepository conceptNodeRepository,
        TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy,
        ObjectMapper objectMapper
    ) {
        this.planningContextAssembler = planningContextAssembler;
        this.learningPlanOrchestrator = learningPlanOrchestrator;
        this.learningPlanRepository = learningPlanRepository;
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.taskObjectiveTemplateStrategy = taskObjectiveTemplateStrategy;
        this.objectMapper = objectMapper;
    }

    public LearningPlanPreviewResponse preview(PreviewLearningPlanCommand command) {
        LearningPlanPlanningContext context = planningContextAssembler.assemble(command);
        LearningPlanOrchestrator.OrchestratedPlan orchestrated = learningPlanOrchestrator.preview(context);

        LearningPlan plan = new LearningPlan();
        plan.setUserId(command.userId());
        plan.setGoalId(command.goalId());
        plan.setDiagnosisId(command.diagnosisId());
        plan.setStatus(LearningPlanStatus.DRAFT);
        plan.setLlmTraceId(orchestrated.llmTraceId());
        writeSnapshot(plan, orchestrated.preview(), context);

        LearningPlan saved = learningPlanRepository.save(plan);
        return toResponse(saved.getId(), orchestrated.preview());
    }

    public LearningPlanPreviewResponse get(Long planId, Long userId) {
        LearningPlanAggregate aggregate = load(planId, userId);
        return toResponse(aggregate.plan().getId(), aggregate.preview());
    }

    public ConfirmLearningPlanResponse confirm(ConfirmLearningPlanCommand command) {
        LearningPlanAggregate aggregate = load(command.planId(), command.userId());
        LearningPlan plan = aggregate.plan();
        if (plan.getSessionId() != null) {
            return buildConfirmResponse(plan);
        }

        List<ConceptNode> chapterNodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(aggregate.planningContext().chapterId());
        if (chapterNodes.isEmpty()) {
            chapterNodes = bootstrapNodes(aggregate.planningContext());
        }
        Map<String, ConceptNode> nodeByPlanId = alignPlanNodes(aggregate.planningContext().nodes(), chapterNodes);
        String startNodePlanId = aggregate.preview().summary().recommendedStartNodeId();
        ConceptNode startNode = nodeByPlanId.get(startNodePlanId);
        if (startNode == null) {
            throw new InternalServerException("Learning plan start node is missing.");
        }

        LearningSession session = new LearningSession();
        session.setUserId("user-" + command.userId());
        session.setUserPk(command.userId());
        session.setCourseId(aggregate.planningContext().courseId());
        session.setChapterId(aggregate.planningContext().chapterId());
        session.setGoalText(aggregate.planningContext().goalText());
        session.setCurrentNodeId(startNode.getId());
        session.setCurrentStage(Stage.STRUCTURE);
        session.setStatus(SessionStatus.PLANNING);
        LearningSession savedSession = sessionRepository.save(session);

        List<Task> tasks = new ArrayList<>();
        for (PlanPathNode pathNode : aggregate.preview().pathPreview()) {
            ConceptNode conceptNode = nodeByPlanId.get(pathNode.nodeId());
            if (conceptNode == null) {
                continue;
            }
            tasks.add(buildTask(savedSession.getId(), conceptNode.getId(), Stage.STRUCTURE, conceptNode.getName()));
            tasks.add(buildTask(savedSession.getId(), conceptNode.getId(), Stage.UNDERSTANDING, conceptNode.getName()));
            tasks.add(buildTask(savedSession.getId(), conceptNode.getId(), Stage.TRAINING, conceptNode.getName()));
            tasks.add(buildTask(savedSession.getId(), conceptNode.getId(), Stage.REFLECTION, conceptNode.getName()));
        }
        List<Task> savedTasks = taskRepository.saveAll(tasks);
        sessionRepository.updateStatus(savedSession.getId(), SessionStatus.LEARNING);

        plan.setSessionId(savedSession.getId());
        plan.setStatus(LearningPlanStatus.CONFIRMED);
        learningPlanRepository.update(plan);

        Long firstTaskId = savedTasks.stream().findFirst().map(Task::getId).orElse(null);
        return new ConfirmLearningPlanResponse(
            String.valueOf(plan.getId()),
            savedSession.getId(),
            startNode.getId(),
            firstTaskId,
            "/sessions/" + savedSession.getId()
        );
    }

    private ConfirmLearningPlanResponse buildConfirmResponse(LearningPlan plan) {
        List<Task> tasks = taskRepository.findBySessionIdWithStatus(plan.getSessionId());
        Long firstTaskId = tasks.isEmpty() ? null : tasks.get(0).getId();
        Long currentNodeId = sessionRepository.findByIdAndUserPk(plan.getSessionId(), plan.getUserId())
            .map(LearningSession::getCurrentNodeId)
            .orElse(null);
        return new ConfirmLearningPlanResponse(
            String.valueOf(plan.getId()),
            plan.getSessionId(),
            currentNodeId,
            firstTaskId,
            "/sessions/" + plan.getSessionId()
        );
    }

    private LearningPlanAggregate load(Long planId, Long userId) {
        LearningPlan plan = learningPlanRepository.findByIdAndUserId(planId, userId)
            .orElseThrow(() -> new NotFoundException("Learning plan not found."));
        try {
            LearningPlanPreview preview = new LearningPlanPreview(
                objectMapper.readValue(plan.getSummaryJson(), LearningPlanSummary.class),
                objectMapper.readValue(plan.getReasonsJson(), new TypeReference<List<PlanReason>>() {
                }),
                objectMapper.readValue(plan.getFocusesJson(), new TypeReference<List<String>>() {
                }),
                objectMapper.readValue(plan.getPathPreviewJson(), new TypeReference<List<PlanPathNode>>() {
                }),
                objectMapper.readValue(plan.getTaskPreviewJson(), new TypeReference<List<PlanTaskPreview>>() {
                }),
                objectMapper.readValue(plan.getAdjustmentsJson(), PlanAdjustments.class)
            );
            LearningPlanPlanningContext context = objectMapper.readValue(plan.getPlanningContextJson(), LearningPlanPlanningContext.class);
            return new LearningPlanAggregate(plan, preview, context);
        } catch (Exception ex) {
            throw new InternalServerException("Failed to read stored learning plan.");
        }
    }

    private void writeSnapshot(LearningPlan plan, LearningPlanPreview preview, LearningPlanPlanningContext context) {
        try {
            plan.setSummaryJson(objectMapper.writeValueAsString(preview.summary()));
            plan.setReasonsJson(objectMapper.writeValueAsString(preview.reasons()));
            plan.setFocusesJson(objectMapper.writeValueAsString(preview.focuses()));
            plan.setPathPreviewJson(objectMapper.writeValueAsString(preview.pathPreview()));
            plan.setTaskPreviewJson(objectMapper.writeValueAsString(preview.taskPreview()));
            plan.setAdjustmentsJson(objectMapper.writeValueAsString(preview.adjustments()));
            plan.setPlanningContextJson(objectMapper.writeValueAsString(context));
        } catch (Exception ex) {
            throw new InternalServerException("Failed to serialize learning plan.");
        }
    }

    private LearningPlanPreviewResponse toResponse(Long planId, LearningPlanPreview preview) {
        return new LearningPlanPreviewResponse(
            String.valueOf(planId),
            new LearningPlanSummaryResponse(
                preview.summary().headline(),
                preview.summary().recommendedStartNodeId(),
                preview.summary().recommendedStartNodeName(),
                preview.summary().recommendedPace(),
                preview.summary().estimatedMinutes(),
                preview.summary().estimatedNodeCount(),
                preview.summary().estimatedStageCount()
            ),
            preview.reasons().stream().map(item -> new PlanReasonResponse(item.type(), item.title(), item.description())).toList(),
            preview.focuses(),
            preview.pathPreview().stream().map(item -> new PlanPathNodeResponse(
                item.nodeId(),
                item.nodeName(),
                item.difficulty(),
                item.mastery(),
                item.status(),
                item.isRecommendedStart(),
                item.estimatedMinutes(),
                item.reasonTag()
            )).toList(),
            preview.taskPreview().stream().map(item -> new PlanTaskPreviewResponse(
                item.stage(),
                item.title(),
                item.goal(),
                item.learnerAction(),
                item.aiSupport(),
                item.estimatedMinutes()
            )).toList(),
            new LearningPlanAdjustmentsRequest(
                preview.adjustments().intensity(),
                preview.adjustments().learningMode(),
                preview.adjustments().preferPrerequisite()
            )
        );
    }

    private List<ConceptNode> bootstrapNodes(LearningPlanPlanningContext context) {
        List<ConceptNode> saved = new ArrayList<>();
        for (LearningPlanContextNode node : context.nodes()) {
            ConceptNode conceptNode = new ConceptNode();
            conceptNode.setChapterId(context.chapterId());
            conceptNode.setName(node.nodeName());
            conceptNode.setOutline("Goal=" + context.goalText());
            conceptNode.setOrderNo(node.orderNo());
            saved.add(conceptNodeRepository.save(conceptNode));
        }
        return saved;
    }

    private Map<String, ConceptNode> alignPlanNodes(List<LearningPlanContextNode> plannedNodes, List<ConceptNode> persistedNodes) {
        List<ConceptNode> orderedPersisted = persistedNodes.stream()
            .sorted(Comparator.comparing(ConceptNode::getOrderNo).thenComparing(ConceptNode::getId))
            .toList();
        Map<Long, ConceptNode> byId = orderedPersisted.stream()
            .collect(Collectors.toMap(ConceptNode::getId, node -> node, (left, right) -> left, LinkedHashMap::new));

        Map<String, ConceptNode> result = new LinkedHashMap<>();
        for (LearningPlanContextNode node : plannedNodes) {
            if (node.persistedNodeId() != null && byId.containsKey(node.persistedNodeId())) {
                result.put(node.planNodeId(), byId.get(node.persistedNodeId()));
            }
        }
        for (int i = 0; i < Math.min(plannedNodes.size(), orderedPersisted.size()); i++) {
            result.putIfAbsent(plannedNodes.get(i).planNodeId(), orderedPersisted.get(i));
        }
        return result;
    }

    private Task buildTask(Long sessionId, Long nodeId, Stage stage, String conceptName) {
        Task task = new Task();
        task.setSessionId(sessionId);
        task.setNodeId(nodeId);
        task.setStage(stage);
        task.setStatus(TaskStatus.PENDING);
        task.setObjective(taskObjectiveTemplateStrategy.buildObjective(stage, conceptName));
        return task;
    }
}
