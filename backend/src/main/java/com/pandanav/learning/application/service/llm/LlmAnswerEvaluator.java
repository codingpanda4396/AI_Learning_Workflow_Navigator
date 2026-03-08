package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.llm.AnswerEvaluator;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.EvaluationResult;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class LlmAnswerEvaluator implements AnswerEvaluator {

    private final LlmGateway llmGateway;
    private final PromptTemplateProvider promptTemplateProvider;
    private final LlmJsonParser llmJsonParser;

    public LlmAnswerEvaluator(
        LlmGateway llmGateway,
        PromptTemplateProvider promptTemplateProvider,
        LlmJsonParser llmJsonParser
    ) {
        this.llmGateway = llmGateway;
        this.promptTemplateProvider = promptTemplateProvider;
        this.llmJsonParser = llmJsonParser;
    }

    @Override
    public EvaluationResult evaluate(EvaluationContext context) {
        LlmPrompt prompt = promptTemplateProvider.buildEvaluationPrompt(PromptTemplateKey.EVALUATE_PROMPT_V1, context);
        LlmTextResult result = llmGateway.generate(prompt);
        JsonNode parsed = llmJsonParser.parse(result.text());
        Integer score = parsed.path("score").isNumber() ? parsed.path("score").asInt() : null;
        BigDecimal normalized = readNormalized(parsed, score);
        if (score == null || normalized == null) {
            throw new InternalServerException("LLM evaluation output missing score fields.");
        }
        return new EvaluationResult(
            score,
            normalized,
            readText(parsed, "feedback"),
            readStringList(parsed, "error_tags"),
            readStringList(parsed, "strengths"),
            readStringList(parsed, "weaknesses"),
            readText(parsed, "suggested_next_action"),
            parsed,
            result.provider(),
            result.model(),
            prompt.promptVersion(),
            result.usage()
        );
    }

    private BigDecimal readNormalized(JsonNode parsed, Integer score) {
        if (parsed.path("normalized_score").isNumber()) {
            return parsed.path("normalized_score").decimalValue().setScale(3, RoundingMode.HALF_UP);
        }
        if (score == null) {
            return null;
        }
        return BigDecimal.valueOf(score).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
    }

    private String readText(JsonNode parsed, String field) {
        JsonNode node = parsed.path(field);
        return node.isTextual() ? node.asText() : "";
    }

    private List<String> readStringList(JsonNode parsed, String field) {
        JsonNode node = parsed.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
            .filter(JsonNode::isTextual)
            .map(JsonNode::asText)
            .toList();
    }
}

