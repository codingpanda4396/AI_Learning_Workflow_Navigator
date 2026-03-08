package com.pandanav.learning.application.service.llm;

import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;
import org.springframework.stereotype.Component;

@Component
public class DefaultPromptTemplateProvider implements PromptTemplateProvider {

    @Override
    public LlmPrompt buildStagePrompt(PromptTemplateKey key, StageGenerationContext context) {
        String systemPrompt = """
            你是面向大学生学习流程的智能导师系统。
            你只能返回 JSON，禁止输出 markdown code fence，禁止输出额外字段。
            所有字段内容使用中文，必须紧扣知识点与任务目标。
            """;

        String userPrompt = switch (key) {
            case STRUCTURE_PROMPT_V1 -> """
                任务阶段：STRUCTURE
                任务目标：%s
                知识点：%s
                输出字段：title,summary,key_points,common_misconceptions,suggested_sequence
                仅输出 JSON。
                """.formatted(context.objective(), context.nodeTitle());
            case UNDERSTANDING_PROMPT_V1 -> """
                任务阶段：UNDERSTANDING
                任务目标：%s
                知识点：%s
                输出字段：concept_explanation,analogy,step_by_step_reasoning,common_errors,check_questions
                仅输出 JSON。
                """.formatted(context.objective(), context.nodeTitle());
            case TRAINING_PROMPT_V1 -> """
                任务阶段：TRAINING
                任务目标：%s
                知识点：%s
                输出字段：questions
                questions 为 3~5 道题，每题包含 id,type,question,reference_points,difficulty。
                不要脱离题目和知识点本身。
                仅输出 JSON。
                """.formatted(context.objective(), context.nodeTitle());
            case REFLECTION_PROMPT_V1 -> """
                任务阶段：REFLECTION
                任务目标：%s
                知识点：%s
                输出字段：reflection_prompt,review_checklist,next_step_suggestion
                仅输出 JSON。
                """.formatted(context.objective(), context.nodeTitle());
            default -> throw new IllegalArgumentException("Unsupported prompt key: " + key);
        };

        return new LlmPrompt(key, "v1", systemPrompt, userPrompt);
    }

    @Override
    public LlmPrompt buildEvaluationPrompt(PromptTemplateKey key, EvaluationContext context) {
        if (key != PromptTemplateKey.EVALUATE_PROMPT_V1) {
            throw new IllegalArgumentException("Unsupported prompt key: " + key);
        }
        String systemPrompt = """
            你是面向大学生学习流程的智能导师系统。
            你只能返回 JSON，禁止输出 markdown code fence，禁止输出额外字段。
            所有字段内容使用中文，必须紧扣题目与知识点。
            """;

        String userPrompt = """
            任务目标：%s
            题目内容：%s
            用户答案：%s
            请给出结构化评估，输出字段：
            score,normalized_score,feedback,error_tags,strengths,weaknesses,suggested_next_action
            其中 score 在 0~100，normalized_score 在 0~1。
            不要脱离题目和知识点本身。
            仅输出 JSON。
            """.formatted(
            safe(context.taskObjective()),
            safe(context.generatedQuestionContent()),
            safe(context.userAnswer())
        );

        return new LlmPrompt(key, "v1", systemPrompt, userPrompt);
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}

