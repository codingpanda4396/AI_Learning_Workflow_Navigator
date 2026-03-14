package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.EvidenceLevel;
import com.pandanav.learning.domain.enums.GoalOrientation;
import com.pandanav.learning.domain.enums.MotivationRisk;
import com.pandanav.learning.domain.enums.PacePreference;
import com.pandanav.learning.domain.enums.PreferredLearningMode;
import com.pandanav.learning.domain.model.LearnerStateSnapshot;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class DefaultLearnerStateInterpreter implements LearnerStateInterpreter {

    private static final Logger log = LoggerFactory.getLogger(DefaultLearnerStateInterpreter.class);

    @Override
    public LearnerStateSnapshot interpret(LearningPlanPlanningContext context) {
        GoalOrientation goalOrientation = resolveGoalOrientation(context);
        PreferredLearningMode preferredLearningMode = resolvePreferredLearningMode(context);
        PacePreference pacePreference = resolvePacePreference(context);
        EvidenceLevel evidenceLevel = resolveEvidenceLevel(context);
        CurrentBlockType currentBlockType = resolveCurrentBlockType(context, evidenceLevel);
        MotivationRisk motivationRisk = resolveMotivationRisk(context, evidenceLevel, currentBlockType, pacePreference);
        String confidenceReasonSummary = buildConfidenceReasonSummary(context, evidenceLevel);
        String primaryBlockDescription = resolvePrimaryBlockDescription(currentBlockType);
        String secondaryBlockDescription = resolveSecondaryBlockDescription(context, currentBlockType);

        LearnerStateSnapshot snapshot = new LearnerStateSnapshot(
            goalOrientation,
            preferredLearningMode,
            pacePreference,
            currentBlockType,
            evidenceLevel,
            motivationRisk,
            confidenceReasonSummary,
            primaryBlockDescription,
            secondaryBlockDescription
        );
        log.info(
            "LearnerState interpreted. goalOrientation={} currentBlockType={} evidenceLevel={} motivationRisk={}",
            snapshot.goalOrientation(),
            snapshot.currentBlockType(),
            snapshot.evidenceLevel(),
            snapshot.motivationRisk()
        );
        return snapshot;
    }

    private GoalOrientation resolveGoalOrientation(LearningPlanPlanningContext context) {
        String corpus = (safe(context.goalText()) + " " + safe(context.learnerProfileSummary())).toLowerCase(Locale.ROOT);
        if (containsAny(corpus, "考试", "应试", "冲刺", "真题", "刷题", "score", "exam")) {
            return GoalOrientation.EXAM_PREP;
        }
        if (containsAny(corpus, "快速", "速成", "马上", "尽快", "quick", "asap", "上手")) {
            return GoalOrientation.QUICK_START;
        }
        if (containsAny(corpus, "查漏", "补缺", "复习", "回顾", "纠错", "review", "fix")) {
            return GoalOrientation.REVIEW_FIX;
        }
        return GoalOrientation.UNDERSTAND_PRINCIPLE;
    }

    private PreferredLearningMode resolvePreferredLearningMode(LearningPlanPlanningContext context) {
        if (context.adjustments() == null || context.adjustments().learningMode() == null) {
            return PreferredLearningMode.UNKNOWN;
        }
        String mode = context.adjustments().learningMode().toUpperCase(Locale.ROOT);
        return switch (mode) {
            case "LEARN_THEN_PRACTICE" -> PreferredLearningMode.LEARN_THEN_PRACTICE;
            case "PRACTICE_DRIVEN", "PRACTICE_FIRST" -> PreferredLearningMode.PRACTICE_THEN_LEARN;
            case "MIXED" -> PreferredLearningMode.MIXED;
            default -> PreferredLearningMode.UNKNOWN;
        };
    }

    private PacePreference resolvePacePreference(LearningPlanPlanningContext context) {
        if (context.adjustments() == null || context.adjustments().intensity() == null) {
            return PacePreference.UNKNOWN;
        }
        String intensity = context.adjustments().intensity().toUpperCase(Locale.ROOT);
        return switch (intensity) {
            case "LIGHT" -> PacePreference.LIGHT;
            case "INTENSIVE" -> PacePreference.INTENSIVE;
            case "STANDARD", "NORMAL" -> PacePreference.NORMAL;
            default -> PacePreference.UNKNOWN;
        };
    }

    private EvidenceLevel resolveEvidenceLevel(LearningPlanPlanningContext context) {
        int attemptCount = context.nodes() == null ? 0 : context.nodes().stream()
            .map(LearningPlanContextNode::attemptCount)
            .filter(item -> item != null)
            .mapToInt(Integer::intValue)
            .sum();
        int scoreCount = context.recentScores() == null ? 0 : context.recentScores().size();
        int weakCount = context.weakPointLabels() == null ? 0 : context.weakPointLabels().size();
        if (attemptCount >= 6 && scoreCount >= 3 && weakCount >= 1) {
            return EvidenceLevel.HIGH;
        }
        if (attemptCount >= 2 || scoreCount >= 2 || weakCount >= 1) {
            return EvidenceLevel.MEDIUM;
        }
        return EvidenceLevel.LOW;
    }

    private CurrentBlockType resolveCurrentBlockType(LearningPlanPlanningContext context, EvidenceLevel evidenceLevel) {
        if (evidenceLevel == EvidenceLevel.LOW) {
            return CurrentBlockType.EVIDENCE_LOW;
        }

        int foundationSignals = 0;
        int conceptSignals = 0;
        int applicationSignals = 0;
        List<LearningPlanContextNode> nodes = context.nodes() == null ? List.of() : context.nodes();

        for (LearningPlanContextNode node : nodes) {
            int mastery = node.mastery() == null ? 55 : node.mastery();
            int attempts = node.attemptCount() == null ? 0 : node.attemptCount();
            boolean hasPrerequisite = node.prerequisiteNodeIds() != null && !node.prerequisiteNodeIds().isEmpty();
            boolean conceptTag = hasConceptLinkSignal(node);
            boolean applicationTag = hasApplicationSignal(node);

            if ((node.orderNo() != null && node.orderNo() <= 2 && mastery < 55) || hasWeakReason(node, "PREREQUISITE_GAP", "LOW_MASTERY")) {
                foundationSignals++;
            }
            if (conceptTag || (hasPrerequisite && mastery >= 45 && mastery <= 70 && attempts >= 1)) {
                conceptSignals++;
            }
            if (applicationTag || (mastery >= 60 && attempts >= 2 && lowRecentScore(context.recentScores()))) {
                applicationSignals++;
            }
        }

        int activeTypes = countPositive(foundationSignals, conceptSignals, applicationSignals);
        int maxSignal = Math.max(foundationSignals, Math.max(conceptSignals, applicationSignals));
        int secondSignal = secondLargest(foundationSignals, conceptSignals, applicationSignals);
        if (activeTypes >= 2 && (maxSignal - secondSignal) <= 0) {
            return CurrentBlockType.MIXED;
        }
        if (foundationSignals > 0) {
            return CurrentBlockType.FOUNDATION_GAP;
        }
        if (applicationSignals > 0) {
            return CurrentBlockType.APPLICATION_GAP;
        }
        if (conceptSignals > 0) {
            return CurrentBlockType.CONCEPT_LINK_GAP;
        }
        return CurrentBlockType.EVIDENCE_LOW;
    }

    private MotivationRisk resolveMotivationRisk(
        LearningPlanPlanningContext context,
        EvidenceLevel evidenceLevel,
        CurrentBlockType blockType,
        PacePreference pacePreference
    ) {
        int riskScore = 0;
        if (evidenceLevel == EvidenceLevel.LOW) {
            riskScore += 2;
        }
        if (pacePreference == PacePreference.INTENSIVE) {
            riskScore += 1;
        }
        if (blockType == CurrentBlockType.FOUNDATION_GAP || blockType == CurrentBlockType.APPLICATION_GAP || blockType == CurrentBlockType.MIXED) {
            riskScore += 1;
        }
        if (context.nodes() != null && context.nodes().size() >= 4) {
            riskScore += 1;
        }
        if (riskScore >= 4) {
            return MotivationRisk.HIGH;
        }
        if (riskScore >= 2) {
            return MotivationRisk.MEDIUM;
        }
        return MotivationRisk.LOW;
    }

    private String buildConfidenceReasonSummary(LearningPlanPlanningContext context, EvidenceLevel evidenceLevel) {
        int scoreCount = context.recentScores() == null ? 0 : context.recentScores().size();
        int weakCount = context.weakPointLabels() == null ? 0 : context.weakPointLabels().size();
        int attempts = context.nodes() == null ? 0 : context.nodes().stream()
            .map(LearningPlanContextNode::attemptCount)
            .filter(item -> item != null)
            .mapToInt(Integer::intValue)
            .sum();
        return switch (evidenceLevel) {
            case HIGH -> "近期练习与薄弱点证据较完整（尝试次数 " + attempts + "，得分记录 " + scoreCount + "），当前推荐可信度较高。";
            case MEDIUM -> "已有部分学习证据（薄弱点 " + weakCount + " 项，得分记录 " + scoreCount + "），推荐可信度中等并会继续校准。";
            case LOW -> "当前可用证据较少，系统会先采用低风险起步策略，并在首轮后根据行为数据快速提速或回补。";
        };
    }

    private String resolvePrimaryBlockDescription(CurrentBlockType currentBlockType) {
        return switch (currentBlockType) {
            case FOUNDATION_GAP -> "你当前主要卡在前置基础不稳，导致后续节点容易反复卡住。";
            case CONCEPT_LINK_GAP -> "你已经接触过核心概念，但关键连接还不稳定，迁移到下一步时容易断链。";
            case APPLICATION_GAP -> "你对概念有一定理解，但在应用和训练阶段还没有形成稳定输出。";
            case MIXED -> "你当前同时存在基础与应用层面的阻塞，需要先做一轮低风险收敛。";
            case EVIDENCE_LOW -> "目前可用学习证据有限，系统先给出稳健路径并等待更多反馈。";
        };
    }

    private String resolveSecondaryBlockDescription(LearningPlanPlanningContext context, CurrentBlockType currentBlockType) {
        List<String> details = new ArrayList<>();
        if (context.recentErrorTags() != null && !context.recentErrorTags().isEmpty()) {
            details.add("近期错误标签集中在 " + String.join("、", context.recentErrorTags().stream().limit(2).toList()));
        }
        if (context.weakPointLabels() != null && !context.weakPointLabels().isEmpty()) {
            details.add("薄弱点包括 " + String.join("、", context.weakPointLabels().stream().limit(2).toList()));
        }
        if (details.isEmpty() || currentBlockType == CurrentBlockType.EVIDENCE_LOW) {
            return null;
        }
        return String.join("；", details) + "。";
    }

    private boolean hasConceptLinkSignal(LearningPlanContextNode node) {
        return containsAnyTags(node.recentErrorTags(), "CONFUSION", "LINK", "DEPENDENCY", "RELATION")
            || containsAnyTags(node.weakReasons(), "CONCEPT", "DEPENDENCY");
    }

    private boolean hasApplicationSignal(LearningPlanContextNode node) {
        return containsAnyTags(node.recentErrorTags(), "APPLICATION", "PRACTICE", "TRANSFER", "IMPLEMENT")
            || containsAnyTags(node.weakReasons(), "APPLICATION", "PRACTICE");
    }

    private boolean hasWeakReason(LearningPlanContextNode node, String... targets) {
        if (node.weakReasons() == null || node.weakReasons().isEmpty()) {
            return false;
        }
        for (String reason : node.weakReasons()) {
            if (reason == null) {
                continue;
            }
            String upper = reason.toUpperCase(Locale.ROOT);
            for (String target : targets) {
                if (upper.contains(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsAnyTags(List<String> values, String... targets) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String upper = value.toUpperCase(Locale.ROOT);
            for (String target : targets) {
                if (upper.contains(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean lowRecentScore(List<Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return false;
        }
        double average = scores.stream().mapToInt(Integer::intValue).average().orElse(100D);
        return average < 60D;
    }

    private int countPositive(int... values) {
        int count = 0;
        for (int value : values) {
            if (value > 0) {
                count++;
            }
        }
        return count;
    }

    private int secondLargest(int a, int b, int c) {
        int largest = Integer.MIN_VALUE;
        int second = Integer.MIN_VALUE;
        for (int value : new int[] {a, b, c}) {
            if (value > largest) {
                second = largest;
                largest = value;
            } else if (value > second) {
                second = value;
            }
        }
        return second == Integer.MIN_VALUE ? 0 : second;
    }

    private boolean containsAny(String text, String... parts) {
        for (String part : parts) {
            if (text.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
