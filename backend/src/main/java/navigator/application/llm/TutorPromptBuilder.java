package navigator.application.llm;

import navigator.application.task.TaskExecutionContext;
import navigator.domain.enums.GuidanceIntent;
import navigator.domain.model.GuidanceDecision;
import navigator.domain.model.TaskExecutionEvidenceSnapshot;
import navigator.application.task.guidance.TaskExecutionEvidenceAccumulator;
import navigator.application.task.TaskExecutionRuntime;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 将规则决策裁剪为 LLM system prompt（结构化分段）。
 */
@Component
public class TutorPromptBuilder {

    public String buildExploreSystemPrompt(TaskExecutionContext ctx,
                                           TaskExecutionRuntime rt,
                                           GuidanceDecision decision,
                                           String goal,
                                           String completionCriteria,
                                           String boundaryLine) {
        TaskExecutionEvidenceSnapshot ev = TaskExecutionEvidenceAccumulator.ensureSnapshot(rt);
        String userState = String.format(
                "explore_turns=%d; active_questions=%d; direct_answer_seeks=%d; vague_replies=%d; confusion=%d; dependency_score=%.2f",
                ev.getTotalTurns(),
                ev.getUserInitiatedQuestionTurns(),
                ev.getDirectAnswerSeekCount(),
                ev.getVagueUserReplyCount(),
                ev.getConfusionSignalsCount(),
                ev.getDirectAnswerDependencyScore());

        GuidanceIntent intent = decision != null ? decision.getIntent() : GuidanceIntent.ASK_CLARIFYING_QUESTION;
        boolean allowAnswer = decision != null && decision.isAllowSubstantiveAnswer();
        String mandatory = decision != null && decision.getMandatoryBehaviors() != null
                ? decision.getMandatoryBehaviors().stream().collect(Collectors.joining("; "))
                : "";

        String slots = "";
        if (decision != null && decision.getPromptSlots() != null && !decision.getPromptSlots().isEmpty()) {
            slots = decision.getPromptSlots().entrySet().stream()
                    .filter(e -> e.getValue() != null && !e.getValue().isBlank() && !"forced_reply".equals(e.getKey()))
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));
        }

        return "STAGE_NAME=EXECUTION/EXPLORE\n"
                + "GUIDANCE_PHASE=" + (decision != null && decision.getPhase() != null ? decision.getPhase().name() : "") + "\n"
                + "OBJECTIVE=" + (goal != null ? goal : "") + "\n"
                + (completionCriteria.isBlank() ? "" : "COMPLETION_CRITERIA=" + completionCriteria + "\n")
                + "USER_STATE=" + userState + "\n"
                + "DECISION_INTENT=" + intent.name() + "\n"
                + (mandatory.isBlank() ? "" : "MANDATORY=" + mandatory + "\n")
                + (slots.isBlank() ? "" : "SLOTS=\n" + slots + "\n")
                + "BOUNDARY=" + boundaryLine + "\n"
                + (allowAnswer
                ? "ALLOWED=short_explanation_with_one_minimal_example\n"
                : "FORBIDDEN=final_answer,complete_solution,copy_paste_code\n")
                + "OUTPUT_FORMAT=plain_text; max 2 short paragraphs; prefer 1-2 guiding questions then at most one tiny hint.\n";
    }
}
