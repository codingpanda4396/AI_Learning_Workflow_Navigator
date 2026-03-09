package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pandanav.learning.domain.enums.Stage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PromptOutputValidator {

    public List<String> validateStage(Stage stage, JsonNode node) {
        List<String> errors = new ArrayList<>();
        if (node == null || !node.isObject()) {
            errors.add("stage output must be a JSON object");
            return errors;
        }
        Set<String> required = switch (stage) {
            case STRUCTURE -> Set.of("title", "summary", "key_points", "common_misconceptions", "suggested_sequence");
            case UNDERSTANDING -> Set.of("concept_explanation", "analogy", "worked_example", "step_by_step_reasoning", "common_errors", "check_questions");
            case TRAINING -> Set.of("questions");
            case REFLECTION -> Set.of("reflection_prompt", "review_checklist", "next_step_suggestion");
        };
        validateNoExtraFields(node, required, errors);

        for (String field : required) {
            if (!node.has(field)) {
                errors.add("missing field: " + field);
            }
        }

        switch (stage) {
            case STRUCTURE -> {
                validateText(node, "title", 8, 30, errors);
                validateText(node, "summary", 40, 120, errors);
                validateStringArray(node, "key_points", 3, 5, 10, 40, errors);
                validateStringArray(node, "common_misconceptions", 2, 3, 10, 40, errors);
                validateStringArray(node, "suggested_sequence", 3, 5, 8, 30, errors);
            }
            case UNDERSTANDING -> {
                validateText(node, "concept_explanation", 80, 220, errors);
                validateText(node, "analogy", 40, 120, errors);
                validateText(node, "worked_example", 80, 220, errors);
                validateStringArray(node, "step_by_step_reasoning", 3, 5, 12, 45, errors);
                validateStringArray(node, "common_errors", 2, 4, 10, 40, errors);
                validateStringArray(node, "check_questions", 2, 4, 12, 45, errors);
            }
            case TRAINING -> validateTrainingQuestions(node.path("questions"), errors);
            case REFLECTION -> {
                validateText(node, "reflection_prompt", 30, 120, errors);
                validateStringArray(node, "review_checklist", 3, 5, 10, 35, errors);
                validateText(node, "next_step_suggestion", 30, 120, errors);
            }
        }
        return errors;
    }

    public List<String> validateEvaluation(JsonNode node) {
        List<String> errors = new ArrayList<>();
        if (node == null || !node.isObject()) {
            errors.add("evaluation output must be a JSON object");
            return errors;
        }

        Set<String> required = Set.of(
            "score", "normalized_score", "rubric", "feedback", "error_tags", "strengths", "weaknesses", "suggested_next_action"
        );
        validateNoExtraFields(node, required, errors);

        Integer score = readInt(node, "score", errors);
        BigDecimal normalized = readDecimal(node, "normalized_score", errors);
        validateText(node, "feedback", 10, 300, errors);
        validateStringArray(node, "error_tags", 2, 4, 2, 50, errors);
        validateStringArray(node, "strengths", 2, 3, 2, 80, errors);
        validateStringArray(node, "weaknesses", 2, 3, 2, 80, errors);
        validateText(node, "suggested_next_action", 4, 50, errors);

        JsonNode rubric = node.path("rubric");
        if (!rubric.isObject()) {
            errors.add("rubric must be object");
        } else {
            int cc = readInt(rubric, "concept_correctness", errors) == null ? 0 : rubric.path("concept_correctness").asInt();
            int rq = readInt(rubric, "reasoning_quality", errors) == null ? 0 : rubric.path("reasoning_quality").asInt();
            int cp = readInt(rubric, "completeness", errors) == null ? 0 : rubric.path("completeness").asInt();
            int cl = readInt(rubric, "clarity", errors) == null ? 0 : rubric.path("clarity").asInt();
            if (cc < 0 || cc > 40) {
                errors.add("rubric.concept_correctness must be 0-40");
            }
            if (rq < 0 || rq > 30) {
                errors.add("rubric.reasoning_quality must be 0-30");
            }
            if (cp < 0 || cp > 20) {
                errors.add("rubric.completeness must be 0-20");
            }
            if (cl < 0 || cl > 10) {
                errors.add("rubric.clarity must be 0-10");
            }
            if (score != null && cc + rq + cp + cl != score) {
                errors.add("rubric sum must equal score");
            }
        }

        if (score != null && (score < 0 || score > 100)) {
            errors.add("score must be 0-100");
        }
        if (score != null && normalized != null) {
            BigDecimal expected = BigDecimal.valueOf(score).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
            if (normalized.setScale(3, RoundingMode.HALF_UP).compareTo(expected) != 0) {
                errors.add("normalized_score must equal score/100");
            }
        }
        return errors;
    }

    public void repairEvaluation(ObjectNode node) {
        Integer score = node.path("score").isNumber() ? node.path("score").asInt() : null;
        if (score != null) {
            int clamped = Math.max(0, Math.min(100, score));
            node.put("score", clamped);
            node.put("normalized_score", BigDecimal.valueOf(clamped)
                .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP)
                .doubleValue());
        }

        limitArray(node, "error_tags", 4);
        limitArray(node, "strengths", 3);
        limitArray(node, "weaknesses", 3);
    }

    public List<String> validateGoalDiagnosis(JsonNode node) {
        List<String> errors = new ArrayList<>();
        if (node == null || !node.isObject()) {
            errors.add("goal diagnosis output must be a JSON object");
            return errors;
        }
        Set<String> required = Set.of("goal_score", "smart_breakdown", "summary", "strengths", "risks", "rewritten_goal");
        validateNoExtraFields(node, required, errors);

        Integer goalScore = readInt(node, "goal_score", errors);
        validateText(node, "summary", 10, 240, errors);
        validateStringArray(node, "strengths", 2, 3, 2, 80, errors);
        validateStringArray(node, "risks", 2, 3, 2, 80, errors);
        validateText(node, "rewritten_goal", 10, 120, errors);

        JsonNode smart = node.path("smart_breakdown");
        if (!smart.isObject()) {
            errors.add("smart_breakdown must be object");
            return errors;
        }
        int specific = readBoundedInt(smart, "specific_score", 0, 20, errors);
        int measurable = readBoundedInt(smart, "measurable_score", 0, 20, errors);
        int achievable = readBoundedInt(smart, "achievable_score", 0, 20, errors);
        int relevant = readBoundedInt(smart, "relevant_score", 0, 20, errors);
        int timeBound = readBoundedInt(smart, "time_bound_score", 0, 20, errors);

        if (goalScore != null && goalScore != specific + measurable + achievable + relevant + timeBound) {
            errors.add("goal_score must equal smart_breakdown sum");
        }
        return errors;
    }

    public List<String> validatePersonalizedPathPlan(JsonNode node) {
        List<String> errors = new ArrayList<>();
        if (node == null || !node.isObject()) {
            errors.add("path plan output must be a JSON object");
            return errors;
        }

        Set<String> required = Set.of("ordered_nodes", "inserted_tasks", "plan_reasoning_summary", "risk_flags");
        validateNoExtraFields(node, required, errors);
        required.forEach(field -> {
            if (!node.has(field)) {
                errors.add("missing field: " + field);
            }
        });

        validateText(node, "plan_reasoning_summary", 20, 400, errors);
        validateStringArray(node, "risk_flags", 0, 8, 2, 80, errors);

        JsonNode orderedNodes = node.path("ordered_nodes");
        if (!orderedNodes.isArray()) {
            errors.add("ordered_nodes must be array");
        } else if (orderedNodes.isEmpty()) {
            errors.add("ordered_nodes must not be empty");
        } else {
            for (JsonNode item : orderedNodes) {
                if (!item.isObject()) {
                    errors.add("ordered_nodes entries must be object");
                    continue;
                }
                readInt(item, "node_id", errors);
                readInt(item, "priority", errors);
                validateText(item, "reason", 6, 120, errors);
                validateNoExtraFields(item, Set.of("node_id", "priority", "reason"), errors);
            }
        }

        JsonNode insertedTasks = node.path("inserted_tasks");
        if (!insertedTasks.isArray()) {
            errors.add("inserted_tasks must be array");
        } else {
            for (JsonNode item : insertedTasks) {
                if (!item.isObject()) {
                    errors.add("inserted_tasks entries must be object");
                    continue;
                }
                readInt(item, "node_id", errors);
                validateText(item, "stage", 4, 20, errors);
                validateText(item, "objective", 10, 200, errors);
                validateText(item, "trigger", 2, 50, errors);
                validateNoExtraFields(item, Set.of("node_id", "stage", "objective", "trigger"), errors);
            }
        }
        return errors;
    }

    private int readBoundedInt(JsonNode node, String field, int min, int max, List<String> errors) {
        Integer value = readInt(node, field, errors);
        if (value == null) {
            return 0;
        }
        if (value < min || value > max) {
            errors.add(field + " must be " + min + "-" + max);
        }
        return value;
    }

    private Integer readInt(JsonNode node, String field, List<String> errors) {
        JsonNode fieldNode = node.path(field);
        if (!fieldNode.isNumber()) {
            errors.add(field + " must be number");
            return null;
        }
        return fieldNode.asInt();
    }

    private BigDecimal readDecimal(JsonNode node, String field, List<String> errors) {
        JsonNode fieldNode = node.path(field);
        if (!fieldNode.isNumber()) {
            errors.add(field + " must be number");
            return null;
        }
        return fieldNode.decimalValue();
    }

    private void validateText(JsonNode node, String field, int minLen, int maxLen, List<String> errors) {
        JsonNode fieldNode = node.path(field);
        if (!fieldNode.isTextual()) {
            errors.add(field + " must be text");
            return;
        }
        String value = fieldNode.asText().trim();
        if (value.isEmpty()) {
            errors.add(field + " must not be blank");
            return;
        }
        if (value.length() < minLen || value.length() > maxLen) {
            errors.add(field + " length must be " + minLen + "-" + maxLen);
        }
    }

    private void validateStringArray(
        JsonNode node,
        String field,
        int minSize,
        int maxSize,
        int minTextLen,
        int maxTextLen,
        List<String> errors
    ) {
        JsonNode arrayNode = node.path(field);
        if (!arrayNode.isArray()) {
            errors.add(field + " must be array");
            return;
        }
        if (arrayNode.size() < minSize || arrayNode.size() > maxSize) {
            errors.add(field + " size must be " + minSize + "-" + maxSize);
        }

        for (JsonNode item : arrayNode) {
            if (!item.isTextual()) {
                errors.add(field + " entries must be text");
                continue;
            }
            String text = item.asText().trim();
            if (text.isEmpty()) {
                errors.add(field + " entries must not be blank");
                continue;
            }
            if (text.length() < minTextLen || text.length() > maxTextLen) {
                errors.add(field + " entry length must be " + minTextLen + "-" + maxTextLen);
            }
        }
    }

    private void validateTrainingQuestions(JsonNode questions, List<String> errors) {
        if (!questions.isArray()) {
            errors.add("questions must be array");
            return;
        }
        if (questions.size() < 3 || questions.size() > 5) {
            errors.add("questions size must be 3-5");
        }

        int basic = 0;
        int application = 0;
        int reasoning = 0;
        for (JsonNode question : questions) {
            if (!question.isObject()) {
                errors.add("each question must be object");
                continue;
            }
            validateText(question, "id", 2, 20, errors);
            validateText(question, "type", 4, 20, errors);
            validateText(question, "question", 20, 120, errors);
            validateStringArray(question, "reference_points", 2, 4, 8, 35, errors);
            validateText(question, "difficulty", 4, 10, errors);

            String type = question.path("type").asText("");
            if ("BASIC".equals(type)) {
                basic++;
            } else if ("APPLICATION".equals(type)) {
                application++;
            } else if ("REASONING".equals(type)) {
                reasoning++;
            }
        }
        if (basic == 0 || application == 0 || reasoning == 0) {
            errors.add("questions must include BASIC/APPLICATION/REASONING");
        }
    }

    private void validateNoExtraFields(JsonNode node, Set<String> required, List<String> errors) {
        node.fieldNames().forEachRemaining(field -> {
            if (!required.contains(field)) {
                errors.add("unexpected field: " + field);
            }
        });
    }

    private void limitArray(ObjectNode node, String field, int max) {
        if (!node.path(field).isArray()) {
            return;
        }
        ArrayNode original = (ArrayNode) node.path(field);
        if (original.size() <= max) {
            return;
        }
        ArrayNode trimmed = node.arrayNode();
        for (int i = 0; i < max; i++) {
            trimmed.add(original.get(i));
        }
        node.set(field, trimmed);
    }
}
