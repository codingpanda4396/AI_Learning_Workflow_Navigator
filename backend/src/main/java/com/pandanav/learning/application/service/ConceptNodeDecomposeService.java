package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.ConceptDecomposeContext;
import com.pandanav.learning.domain.llm.model.ConceptNodeDecomposeResult;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConceptNodeDecomposeService {

    private final LlmGateway llmGateway;
    private final PromptTemplateProvider promptTemplateProvider;
    private final LlmJsonParser llmJsonParser;

    public ConceptNodeDecomposeService(
        LlmGateway llmGateway,
        PromptTemplateProvider promptTemplateProvider,
        LlmJsonParser llmJsonParser
    ) {
        this.llmGateway = llmGateway;
        this.promptTemplateProvider = promptTemplateProvider;
        this.llmJsonParser = llmJsonParser;
    }

    public ConceptNodeDecomposeResult decompose(String chapterId, String concept, String goal) {
        LlmPrompt prompt = promptTemplateProvider.buildConceptDecomposePrompt(
            new ConceptDecomposeContext(chapterId, concept, goal)
        );
        LlmTextResult result = llmGateway.generate(prompt);
        JsonNode parsed = llmJsonParser.parse(result.text());

        List<ConceptNodeDecomposeResult.ConceptNodeItem> nodes = new ArrayList<>();
        JsonNode array = parsed.path("concept_nodes");
        if (array.isArray()) {
            for (JsonNode item : array) {
                List<String> prerequisites = new ArrayList<>();
                JsonNode preArray = item.path("prerequisites");
                if (preArray.isArray()) {
                    for (JsonNode p : preArray) {
                        if (p.isTextual() && !p.asText().isBlank()) {
                            prerequisites.add(p.asText());
                        }
                    }
                }
                nodes.add(new ConceptNodeDecomposeResult.ConceptNodeItem(
                    item.path("id").asText(),
                    item.path("title").asText(),
                    item.path("description").asText(),
                    prerequisites
                ));
            }
        }

        return new ConceptNodeDecomposeResult(nodes);
    }
}
