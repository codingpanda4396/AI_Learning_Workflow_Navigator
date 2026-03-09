package com.pandanav.learning.application.service.practice;

import com.pandanav.learning.domain.enums.PracticeQuestionType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RulePracticeGenerator implements PracticeGenerator {

    @Override
    public PracticeGeneratorResult generate(PracticeGeneratorRequest request) {
        String node = safe(request.nodeTitle());
        String objective = safe(request.taskObjective());

        List<PracticeDraftItem> items = List.of(
            new PracticeDraftItem(
                PracticeQuestionType.SINGLE_CHOICE,
                "Which statement best matches the core idea of \"" + node + "\" for this task?",
                List.of(
                    "Focus on definitions and boundary conditions before applying formulas.",
                    "Skip concept checks and solve by memorized tricks first.",
                    "Only summarize history without solving any example.",
                    "Treat every input case as the same regardless of constraints."
                ),
                "Focus on definitions and boundary conditions before applying formulas.",
                "This aligns with the task objective and prevents common misunderstanding.",
                "EASY"
            ),
            new PracticeDraftItem(
                PracticeQuestionType.TRUE_FALSE,
                "True or False: In this task (" + objective + "), ignoring edge cases is acceptable if the main flow works.",
                List.of("True", "False"),
                "False",
                "Edge cases are part of correctness and directly affect learning quality.",
                "MEDIUM"
            ),
            new PracticeDraftItem(
                PracticeQuestionType.SHORT_ANSWER,
                "Use 3-5 sentences to explain how you would apply \"" + node + "\" to complete this objective: " + objective,
                List.of(),
                "Answer should include definition, key steps, and one boundary/edge condition.",
                "A complete response should show concept understanding plus executable reasoning.",
                "HARD"
            )
        );

        return new PracticeGeneratorResult(
            items,
            "RULE",
            false,
            false,
            "rule-v1",
            null,
            null,
            null,
            null,
            null
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "current node" : value.trim();
    }
}
