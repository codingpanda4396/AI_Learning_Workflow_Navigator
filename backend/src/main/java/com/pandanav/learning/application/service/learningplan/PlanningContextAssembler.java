package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsDto;
import com.pandanav.learning.application.command.PreviewLearningPlanCommand;
import com.pandanav.learning.application.service.WeakPointDiagnosisService;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearnerProfileSnapshot;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.NodeMastery;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.model.LearnerSignalSnapshot;
import com.pandanav.learning.domain.model.TrainingAttemptSummary;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearnerProfileSnapshotRepository;
import com.pandanav.learning.domain.repository.NodeMasteryRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.diagnosis.LearnerProfileStructuredSnapshotDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PlanningContextAssembler {

    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final NodeMasteryRepository nodeMasteryRepository;
    private final TaskRepository taskRepository;
    private final WeakPointDiagnosisService weakPointDiagnosisService;
    private final LearnerStateInterpreter learnerStateInterpreter;
    private final LearnerSignalInterpreter learnerSignalInterpreter;
    private final LearnerEvidenceAggregator learnerEvidenceAggregator;
    private final LearnerProfileSnapshotRepository learnerProfileSnapshotRepository;
    private final ObjectMapper objectMapper;

    public PlanningContextAssembler(
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        NodeMasteryRepository nodeMasteryRepository,
        TaskRepository taskRepository,
        WeakPointDiagnosisService weakPointDiagnosisService,
        LearnerStateInterpreter learnerStateInterpreter,
        LearnerSignalInterpreter learnerSignalInterpreter,
        LearnerEvidenceAggregator learnerEvidenceAggregator,
        LearnerProfileSnapshotRepository learnerProfileSnapshotRepository,
        ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.nodeMasteryRepository = nodeMasteryRepository;
        this.taskRepository = taskRepository;
        this.weakPointDiagnosisService = weakPointDiagnosisService;
        this.learnerStateInterpreter = learnerStateInterpreter;
        this.learnerSignalInterpreter = learnerSignalInterpreter;
        this.learnerEvidenceAggregator = learnerEvidenceAggregator;
        this.learnerProfileSnapshotRepository = learnerProfileSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    public LearningPlanPlanningContext assemble(PreviewLearningPlanCommand command) {
        PlanAdjustments adjustments = toAdjustments(command.adjustments());
        Optional<LearningSession> latestSession = sessionRepository.findLatestActiveByUserPk(command.userId())
            .or(() -> sessionRepository.findLatestByUserId("user-" + command.userId()));

        String goalKey = command.sessionId() == null ? command.goalText() : "session-" + command.sessionId();
        String courseName = prefer(command.courseName(), latestSession.map(LearningSession::getCourseId).orElse("course-" + sanitize(goalKey)));
        String chapterName = prefer(command.chapterName(), latestSession.map(LearningSession::getChapterId).orElse("chapter-" + sanitize(goalKey)));
        String goalText = prefer(command.goalText(), latestSession.map(LearningSession::getGoalText).orElse("围绕当前章节目标完成本轮重点补强"));

        List<ConceptNode> persistedNodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(chapterName);
        Map<Long, List<Long>> prerequisiteMap = persistedNodes.isEmpty()
            ? Map.of()
            : conceptNodeRepository.findPrerequisiteNodeIdsByChapterId(chapterName);

        List<WeakPointDiagnosisService.WeakNode> weakNodes = latestSession.isPresent()
            ? weakPointDiagnosisService.diagnoseWeakPoints(latestSession.get().getId(), command.userId()).weakNodes()
            : List.of();
        Map<Long, WeakPointDiagnosisService.WeakNode> weakNodeMap = weakNodes.stream()
            .collect(Collectors.toMap(WeakPointDiagnosisService.WeakNode::nodeId, node -> node, (left, right) -> left, LinkedHashMap::new));

        Map<Long, NodeMastery> masteryMap = nodeMasteryRepository.findByUserIdAndChapterId(command.userId(), chapterName).stream()
            .collect(Collectors.toMap(NodeMastery::getNodeId, row -> row, (left, right) -> left, LinkedHashMap::new));

        List<TrainingAttemptSummary> recentAttempts = latestSession.isPresent()
            ? taskRepository.findRecentTrainingAttempts(latestSession.get().getId(), 6)
            : List.of();
        Map<Long, List<String>> errorTagsByNode = recentAttempts.stream()
            .collect(Collectors.groupingBy(
                TrainingAttemptSummary::nodeId,
                LinkedHashMap::new,
                Collectors.flatMapping(item -> item.errorTags().stream(), Collectors.toList())
            ));

        List<LearningPlanContextNode> nodes = persistedNodes.isEmpty()
            ? buildSyntheticNodes(goalText)
            : persistedNodes.stream()
                .sorted(Comparator.comparing(ConceptNode::getOrderNo).thenComparing(ConceptNode::getId))
                .map(node -> toContextNode(
                    node,
                    masteryMap.get(node.getId()),
                    weakNodeMap.get(node.getId()),
                    errorTagsByNode.getOrDefault(node.getId(), List.of()),
                    prerequisiteMap.getOrDefault(node.getId(), List.of())
                ))
                .toList();

        List<String> recentErrorTags = recentAttempts.stream()
            .flatMap(item -> item.errorTags().stream())
            .filter(tag -> tag != null && !tag.isBlank())
            .distinct()
            .limit(6)
            .toList();
        List<Integer> recentScores = recentAttempts.stream().map(TrainingAttemptSummary::score).filter(score -> score != null).toList();
        List<String> weakLabels = weakNodes.stream().map(WeakPointDiagnosisService.WeakNode::nodeName).limit(4).toList();
        LearnerProfileSnapshot learnerProfileSnapshot = resolveLearnerProfileSnapshot(command.diagnosisId());
        String learnerProfileSummary = learnerProfileSnapshot != null
            ? buildSnapshotSummary(learnerProfileSnapshot)
            : weakLabels.isEmpty()
            ? "当前可用历史证据较少，系统会优先根据目标与知识顺序给出稳健预览。"
            : "结合最近的薄弱点表现，本轮会优先补齐 " + String.join("、", weakLabels) + " 相关的关键连接。";

        LearningPlanPlanningContext baseContext = new LearningPlanPlanningContext(
            command.userId(),
            goalKey,
            command.diagnosisId(),
            courseName,
            chapterName,
            goalText,
            latestSession.map(LearningSession::getId).orElse(command.sessionId()),
            nodes,
            recentErrorTags,
            recentScores,
            weakLabels,
            learnerProfileSummary,
            adjustments,
            null,
            null,
            null,
            null,
            null,
            learnerProfileSnapshot,
            null,
            null,
            null,
            null,
            null
        );
        LearnerSignalSnapshot learnerSignalSnapshot = learnerSignalInterpreter.interpret(baseContext);
        return new LearningPlanPlanningContext(
            baseContext.userId(),
            baseContext.goalId(),
            baseContext.diagnosisId(),
            baseContext.courseId(),
            baseContext.chapterId(),
            baseContext.goalText(),
            baseContext.sourceSessionId(),
            baseContext.nodes(),
            baseContext.recentErrorTags(),
            baseContext.recentScores(),
            baseContext.weakPointLabels(),
            baseContext.learnerProfileSummary(),
            baseContext.adjustments(),
            baseContext.requestedStrategy(),
            baseContext.requestedTimeBudgetMinutes(),
            baseContext.adjustmentReason(),
            baseContext.userFeedback(),
            baseContext.basedOnPreviewId(),
            baseContext.learnerProfileSnapshot(),
            learnerStateInterpreter.interpret(baseContext),
            learnerSignalSnapshot,
            learnerEvidenceAggregator.aggregate(baseContext, learnerSignalSnapshot, null),
            null,
            null
        );
    }

    private LearnerProfileSnapshot resolveLearnerProfileSnapshot(String diagnosisId) {
        if (diagnosisId == null || diagnosisId.isBlank()) {
            return null;
        }
        try {
            Long diagnosisSessionId = Long.parseLong(diagnosisId.trim());
            return learnerProfileSnapshotRepository.findByDiagnosisSessionId(diagnosisSessionId).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildSnapshotSummary(LearnerProfileSnapshot snapshot) {
        if (snapshot.getStructuredSnapshotJson() != null && !snapshot.getStructuredSnapshotJson().isBlank()) {
            try {
                LearnerProfileStructuredSnapshotDto structured = objectMapper.readValue(
                    snapshot.getStructuredSnapshotJson(),
                    LearnerProfileStructuredSnapshotDto.class
                );
                if (structured.summary() != null && structured.summary().currentState() != null
                    && !structured.summary().currentState().isBlank()) {
                    return structured.summary().currentState();
                }
            } catch (Exception ignored) {
                // fall through to legacy summary
            }
        }
        String preference = readMapValue(snapshot.getStrategyHints(), "learningPreference");
        String supportPriority = readMapValue(snapshot.getStrategyHints(), "supportPriority");
        String timeBudget = readMapValue(snapshot.getConstraints(), "timeBudget");
        List<String> parts = new ArrayList<>();
        if (!supportPriority.isBlank()) {
            parts.add("支持重点偏向 " + supportPriority);
        }
        if (!preference.isBlank()) {
            parts.add("学习偏好为 " + preference);
        }
        if (!timeBudget.isBlank()) {
            parts.add("时间预算约束为 " + timeBudget);
        }
        if (parts.isEmpty()) {
            return "已读取诊断画像快照，本轮将按画像信号约束候选策略并动态校验一致性。";
        }
        return "已读取诊断画像快照：" + String.join("，", parts) + "。";
    }

    private String readMapValue(Map<String, Object> values, String key) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        Object raw = values.get(key);
        if (raw == null) {
            return "";
        }
        String text = String.valueOf(raw).trim();
        return text.isBlank() ? "" : text;
    }

    private LearningPlanContextNode toContextNode(
        ConceptNode node,
        NodeMastery mastery,
        WeakPointDiagnosisService.WeakNode weakNode,
        List<String> recentErrorTags,
        List<Long> prerequisiteIds
    ) {
        int masteryScore = mastery != null && mastery.getMasteryScore() != null ? mastery.getMasteryScore().intValue() : 55;
        int attemptCount = mastery != null && mastery.getAttemptCount() != null ? mastery.getAttemptCount() : 0;
        List<String> reasons = weakNode == null ? List.of() : weakNode.reasons();
        return new LearningPlanContextNode(
            String.valueOf(node.getId()),
            node.getId(),
            node.getName(),
            node.getOrderNo(),
            resolveDifficulty(node.getOrderNo()),
            masteryScore,
            attemptCount,
            reasons,
            recentErrorTags.stream().distinct().limit(4).toList(),
            prerequisiteIds.stream().map(String::valueOf).toList()
        );
    }

    private List<LearningPlanContextNode> buildSyntheticNodes(String goalText) {
        List<String> names = List.of("基础框架", "关键机制", "综合应用");
        List<LearningPlanContextNode> nodes = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            nodes.add(new LearningPlanContextNode(
                "virtual-" + (i + 1),
                null,
                goalText.length() > 12 ? goalText.substring(0, Math.min(goalText.length(), 12)) + "-" + names.get(i) : names.get(i),
                i + 1,
                i + 1,
                45 + i * 10,
                i == 0 ? 0 : 1,
                i == 0 ? List.of("PREREQUISITE_GAP") : List.of(),
                List.of(),
                i == 0 ? List.of() : List.of("virtual-" + i)
            ));
        }
        return nodes;
    }

    private PlanAdjustments toAdjustments(LearningPlanAdjustmentsDto request) {
        if (request == null) {
            return PlanAdjustments.defaults();
        }
        return new PlanAdjustments(
            request.intensity(),
            request.learningMode(),
            request.prioritizeFoundation() == null || request.prioritizeFoundation()
        ).normalized();
    }

    private int resolveDifficulty(Integer orderNo) {
        if (orderNo == null) {
            return 2;
        }
        return Math.max(1, Math.min(5, orderNo));
    }

    private String prefer(String first, String second) {
        return first != null && !first.isBlank() ? first.trim() : second;
    }

    private String sanitize(String value) {
        return value == null ? "default" : value.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
    }
}
