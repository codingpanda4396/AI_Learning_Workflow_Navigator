package com.pandanav.learning.application.service.diagnosis;

import com.pandanav.learning.api.dto.diagnosis.LearnerProfileStructuredSnapshotDto;
import com.pandanav.learning.api.dto.diagnosis.LearnerProfileStructuredSnapshotDto.PlanHintsDto;
import com.pandanav.learning.api.dto.diagnosis.LearnerProfileStructuredSnapshotDto.SummaryDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 从诊断答案推导结构化 learnerProfileSnapshot（riskTags、planHints、summary），规则实现，不调用 LLM。
 */
@Service
public class DiagnosisProfileDerivationService {

    private static final String V1 = "v1";

    public LearnerProfileStructuredSnapshotDto derive(Map<String, String> answerByQuestionId, String topicTitle) {
        String foundationLevel = get(answerByQuestionId, StructuredDiagnosisQuestionFactory.Q_FOUNDATION);
        String primaryBlocker = get(answerByQuestionId, StructuredDiagnosisQuestionFactory.Q_BLOCKER);
        String practiceLevel = get(answerByQuestionId, StructuredDiagnosisQuestionFactory.Q_PRACTICE);
        String learningPreference = get(answerByQuestionId, StructuredDiagnosisQuestionFactory.Q_PREFERENCE);
        String goalType = get(answerByQuestionId, StructuredDiagnosisQuestionFactory.Q_GOAL);
        String timeBudget = get(answerByQuestionId, StructuredDiagnosisQuestionFactory.Q_TIME_BUDGET);
        String topicConceptClarity = get(answerByQuestionId, StructuredDiagnosisQuestionFactory.Q_TOPIC_CORE);
        String topicOperationRisk = get(answerByQuestionId, StructuredDiagnosisQuestionFactory.Q_TOPIC_OPERATION);

        List<String> riskTags = deriveRiskTags(
            foundationLevel, primaryBlocker, goalType, topicOperationRisk, topicConceptClarity
        );
        PlanHintsDto planHints = derivePlanHints(
            foundationLevel, primaryBlocker, learningPreference, timeBudget, goalType
        );
        SummaryDto summary = deriveSummary(
            foundationLevel, primaryBlocker, practiceLevel, goalType, topicConceptClarity, topicOperationRisk,
            learningPreference, timeBudget
        );

        return new LearnerProfileStructuredSnapshotDto(
            V1,
            foundationLevel,
            primaryBlocker,
            practiceLevel,
            learningPreference,
            goalType,
            timeBudget,
            topicConceptClarity,
            topicOperationRisk,
            riskTags,
            planHints,
            summary
        );
    }

    private static String get(Map<String, String> map, String key) {
        if (map == null) return "";
        String v = map.get(key);
        return v == null ? "" : v.trim();
    }

    /** 风险标签均来源于答题信号，数量控制在 1~3 个，按优先级保留。 */
    private static List<String> deriveRiskTags(
        String foundationLevel,
        String primaryBlocker,
        String goalType,
        String topicOperationRisk,
        String topicConceptClarity
    ) {
        List<String> ordered = new ArrayList<>();
        if ("BEGINNER".equals(foundationLevel)) {
            ordered.add("FOUNDATION_GAP");
        }
        if ("CONCEPT_CONFUSION".equals(primaryBlocker) || "NOT_CLEAR".equals(topicConceptClarity) || "PARTLY_CLEAR".equals(topicConceptClarity)) {
            if (!ordered.contains("CONCEPT_NOT_STABLE")) {
                ordered.add("CONCEPT_NOT_STABLE");
            }
        }
        if ("PROCESS_CONFUSION".equals(topicOperationRisk)) {
            ordered.add("PROCESS_CONFUSION");
        }
        if ("FOLLOW_BUT_CANNOT_DO".equals(primaryBlocker)) {
            ordered.add("INDEPENDENT_SOLVING_WEAKNESS");
        }
        if ("BASIC_OK_BUT_FAIL_ON_VARIATION".equals(primaryBlocker)) {
            ordered.add("TRANSFER_WEAKNESS");
        }
        if ("CAN_DO_BUT_CANNOT_EXPLAIN".equals(primaryBlocker)) {
            ordered.add("EXPRESSION_WEAKNESS");
        }
        if ("BOUNDARY_WEAKNESS".equals(topicOperationRisk)) {
            ordered.add("BOUNDARY_WEAKNESS");
        }
        if ("INTERVIEW".equals(goalType) && ("BEGINNER".equals(foundationLevel) || "BASIC".equals(foundationLevel))) {
            ordered.add("INTERVIEW_FOUNDATION_RISK");
        }
        if ("EXAM".equals(goalType) && ("BEGINNER".equals(foundationLevel) || "BASIC".equals(foundationLevel) || "NOT_CLEAR".equals(topicConceptClarity) || "PARTLY_CLEAR".equals(topicConceptClarity))) {
            ordered.add("EXAM_ORIENTED_SURFACE_LEARNING_RISK");
        }
        return ordered.size() > 3 ? ordered.subList(0, 3) : ordered;
    }

    private static PlanHintsDto derivePlanHints(
        String foundationLevel,
        String primaryBlocker,
        String learningPreference,
        String timeBudget,
        String goalType
    ) {
        String entryMode = "FOUNDATION_FIRST";
        if ("BEGINNER".equals(foundationLevel) || "CONCEPT_CONFUSION".equals(primaryBlocker)) {
            entryMode = "FOUNDATION_FIRST";
        } else if ("FOLLOW_BUT_CANNOT_DO".equals(primaryBlocker)) {
            entryMode = "EXAMPLE_FIRST";
        } else if ("BASIC_OK_BUT_FAIL_ON_VARIATION".equals(primaryBlocker)) {
            entryMode = "EXAMPLE_THEN_RULE";
        }

        String explanationStyle = "TEXT_WITH_MINI_EXAMPLE";
        if ("VISUAL_FIRST".equals(learningPreference)) {
            explanationStyle = "VISUAL_WITH_MINI_EXAMPLE";
        } else if ("CODE_FIRST".equals(learningPreference)) {
            explanationStyle = "CODE_WITH_MINI_VISUAL";
        }

        String pace = "NORMAL";
        String taskGranularity = "MEDIUM";
        if ("SHORT_10".equals(timeBudget)) {
            pace = "FAST";
            taskGranularity = "SMALL";
        } else if ("MEDIUM_30".equals(timeBudget)) {
            pace = "NORMAL";
            taskGranularity = "SMALL";
        } else if ("LONG_60".equals(timeBudget) || "SYSTEMATIC".equals(timeBudget)) {
            pace = "DEEP";
            taskGranularity = "MEDIUM";
        }

        String focusMode = "KNOWLEDGE_AND_STANDARD_QUESTIONS";
        if ("INTERVIEW".equals(goalType)) {
            focusMode = "HIGH_FREQUENCY_PATTERNS";
        } else if ("PROJECT".equals(goalType)) {
            focusMode = "IMPLEMENTATION_FLOW";
        } else if ("EXAM".equals(goalType)) {
            focusMode = "KNOWLEDGE_AND_STANDARD_QUESTIONS";
        }

        return new PlanHintsDto(entryMode, explanationStyle, pace, taskGranularity, focusMode);
    }

    private static SummaryDto deriveSummary(
        String foundationLevel,
        String primaryBlocker,
        String practiceLevel,
        String goalType,
        String topicConceptClarity,
        String topicOperationRisk,
        String learningPreference,
        String timeBudget
    ) {
        String currentState = buildCurrentState(
            foundationLevel, primaryBlocker, practiceLevel, goalType, topicConceptClarity, topicOperationRisk
        );
        List<String> evidence = buildEvidence(
            foundationLevel, primaryBlocker, goalType, learningPreference, timeBudget
        );
        return new SummaryDto(currentState, evidence);
    }

    private static String buildCurrentState(
        String foundationLevel,
        String primaryBlocker,
        String practiceLevel,
        String goalType,
        String topicConceptClarity,
        String topicOperationRisk
    ) {
        if ("BEGINNER".equals(foundationLevel)) {
            return "你刚开始接触这个主题，系统会从最基础的结构和概念带你起步。";
        }
        if ("ADVANCED".equals(foundationLevel) && !"MANY".equals(practiceLevel)) {
            return "你自评基础不错，但练习量偏少，建议先做少量典型题巩固再拓展。";
        }
        if ("BASIC_OK_BUT_FAIL_ON_VARIATION".equals(primaryBlocker)) {
            return "你不是完全没学过，而是基础有一些印象，但一到变形和边界就不稳定。";
        }
        if ("FOLLOW_BUT_CANNOT_DO".equals(primaryBlocker)) {
            return "你看懂例子问题不大，但自己动手容易卡住，需要更多从模仿到独立的练习。";
        }
        if ("CONCEPT_CONFUSION".equals(primaryBlocker)) {
            return "你当前更大的卡点在概念本身，先把核心定义和结构搞清楚会更顺。";
        }
        if ("CAN_DO_BUT_CANNOT_EXPLAIN".equals(primaryBlocker)) {
            return "你能写出来，但讲不清为什么，说明表达与归纳还需要加强。";
        }
        if ("INTERVIEW".equals(goalType) && ("BEGINNER".equals(foundationLevel) || "BASIC".equals(foundationLevel))) {
            return "你目标是面试准备，但基础还不稳，建议先扎牢核心再刷题。";
        }
        if ("PROJECT".equals(goalType)) {
            return "你更偏向项目实践，系统会按实现流程和常见场景来安排。";
        }
        return "根据你的选择，系统已识别当前起点与目标，会据此安排下一步。";
    }

    private static List<String> buildEvidence(
        String foundationLevel,
        String primaryBlocker,
        String goalType,
        String learningPreference,
        String timeBudget
    ) {
        List<String> evidence = new ArrayList<>();
        switch (foundationLevel) {
            case "BEGINNER" -> evidence.add("你选择了「刚开始接触」");
            case "BASIC" -> evidence.add("你选择了「学过但还不太熟」");
            case "PROFICIENT" -> evidence.add("你选择了「基础比较熟」");
            case "ADVANCED" -> evidence.add("你选择了「已经能独立应用」");
            default -> { }
        }
        if (!primaryBlocker.isBlank()) {
            String blockerLabel = switch (primaryBlocker) {
                case "CONCEPT_CONFUSION" -> "概念本身就不太清楚";
                case "FOLLOW_BUT_CANNOT_DO" -> "看懂例子但自己不会做";
                case "BASIC_OK_BUT_FAIL_ON_VARIATION" -> "基础题会，一变形就容易卡住";
                case "CAN_DO_BUT_CANNOT_EXPLAIN" -> "能写出来，但讲不清为什么";
                default -> primaryBlocker;
            };
            evidence.add("你当前更大的卡点是「" + blockerLabel + "」");
        }
        if (!goalType.isBlank()) {
            String goalLabel = switch (goalType) {
                case "QUICK_START" -> "快速入门";
                case "EXAM" -> "应对考试";
                case "INTERVIEW" -> "面试准备";
                case "PROJECT" -> "项目实践";
                case "PATCH_WEAKNESS" -> "查缺补漏";
                default -> goalType;
            };
            evidence.add("你这轮学习目标更偏向「" + goalLabel + "」");
        }
        return evidence.size() > 3 ? evidence.subList(0, 3) : evidence;
    }
}
