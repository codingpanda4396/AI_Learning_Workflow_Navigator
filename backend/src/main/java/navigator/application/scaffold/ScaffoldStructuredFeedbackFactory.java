package navigator.application.scaffold;

import navigator.api.dto.scaffold.StructuredScaffoldFeedbackPayload;
import navigator.api.dto.scaffold.TrainingDetectedProblem;
import navigator.api.dto.scaffold.TrainingFeedback;
import navigator.api.dto.scaffold.TutorResponse;
import navigator.api.dto.scaffold.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 将校验与 tutor 结果收敛为窄反馈（最多 3 条 issue），供前端稳定渲染。
 */
public final class ScaffoldStructuredFeedbackFactory {

    private static final int MAX_ISSUES = 3;

    private ScaffoldStructuredFeedbackFactory() {
    }

    public static StructuredScaffoldFeedbackPayload build(boolean passed,
                                                          ValidationResult validation,
                                                          TutorResponse tutor,
                                                          TrainingFeedback trainingFeedback) {
        if (passed) {
            return StructuredScaffoldFeedbackPayload.builder()
                    .completeness("当前动作已通过")
                    .issuePoints(List.of())
                    .minimalRevision(null)
                    .nextAction(tutor != null && tutor.getNextPrompt() != null ? tutor.getNextPrompt().trim() : null)
                    .build();
        }
        List<String> issues = new ArrayList<>();
        if (trainingFeedback != null && trainingFeedback.getDetectedProblems() != null) {
            for (TrainingDetectedProblem p : trainingFeedback.getDetectedProblems()) {
                if (issues.size() >= MAX_ISSUES) {
                    break;
                }
                if (p != null && p.getProblemText() != null && !p.getProblemText().isBlank()) {
                    issues.add(p.getProblemText().trim());
                }
            }
        }
        if (issues.isEmpty() && validation != null && validation.getMissingAspects() != null) {
            issues.addAll(validation.getMissingAspects().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .limit(MAX_ISSUES)
                    .collect(Collectors.toList()));
        }
        if (issues.isEmpty() && validation != null && validation.getMessage() != null && !validation.getMessage().isBlank()) {
            issues.add(validation.getMessage().trim());
        }
        if (issues.size() > MAX_ISSUES) {
            issues = issues.subList(0, MAX_ISSUES);
        }
        String minimal = null;
        if (tutor != null && tutor.getNextPrompt() != null && !tutor.getNextPrompt().isBlank()) {
            minimal = tutor.getNextPrompt().trim();
        } else if (trainingFeedback != null && trainingFeedback.getRevisionInstruction() != null) {
            minimal = trainingFeedback.getRevisionInstruction().trim();
        } else if (validation != null && validation.getSuggestions() != null && !validation.getSuggestions().isEmpty()) {
            minimal = validation.getSuggestions().get(0).trim();
        }
        String next = minimal;
        return StructuredScaffoldFeedbackPayload.builder()
                .completeness("尚未满足完成标准")
                .issuePoints(issues)
                .minimalRevision(minimal)
                .nextAction(next)
                .build();
    }
}
