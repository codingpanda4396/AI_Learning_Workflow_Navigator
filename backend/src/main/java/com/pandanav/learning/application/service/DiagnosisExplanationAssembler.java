package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisEvidenceSourceDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisReasoningStepDto;
import com.pandanav.learning.api.dto.diagnosis.LearnerProfileStructuredSnapshotDto;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisAnswer;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionOption;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DiagnosisExplanationAssembler {

    private static final String FALLBACK_QUESTION_TITLE = "诊断问题";
    private static final String FALLBACK_SELECTED_ANSWER = "基于你的作答信息";

    private final ObjectMapper objectMapper;

    public DiagnosisExplanationAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 基于唯一主画像 snapshot 组装解释：reasoningSteps 按题目真实结论，strength/weakness 仅当可追溯到 sourceQuestionId 时产出，不允许 unknown-question。
     */
    public DiagnosisExplanation assembleFromSnapshot(
        List<DiagnosisQuestion> questions,
        List<DiagnosisAnswer> answers,
        LearnerProfileStructuredSnapshotDto snapshot,
        Map<DiagnosisDimension, List<String>> answerCodesByDimension
    ) {
        List<DiagnosisQuestion> safeQuestions = questions == null ? List.of() : questions;
        List<DiagnosisAnswer> safeAnswers = answers == null ? List.of() : answers;
        Map<DiagnosisDimension, List<String>> safeCodesByDimension =
            answerCodesByDimension == null ? Map.of() : answerCodesByDimension;

        Map<String, DiagnosisQuestion> questionById = new LinkedHashMap<>();
        Map<DiagnosisDimension, DiagnosisQuestion> questionByDimension = new LinkedHashMap<>();
        for (DiagnosisQuestion question : safeQuestions) {
            if (question == null) continue;
            questionById.put(question.questionId(), question);
            questionByDimension.putIfAbsent(question.dimension(), question);
        }

        List<DiagnosisReasoningStepDto> reasoningSteps = buildReasoningStepsFromSnapshot(
            safeAnswers, questionById, questionByDimension, snapshot, safeCodesByDimension
        );
        if (reasoningSteps.isEmpty() && !questionByDimension.isEmpty()) {
            DiagnosisQuestion first = questionByDimension.values().iterator().next();
            String conclusion = snapshot != null ? inferConclusionFromSnapshot(first.dimension(), "", snapshot) : "系统已根据你的回答形成当前判断。";
            reasoningSteps = List.of(new DiagnosisReasoningStepDto(
                first.dimension().name(),
                first.questionId(),
                fallbackText(first.title(), FALLBACK_QUESTION_TITLE),
                FALLBACK_SELECTED_ANSWER,
                conclusion
            ));
        }

        List<DiagnosisEvidenceSourceDto> strengthSources = buildEvidenceSourcesFromSnapshot(snapshot, questionByDimension, true);
        List<DiagnosisEvidenceSourceDto> weaknessSources = buildEvidenceSourcesFromSnapshot(snapshot, questionByDimension, false);

        return new DiagnosisExplanation(reasoningSteps, strengthSources, weaknessSources);
    }

    private List<DiagnosisReasoningStepDto> buildReasoningStepsFromSnapshot(
        List<DiagnosisAnswer> answers,
        Map<String, DiagnosisQuestion> questionById,
        Map<DiagnosisDimension, DiagnosisQuestion> questionByDimension,
        LearnerProfileStructuredSnapshotDto snapshot,
        Map<DiagnosisDimension, List<String>> answerCodesByDimension
    ) {
        List<DiagnosisReasoningStepDto> steps = new ArrayList<>();
        for (DiagnosisAnswer answer : answers) {
            if (answer == null) continue;
            String questionId = answer.getQuestionId() == null ? "" : answer.getQuestionId().trim();
            if (questionId.isBlank()) continue;
            DiagnosisDimension dimension = answer.getDimension() == null ? DiagnosisDimension.FOUNDATION : answer.getDimension();
            DiagnosisQuestion question = questionById.get(questionId);
            if (question == null) question = questionByDimension.get(dimension);
            if (question == null) continue;
            String selectedAnswer = resolveSelectedAnswerLabel(answer, question);
            String code = firstCode(answerCodesByDimension.get(dimension));
            String conclusion = snapshot != null ? inferConclusionFromSnapshot(dimension, code, snapshot) : "系统已根据你的回答形成当前判断。";
            steps.add(new DiagnosisReasoningStepDto(
                dimension.name(),
                question.questionId(),
                fallbackText(question.title(), FALLBACK_QUESTION_TITLE),
                selectedAnswer,
                conclusion
            ));
        }
        return steps;
    }

    private String firstCode(List<String> codes) {
        return codes != null && !codes.isEmpty() ? codes.get(0) : "";
    }

    private String inferConclusionFromSnapshot(DiagnosisDimension dimension, String selectedCode, LearnerProfileStructuredSnapshotDto snapshot) {
        String code = selectedCode == null ? "" : selectedCode.trim();
        String foundation = nullToEmpty(snapshot.foundationLevel());
        String blocker = nullToEmpty(snapshot.primaryBlocker());
        String practice = nullToEmpty(snapshot.practiceLevel());
        String preference = nullToEmpty(snapshot.learningPreference());
        String goalType = nullToEmpty(snapshot.goalType());
        String timeBudget = nullToEmpty(snapshot.timeBudget());
        String topicCore = nullToEmpty(snapshot.topicConceptClarity());
        String topicOp = nullToEmpty(snapshot.topicOperationRisk());

        return switch (dimension) {
            case FOUNDATION -> {
                if ("BEGINNER".equals(foundation) || "BEGINNER".equals(code)) {
                    yield "你目前更接近刚开始接触，系统会默认你还需要先建立最基本的概念框架。";
                }
                if ("ADVANCED".equals(foundation) || "ADVANCED".equals(code)) {
                    yield "你的基础较为扎实，可更快推进到综合应用。";
                }
                if ("BASIC".equals(foundation) || "PROFICIENT".equals(foundation)) {
                    yield "你已有一定基础，但仍需通过阶段训练巩固稳定性。";
                }
                yield "根据你的选择，系统会从当前起点安排后续内容。";
            }
            case BLOCKER -> {
                if ("FOLLOW_BUT_CANNOT_DO".equals(blocker) || "FOLLOW_BUT_CANNOT_DO".equals(code)) {
                    yield "你不是完全看不懂，而是能跟着理解，但还不能独立完成，这意味着第一步更适合先用示例带你建立操作过程。";
                }
                if ("CONCEPT_CONFUSION".equals(blocker)) {
                    yield "你当前更大的卡点在概念本身，先把核心定义和结构搞清楚会更顺。";
                }
                if ("BASIC_OK_BUT_FAIL_ON_VARIATION".equals(blocker)) {
                    yield "你不是完全没学过，而是基础有一些印象，但一到变形和边界就不稳定。";
                }
                if ("CAN_DO_BUT_CANNOT_EXPLAIN".equals(blocker)) {
                    yield "你能写出来，但讲不清为什么，说明表达与归纳还需要加强。";
                }
                yield "你的主要卡点会影响任务难度分布与讲解方式。";
            }
            case PRACTICE -> {
                if ("NONE".equals(practice) || "NONE".equals(code)) {
                    yield "你目前几乎没有相关练习，系统会避免一开始就直接进入综合题训练。";
                }
                if ("MANY".equals(practice)) {
                    yield "你已有较多练习或实际使用经验，适合结合案例推进。";
                }
                yield "你的练习量信息将用于调整起步难度与示例密度。";
            }
            case PREFERENCE -> {
                if ("VISUAL_FIRST".equals(preference) || "VISUAL_FIRST".equals(code)) {
                    yield "你的进入方式更适合图解或结构示意，因此后续解释会优先提供可视化线索。";
                }
                if ("CODE_FIRST".equals(preference)) {
                    yield "你更适合从代码示例入手，后续会优先提供代码线索。";
                }
                if ("TEXT_FIRST".equals(preference)) {
                    yield "你更适合先看文字讲解，系统会按此安排解释顺序。";
                }
                if ("PRACTICE_FIRST".equals(preference)) {
                    yield "你更适合先做小题再总结，系统会优先安排小练习。";
                }
                yield "你的学习偏好会影响每个阶段的解释方式与练习比重。";
            }
            case GOAL -> {
                if ("QUICK_START".equals(goalType) || "QUICK_START".equals(code)) {
                    yield "你当前更希望快速进入状态，因此系统会优先安排最小可执行起步动作，而不是完整铺开所有内容。";
                }
                if ("EXAM".equals(goalType)) {
                    yield "你更偏向应对考试，系统会侧重知识点与标准题型。";
                }
                if ("INTERVIEW".equals(goalType)) {
                    yield "你更偏向面试准备，系统会侧重高频考点与表达。";
                }
                if ("PROJECT".equals(goalType)) {
                    yield "你更偏向项目实践，系统会按实现流程安排。";
                }
                if ("PATCH_WEAKNESS".equals(goalType)) {
                    yield "你更偏向查缺补漏，系统会先定位薄弱再针对性训练。";
                }
                yield "你的目标导向会影响路径侧重点与任务模板。";
            }
            case TIME_BUDGET -> {
                if ("MEDIUM_30".equals(timeBudget) || "MEDIUM_30".equals(code)) {
                    yield "你愿意投入 20~30 分钟，这适合安排一个短讲解加一个小训练的起步节奏。";
                }
                if ("SHORT_10".equals(timeBudget)) {
                    yield "你当前可投入时间较短，规划会优先安排小而完整的单次节奏。";
                }
                if ("LONG_60".equals(timeBudget) || "SYSTEMATIC".equals(timeBudget)) {
                    yield "你的时间投入可以支持更系统的一轮学习。";
                }
                yield "你的时间投入将用于安排单次任务量与节奏。";
            }
            case TOPIC_CORE -> {
                if ("PARTLY_CLEAR".equals(topicCore) || "PARTLY_CLEAR".equals(code)) {
                    yield "你对核心概念不是完全陌生，但还不稳定，因此第一步不适合直接跳到复杂应用。";
                }
                if ("NOT_CLEAR".equals(topicCore)) {
                    yield "你对核心概念还不太清楚，系统会从结构和定义带你起步。";
                }
                if ("CLEAR".equals(topicCore) || "VERY_CLEAR".equals(topicCore)) {
                    yield "你对核心概念已有把握，可更快进入应用与练习。";
                }
                yield "你对主题概念的把握会影响起步难度。";
            }
            case TOPIC_OPERATION -> {
                if ("PROCESS_CONFUSION".equals(topicOp) || "PROCESS_CONFUSION".equals(code)) {
                    yield "你更容易在操作过程上混乱，因此系统会优先帮你拆清步骤，而不是只给结论。";
                }
                if ("NO_IDEA".equals(topicOp)) {
                    yield "你还不清楚从哪下手，系统会从最小步骤和示例开始。";
                }
                if ("BOUNDARY_WEAKNESS".equals(topicOp)) {
                    yield "你在边界条件上容易出错，后续会加强边界与异常场景。";
                }
                if ("EXPRESSION_WEAKNESS".equals(topicOp)) {
                    yield "你会做但表达不清，后续会加强归纳与表达。";
                }
                yield "你在主题应用上的卡点会影响讲解侧重。";
            }
            default -> "系统已根据你的回答形成当前能力判断。";
        };
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /** 仅当能追溯到某题的 sourceQuestionId 时才加入；不允许 unknown-question。 */
    private List<DiagnosisEvidenceSourceDto> buildEvidenceSourcesFromSnapshot(
        LearnerProfileStructuredSnapshotDto snapshot,
        Map<DiagnosisDimension, DiagnosisQuestion> questionByDimension,
        boolean strength
    ) {
        List<DiagnosisEvidenceSourceDto> sources = new ArrayList<>();
        if (snapshot == null || questionByDimension == null) return sources;
        List<EvidenceItem> items = strength ? snapshotStrengthItems(snapshot) : snapshotWeaknessItems(snapshot);
        for (EvidenceItem item : items) {
            DiagnosisQuestion question = questionByDimension.get(item.dimension);
            if (question == null) continue;
            sources.add(new DiagnosisEvidenceSourceDto(item.label, item.dimension.name(), question.questionId()));
        }
        return sources;
    }

    private List<EvidenceItem> snapshotStrengthItems(LearnerProfileStructuredSnapshotDto snapshot) {
        List<EvidenceItem> out = new ArrayList<>();
        String foundation = nullToEmpty(snapshot.foundationLevel());
        String practice = nullToEmpty(snapshot.practiceLevel());
        String goalType = nullToEmpty(snapshot.goalType());
        String preference = nullToEmpty(snapshot.learningPreference());
        String timeBudget = nullToEmpty(snapshot.timeBudget());
        if ("ADVANCED".equals(foundation) || "PROFICIENT".equals(foundation)) {
            out.add(new EvidenceItem(DiagnosisDimension.FOUNDATION, "基础相对扎实，可以更快进入综合应用。"));
        }
        if ("MANY".equals(practice) && !"BEGINNER".equals(foundation)) {
            out.add(new EvidenceItem(DiagnosisDimension.PRACTICE, "已有较多练习或实际使用经验，适合结合案例推进。"));
        }
        if ("INTERVIEW".equals(goalType) || "EXAM".equals(goalType)) {
            out.add(new EvidenceItem(DiagnosisDimension.GOAL, "目标清晰（面试/考试导向），便于安排针对性训练。"));
        } else if ("PROJECT".equals(goalType)) {
            out.add(new EvidenceItem(DiagnosisDimension.GOAL, "以项目或实践为导向，适合按场景推进。"));
        } else if (!goalType.isBlank()) {
            out.add(new EvidenceItem(DiagnosisDimension.GOAL, "学习目标明确，便于安排个性化内容。"));
        }
        if (!preference.isBlank()) {
            out.add(new EvidenceItem(DiagnosisDimension.PREFERENCE, "已选择学习偏好，后续讲解与练习会按此调整。"));
        }
        if (!timeBudget.isBlank() && !"SHORT_10".equals(timeBudget)) {
            out.add(new EvidenceItem(DiagnosisDimension.TIME_BUDGET, "时间投入较充足，可支持更完整的学习节奏。"));
        }
        return out;
    }

    private List<EvidenceItem> snapshotWeaknessItems(LearnerProfileStructuredSnapshotDto snapshot) {
        List<EvidenceItem> out = new ArrayList<>();
        String foundation = nullToEmpty(snapshot.foundationLevel());
        String blocker = nullToEmpty(snapshot.primaryBlocker());
        String practice = nullToEmpty(snapshot.practiceLevel());
        List<String> riskTags = snapshot.riskTags();
        if ("BEGINNER".equals(foundation)) {
            out.add(new EvidenceItem(DiagnosisDimension.FOUNDATION, "当前基础还不够稳定，需要先补齐关键概念。"));
        }
        if ("NONE".equals(practice)) {
            out.add(new EvidenceItem(DiagnosisDimension.PRACTICE, "相关练习较少，起步阶段更需要例子和分步练习。"));
        }
        if ("FOLLOW_BUT_CANNOT_DO".equals(blocker)) {
            out.add(new EvidenceItem(DiagnosisDimension.BLOCKER, "看懂例子但独立完成还不足，需要更多从模仿到独立的练习。"));
        }
        if (riskTags != null) {
            if (riskTags.contains("TRANSFER_WEAKNESS")) {
                out.add(new EvidenceItem(DiagnosisDimension.BLOCKER, "变形与迁移还不稳定，需要针对性巩固。"));
            }
            if (riskTags.contains("EXPRESSION_WEAKNESS")) {
                out.add(new EvidenceItem(DiagnosisDimension.BLOCKER, "表达与归纳还需加强。"));
            }
            if (riskTags.contains("INTERVIEW_FOUNDATION_RISK")) {
                out.add(new EvidenceItem(DiagnosisDimension.FOUNDATION, "面试目标下基础尚需扎牢，建议先稳核心再刷题。"));
            }
            if (riskTags.contains("PROCESS_CONFUSION")) {
                out.add(new EvidenceItem(DiagnosisDimension.TOPIC_OPERATION, "操作步骤容易混淆，需要先理清流程再练。"));
            }
            if (riskTags.contains("INDEPENDENT_SOLVING_WEAKNESS")
                && out.stream().noneMatch(e -> e.label().contains("模仿到独立"))) {
                out.add(new EvidenceItem(DiagnosisDimension.BLOCKER, "独立解题还不足，会多安排从模仿到独立的练习。"));
            }
            if (riskTags.contains("EXAM_ORIENTED_SURFACE_LEARNING_RISK")) {
                out.add(new EvidenceItem(DiagnosisDimension.GOAL, "考试导向下建议先稳概念再刷题，避免只记套路。"));
            }
            if (riskTags.contains("CONCEPT_NOT_STABLE")) {
                out.add(new EvidenceItem(DiagnosisDimension.FOUNDATION, "核心概念还不稳，建议先巩固定义与结构。"));
            }
            if (riskTags.contains("BOUNDARY_WEAKNESS")) {
                out.add(new EvidenceItem(DiagnosisDimension.TOPIC_OPERATION, "边界与特殊情况容易出错，后续会加强这类练习。"));
            }
        }
        return out;
    }

    private record EvidenceItem(DiagnosisDimension dimension, String label) {}

    private String resolveSelectedAnswerLabel(DiagnosisAnswer answer, DiagnosisQuestion question) {
        if (answer.getRawText() != null && !answer.getRawText().isBlank()) {
            return answer.getRawText().trim();
        }
        if (question == null || answer.getAnswerValueJson() == null) {
            return FALLBACK_SELECTED_ANSWER;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(answer.getAnswerValueJson());
            if (jsonNode == null || jsonNode.isNull()) {
                return FALLBACK_SELECTED_ANSWER;
            }
            if (jsonNode.isArray()) {
                List<String> labels = new ArrayList<>();
                for (JsonNode item : jsonNode) {
                    labels.add(resolveOptionLabel(question, item.asText("")));
                }
                return labels.isEmpty() ? FALLBACK_SELECTED_ANSWER : String.join(" | ", labels);
            }
            return resolveOptionLabel(question, jsonNode.asText(""));
        } catch (Exception ex) {
            return FALLBACK_SELECTED_ANSWER;
        }
    }

    private String resolveOptionLabel(DiagnosisQuestion question, String code) {
        if (question == null) return fallbackText(code, FALLBACK_SELECTED_ANSWER);
        for (DiagnosisQuestionOption option : question.options()) {
            if (option.code().equalsIgnoreCase(code)) {
                return option.label();
            }
        }
        return fallbackText(code, FALLBACK_SELECTED_ANSWER);
    }

    private String fallbackText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    public record DiagnosisExplanation(
        List<DiagnosisReasoningStepDto> reasoningSteps,
        List<DiagnosisEvidenceSourceDto> strengthSources,
        List<DiagnosisEvidenceSourceDto> weaknessSources
    ) {
    }
}
