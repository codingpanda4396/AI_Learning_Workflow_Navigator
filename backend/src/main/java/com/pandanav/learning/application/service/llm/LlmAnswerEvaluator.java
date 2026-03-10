package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private final PromptOutputValidator promptOutputValidator;

    public LlmAnswerEvaluator(
        LlmGateway llmGateway,
        PromptTemplateProvider promptTemplateProvider,
        LlmJsonParser llmJsonParser,
        PromptOutputValidator promptOutputValidator
    ) {
        this.llmGateway = llmGateway;
        this.promptTemplateProvider = promptTemplateProvider;
        this.llmJsonParser = llmJsonParser;
        this.promptOutputValidator = promptOutputValidator;
    }

    @Override
    public EvaluationResult evaluate(EvaluationContext context) {
        LlmPrompt prompt = promptTemplateProvider.buildEvaluationPrompt(PromptTemplateKey.EVALUATE_V1, context);
        LlmTextResult result = llmGateway.generate(prompt);
        JsonNode parsed = llmJsonParser.parse(result.text());

        if (!(parsed instanceof ObjectNode objectNode)) {
            throw new InternalServerException("LLM evaluation output must be JSON object.");
        }

        promptOutputValidator.repairEvaluation(objectNode);
        List<String> errors = promptOutputValidator.validateEvaluation(objectNode);
        if (!errors.isEmpty()) {
            throw new InternalServerException("LLM evaluation output invalid: " + String.join("; ", errors));
        }

        Integer score = objectNode.path("score").asInt();
        BigDecimal normalized = readNormalized(objectNode.path("normalized_score"));

        return new EvaluationResult(
            score,
            normalized,
            readText(objectNode, "feedback"),
            readStringList(objectNode, "error_tags"),
            readStringList(objectNode, "strengths"),
            readStringList(objectNode, "weaknesses"),
            readText(objectNode, "suggested_next_action"),
            objectNode.path("rubric"),
            objectNode,
            result.provider(),
            result.model(),
            prompt.promptKey(),
            prompt.promptVersion(),
            result.usage(),
            result.requestPayload(),
            result.responsePayload()
        );
    }

    private BigDecimal readNormalized(JsonNode node) {
        if (!node.isNumber()) {
            throw new InternalServerException("LLM evaluation output missing normalized_score.");
        }
        return node.decimalValue().setScale(3, RoundingMode.HALF_UP);
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
