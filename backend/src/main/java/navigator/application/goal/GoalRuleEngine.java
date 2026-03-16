package navigator.application.goal;

import navigator.domain.enums.GoalType;
import navigator.domain.enums.SelfReportedLevel;
import navigator.domain.enums.TimeBudget;
import navigator.domain.model.LearningGoalInput;
import navigator.domain.model.StructuredLearningGoal;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Sprint 1: LearningGoalInput -> StructuredLearningGoal 规则引擎（规则 1～5）。
 */
@Component
public class GoalRuleEngine {

    private static final Pattern EXAM_KEYWORDS = Pattern.compile("考试|期末|明天|复习|冲刺");
    private static final Pattern BLOCKER_KEYWORDS = Pattern.compile("不会|卡住|看不懂|一直错|阻塞|某题不会");
    private static final Pattern PRACTICE_KEYWORDS = Pattern.compile("刷题|做题|练习|提高正确率");
    private static final Pattern SYSTEMATIC_KEYWORDS = Pattern.compile("系统|全面|整体|框架|408|一轮");
    private static final Pattern URGENT_KEYWORDS = Pattern.compile("明天|今晚|马上|赶紧");
    private static final Pattern DATA_STRUCTURE_TOPICS = Pattern.compile("链表|栈|队列|树|图|排序|查找");
    private static final Pattern COMPUTER_408 = Pattern.compile("408");

    public StructuredLearningGoal derive(LearningGoalInput input) {
        if (input == null) {
            input = LearningGoalInput.builder().rawGoalText("").build();
        }
        String raw = input.getRawGoalText() != null ? input.getRawGoalText() : "";
        List<String> topics = GoalKeywordExtractor.extractTopics(raw, input.getTopicHints());
        GoalType goalType = deriveGoalType(input, raw);
        String subject = deriveSubject(input, raw);
        String topicScopeType = deriveTopicScopeType(input, raw, topics);
        String urgencyLevel = deriveUrgencyLevel(input, raw);
        String expectedDepth = deriveExpectedDepth(goalType, input);
        String normalizedGoalText = buildNormalizedGoalText(goalType, topics);
        String intentDescription = buildIntentDescription(goalType, topicScopeType, topics);

        return StructuredLearningGoal.builder()
                .rawGoalText(raw)
                .normalizedGoalText(normalizedGoalText)
                .goalType(goalType)
                .subject(subject)
                .topicScopeType(topicScopeType)
                .topics(topics)
                .intentDescription(intentDescription)
                .timeBudget(input.getTimeBudget())
                .urgencyLevel(urgencyLevel)
                .expectedDepth(expectedDepth)
                .selfReportedLevel(input.getSelfReportedLevel())
                .preferenceTags(input.getPreferenceTags())
                .constraints(List.of())
                .sourceContext(input.getSourceContext())
                .build();
    }

    private GoalType deriveGoalType(LearningGoalInput input, String raw) {
        if (input.getGoalTypeHint() != null) {
            return input.getGoalTypeHint();
        }
        if (EXAM_KEYWORDS.matcher(raw).find()) return GoalType.REVIEW_FOR_EXAM;
        if (BLOCKER_KEYWORDS.matcher(raw).find()) return GoalType.FIX_SPECIFIC_BLOCKER;
        if (PRACTICE_KEYWORDS.matcher(raw).find()) return GoalType.PRACTICE_ENHANCEMENT;
        if (SYSTEMATIC_KEYWORDS.matcher(raw).find()) return GoalType.BUILD_SYSTEMATIC_UNDERSTANDING;
        return GoalType.LEARN_NEW_CONCEPT;
    }

    private String deriveSubject(LearningGoalInput input, String raw) {
        if (input.getSubjectHint() != null && !input.getSubjectHint().isBlank()) {
            return input.getSubjectHint();
        }
        if (COMPUTER_408.matcher(raw).find()) return "COMPUTER_408";
        if (DATA_STRUCTURE_TOPICS.matcher(raw).find()) return "DATA_STRUCTURE";
        return "GENERAL_CS";
    }

    private String deriveTopicScopeType(LearningGoalInput input, String raw, List<String> topics) {
        if (GoalKeywordExtractor.isChapterScope(raw)) return "CHAPTER";
        if (GoalKeywordExtractor.isCourseScope(raw)) return "COURSE";
        if (topics.size() == 1) return "SINGLE_TOPIC";
        if (topics.size() >= 2 && topics.size() <= 4) return "MULTI_TOPIC";
        return "SINGLE_TOPIC";
    }

    private String deriveUrgencyLevel(LearningGoalInput input, String raw) {
        if (URGENT_KEYWORDS.matcher(raw).find()) return "HIGH";
        TimeBudget budget = input.getTimeBudget();
        if (budget == null) return "MEDIUM";
        switch (budget) {
            case WITHIN_15_MIN:
            case WITHIN_30_MIN:
                return "HIGH";
            case WITHIN_60_MIN:
            case MULTI_DAY:
                return "MEDIUM";
            case LONG_TERM:
                return "LOW";
            default:
                return "MEDIUM";
        }
    }

    private String deriveExpectedDepth(GoalType goalType, LearningGoalInput input) {
        TimeBudget budget = input.getTimeBudget();
        boolean shortTime = budget == TimeBudget.WITHIN_15_MIN || budget == TimeBudget.WITHIN_30_MIN;
        switch (goalType) {
            case REVIEW_FOR_EXAM:
                return shortTime ? "APPLY_BASIC" : "UNDERSTAND_CORE_IDEA";
            case PRACTICE_ENHANCEMENT:
                return "SOLVE_TYPICAL_PROBLEMS";
            case BUILD_SYSTEMATIC_UNDERSTANDING:
                return "SYSTEMATIC_MASTERY";
            case FIX_SPECIFIC_BLOCKER:
                return "UNDERSTAND_CORE_IDEA";
            default:
                return "UNDERSTAND_CORE_IDEA";
        }
    }

    private String buildNormalizedGoalText(GoalType goalType, List<String> topics) {
        String typeLabel = goalType.name();
        String topicStr = topics.isEmpty() ? "未指定" : String.join("、", topics);
        return typeLabel + "：" + topicStr;
    }

    private String buildIntentDescription(GoalType goalType, String topicScopeType, List<String> topics) {
        String topicStr = topics.isEmpty() ? "相关主题" : String.join("、", topics);
        return "用户希望在有限时间内围绕" + topicStr + "建立可做题的基本理解";
    }
}
