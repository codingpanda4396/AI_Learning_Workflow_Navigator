package navigator.application.learning;

import navigator.application.task.TaskExecutionRuntime;
import navigator.application.task.guidance.TaskExecutionEvidenceAccumulator;
import navigator.domain.enums.LearningActionType;
import navigator.domain.model.LearningEvidence;
import navigator.domain.model.TaskExecutionEvidenceSnapshot;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class LearningEvidenceBuilder {

    private static final Pattern ATTEMPTED_EXPLAIN = Pattern.compile(
            "我觉得|我理解|我的理解|是不是|也就是说|总结一下|in my understanding",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern KEY_CONCEPT = Pattern.compile(
            "定义|概念|关键|本质|因为|所以|步骤|关系|区别|原理|公式|条件|definition|concept|because|therefore",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MISCONCEPTION = Pattern.compile(
            "是不是就是|等于说|完全一样|直接套公式|照背|i guess it is exactly",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ASKING_FOR_ANSWER = Pattern.compile(
            "答案|直接说|直接给|告诉我|标准答案|完整代码|直接做完|answer",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CONFUSED_SIGNAL = Pattern.compile(
            "不懂|不会|不明白|看不懂|还是懵|没思路|confused|lost",
            Pattern.CASE_INSENSITIVE);

    public LearningEvidence build(String userInput) {
        return build(null, null, userInput);
    }

    public LearningEvidence build(TaskExecutionRuntime runtime, LearningActionType action, String userInput) {
        String content = userInput == null ? "" : userInput.trim();
        String normalized = content.toLowerCase(Locale.ROOT);
        TaskExecutionEvidenceSnapshot snapshot = runtime != null
                ? TaskExecutionEvidenceAccumulator.ensureSnapshot(runtime)
                : TaskExecutionEvidenceSnapshot.builder().build();

        boolean attemptedExplain = action == LearningActionType.SELF_EXPLANATION
                || ATTEMPTED_EXPLAIN.matcher(content).find()
                || snapshot.isSelfExplanationSubmitted();
        boolean containsKeyConcept = KEY_CONCEPT.matcher(content).find()
                || content.length() >= 24 && (content.contains("因为") || content.contains("所以"));
        boolean hasMisconception = MISCONCEPTION.matcher(content).find()
                || attemptedExplain && !containsKeyConcept && content.length() >= 12 && !content.endsWith("?");
        boolean askingForAnswer = action == LearningActionType.SEEK_DIRECT_ANSWER
                || ASKING_FOR_ANSWER.matcher(content).find();
        boolean confusedSignal = action == LearningActionType.CONFUSION_SIGNAL
                || CONFUSED_SIGNAL.matcher(content).find();
        int interactionDepth = snapshot.getTotalTurns() + (content.isBlank() ? 0 : 1);

        if (!normalized.isBlank() && normalized.contains("example")) {
            containsKeyConcept = true;
        }

        return LearningEvidence.builder()
                .attemptedExplain(attemptedExplain)
                .containsKeyConcept(containsKeyConcept)
                .hasMisconception(hasMisconception)
                .askingForAnswer(askingForAnswer)
                .confusedSignal(confusedSignal)
                .interactionDepth(interactionDepth)
                .build();
    }
}
