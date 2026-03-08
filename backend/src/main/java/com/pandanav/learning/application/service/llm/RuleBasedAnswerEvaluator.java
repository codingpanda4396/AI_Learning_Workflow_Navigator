package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.pandanav.learning.domain.llm.AnswerEvaluator;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.EvaluationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RuleBasedAnswerEvaluator implements AnswerEvaluator {

    private static final int MIN_REASONING_LENGTH = 40;

    @Override
    public EvaluationResult evaluate(EvaluationContext context) {
        String answer = normalize(context.userAnswer());
        String objective = normalize(context.taskObjective());
        String questionContent = normalize(context.generatedQuestionContent());
        String combined = (objective + " " + questionContent).trim();

        int score = 100;
        Set<String> tags = new LinkedHashSet<>();
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        if (answer.isBlank()) {
            return emptyResult();
        }

        if (answer.length() < MIN_REASONING_LENGTH) {
            score -= 20;
            tags.add("SHALLOW_REASONING");
            weaknesses.add("答案长度偏短，论证链条不完整。");
        } else {
            strengths.add("答案提供了基本解释。");
        }

        if (isOffTopic(answer, combined)) {
            score -= 25;
            tags.add("CONCEPT_CONFUSION");
            weaknesses.add("答案与题目核心知识点偏离。");
        }

        List<String> keywords = buildKeywords(combined);
        long matched = keywords.stream().filter(answer::contains).count();
        if (matched == 0) {
            score -= 20;
            tags.add("MEMORY_GAP");
            weaknesses.add("未覆盖题目涉及的关键术语。");
        } else if (matched < Math.max(1, keywords.size() / 2)) {
            score -= 12;
            tags.add("MISSING_STEPS");
            weaknesses.add("关键点覆盖不完整。");
        } else {
            strengths.add("覆盖了主要关键点。");
        }

        if (!containsAny(answer, List.of("因此", "所以", "because", "cause"))) {
            score -= 8;
            tags.add("SHALLOW_REASONING");
            weaknesses.add("缺少因果推理表达。");
        }

        score = Math.max(0, Math.min(100, score));
        BigDecimal normalized = BigDecimal.valueOf(score)
            .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);

        String feedback = tags.isEmpty() ? "答案整体准确，建议继续保持。" : "答案存在可改进点，建议按薄弱项补强。";
        String nextAction = score < 60 ? "INSERT_REMEDIAL_UNDERSTANDING"
            : score < 80 ? "INSERT_TRAINING_VARIANTS"
            : score < 90 ? "INSERT_TRAINING_REINFORCEMENT"
            : "ADVANCE_TO_NEXT_NODE";

        JsonNode raw = JsonNodeFactory.instance.objectNode();
        return new EvaluationResult(
            score,
            normalized,
            feedback,
            List.copyOf(tags),
            strengths.isEmpty() ? List.of("有作答尝试。") : strengths,
            weaknesses,
            nextAction,
            raw,
            null,
            null,
            "rule-v1",
            null
        );
    }

    private EvaluationResult emptyResult() {
        return new EvaluationResult(
            0,
            BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
            "答案为空，无法评估有效掌握程度。",
            List.of("MEMORY_GAP"),
            List.of(),
            List.of("请先给出核心概念定义与推理过程。"),
            "INSERT_REMEDIAL_UNDERSTANDING",
            JsonNodeFactory.instance.objectNode(),
            null,
            null,
            "rule-v1",
            null
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private List<String> buildKeywords(String context) {
        List<String> source = List.of("定义", "原理", "机制", "步骤", "条件", "边界");
        return source.stream().filter(context::contains).toList();
    }

    private boolean isOffTopic(String answer, String context) {
        if (context.isBlank()) {
            return false;
        }
        return buildKeywords(context).stream().noneMatch(answer::contains);
    }

    private boolean containsAny(String answer, List<String> words) {
        return words.stream().anyMatch(answer::contains);
    }
}

