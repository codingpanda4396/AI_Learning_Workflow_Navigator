package com.pandanav.learning.application.service.llm;

import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.ConceptDecomposeContext;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.GoalDiagnosisContext;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.PersonalizedPathContext;
import com.pandanav.learning.domain.llm.model.PromptDefinition;
import com.pandanav.learning.domain.llm.model.PromptSpec;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;
import com.pandanav.learning.domain.llm.model.TutorPromptContext;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultPromptTemplateProvider implements PromptTemplateProvider {

    private static final String STAGE_SYSTEM_PROMPT = """
        你是面向大学生学习流程的教学型 AI 引擎。
        你只允许输出 JSON，不允许输出 markdown code fence，不允许输出 schema 外字段。
        所有字段值必须使用简体中文，且内容必须紧扣当前知识点与任务目标，禁止泛化空谈。
        """;

    private static final String EVAL_SYSTEM_PROMPT = """
        你是学习任务评估器，只负责根据题目与学生答案输出结构化评估 JSON。
        你只允许输出 JSON，不允许输出 markdown code fence，不允许输出 schema 外字段。
        所有字段值必须使用简体中文，评分必须严格遵循给定 rubric。
        """;

    private static final String GOAL_DIAGNOSE_SYSTEM_PROMPT = """
        你是学习目标诊断助手，使用 SMART 方法诊断目标质量。
        你只允许输出 JSON，不允许输出 markdown code fence，不允许输出 schema 外字段。
        所有字段值必须使用简体中文，改写目标必须为一句话、可执行、可衡量。
        """;

    private static final String TUTOR_SYSTEM_PROMPT_TEMPLATE = """
        你是“学习流程导师”，不是普通问答助手。

        教学策略：
        1) 优先引导，不直接给最终答案。
        2) 先判断学生卡点；能提问就先提问。
        3) 学生连续卡住时给 hint；必要时给 partial answer。
        4) 学生理解正确时，推进到更高一级问题。
        5) 单次回复控制在 120 字以内，避免长篇大论。

        幻觉约束：
        1) 不编造课程上下文中不存在的事实。
        2) 不假设学生已经掌握未提供的前置知识。

        模式开关：
        - hint_mode: {{hint_mode}}
        - direct_answer_mode: {{direct_answer_mode}}

        当前任务上下文：
        - stage: {{task_stage}}
        - objective: {{task_objective}}
        - concept: {{node_name}}
        - learning_goal: {{session_goal}}
        """;

    private static final String CONCEPT_DECOMPOSE_SYSTEM_PROMPT = """
        你是学习路径拆解助手，负责把章节/概念/目标拆成最小可学习节点。
        你只允许输出 JSON，不允许输出 markdown code fence，不允许输出 schema 外字段。
        所有字段值必须使用简体中文，节点关系要实用，不要过度图谱化。
        """;

    private static final String PATH_PLAN_SYSTEM_PROMPT = """
        You are a personalized learning path planner.
        Output only one JSON object without markdown code fences.
        Do not output fields outside schema.
        """;

    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer();
    private final Map<PromptTemplateKey, PromptDefinition> definitions = new EnumMap<>(PromptTemplateKey.class);

    public DefaultPromptTemplateProvider() {
        registerStagePrompts();
        registerEvaluatePrompt();
        registerGoalDiagnosePrompt();
        registerPathPlanPrompt();
        registerTutorPrompt();
        registerConceptDecomposePrompt();
    }

    @Override
    public LlmPrompt buildStagePrompt(PromptTemplateKey key, StageGenerationContext context) {
        PromptDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported prompt key: " + key);
        }
        Map<String, String> variables = Map.of(
            "objective", safe(context.objective()),
            "node_title", safe(context.nodeTitle())
        );
        return toPrompt(definition, variables);
    }

    @Override
    public LlmPrompt buildEvaluationPrompt(PromptTemplateKey key, EvaluationContext context) {
        if (key != PromptTemplateKey.EVALUATE_V1 && key != PromptTemplateKey.EVALUATE_V2) {
            throw new IllegalArgumentException("Unsupported prompt key: " + key);
        }
        PromptDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported prompt key: " + key);
        }
        Map<String, String> variables = Map.of(
            "task_objective", safe(context.taskObjective()),
            "generated_question_content", safe(context.generatedQuestionContent()),
            "user_answer", safe(context.userAnswer())
        );
        return toPrompt(definition, variables);
    }

    @Override
    public LlmPrompt buildGoalDiagnosisPrompt(GoalDiagnosisContext context) {
        PromptDefinition definition = definitions.get(PromptTemplateKey.GOAL_DIAGNOSE_V1);
        Map<String, String> variables = Map.of(
            "course_id", safe(context.courseId()),
            "chapter_id", safe(context.chapterId()),
            "goal_text", safe(context.goalText())
        );
        return toPrompt(definition, variables);
    }

    @Override
    public LlmPrompt buildPersonalizedPathPlanPrompt(PersonalizedPathContext context) {
        PromptDefinition definition = definitions.get(PromptTemplateKey.PATH_PLAN_V1);
        Map<String, String> variables = new HashMap<>();
        variables.put("goal_text", safe(context.goalText()));
        variables.put(
            "goal_diagnosis",
            "specific=%d, measurable=%d, achievable=%d, relevant=%d, time_bound=%d".formatted(
                context.goalDiagnosis().specificScore(),
                context.goalDiagnosis().measurableScore(),
                context.goalDiagnosis().achievableScore(),
                context.goalDiagnosis().relevantScore(),
                context.goalDiagnosis().timeBoundScore()
            )
        );
        variables.put("mastery_by_node", safe(context.masteryByNode().toString()));
        variables.put("recent_error_tags", safe(context.recentErrorTags().toString()));
        variables.put("recent_scores", safe(context.recentScores().toString()));
        variables.put("chapter_nodes", safe(context.chapterNodes().toString()));
        return toPrompt(definition, variables);
    }

    @Override
    public LlmPrompt buildConceptDecomposePrompt(ConceptDecomposeContext context) {
        PromptDefinition definition = definitions.get(PromptTemplateKey.CONCEPT_DECOMPOSE_V1);
        Map<String, String> variables = Map.of(
            "chapter_id", safe(context.chapterId()),
            "concept", safe(context.concept()),
            "goal", safe(context.goal())
        );
        return toPrompt(definition, variables);
    }

    @Override
    public String buildTutorSystemPrompt(TutorPromptContext context) {
        PromptDefinition definition = definitions.get(PromptTemplateKey.TUTOR_V1);
        return renderer.render(definition.spec().systemPrompt(), Map.of(
            "task_stage", safe(context.taskStage()),
            "task_objective", safe(context.taskObjective()),
            "node_name", safe(context.nodeName()),
            "session_goal", safe(context.sessionGoal()),
            "hint_mode", context.hintMode() == null ? "AUTO" : context.hintMode().name(),
            "direct_answer_mode", context.directAnswerMode() == null ? "AUTO" : context.directAnswerMode().name()
        ));
    }

    private LlmPrompt toPrompt(PromptDefinition definition, Map<String, String> variables) {
        PromptSpec spec = definition.spec();
        String renderedUserPrompt = renderer.render(spec.userPromptTemplate(), variables);
        String userPrompt = renderedUserPrompt
            + "\n\nexpected_json_schema:\n" + spec.expectedJsonSchemaText()
            + "\n\noutput_rules:\n" + spec.outputRules();

        return new LlmPrompt(
            definition.templateKey(),
            spec.promptKey(),
            spec.promptVersion(),
            spec.systemPrompt(),
            userPrompt,
            spec.expectedJsonSchemaText(),
            spec.outputRules(),
            spec.modelHint()
        );
    }

    private void registerStagePrompts() {
        definitions.put(PromptTemplateKey.STRUCTURE_V1, new PromptDefinition(
            PromptTemplateKey.STRUCTURE_V1,
            new PromptSpec(
                PromptTemplateKey.STRUCTURE_V1.promptKey(),
                PromptTemplateKey.STRUCTURE_V1.promptVersion(),
                STAGE_SYSTEM_PROMPT,
                """
                    任务阶段：STRUCTURE
                    任务目标：{{objective}}
                    知识点：{{node_title}}
                    请根据任务目标输出结构化学习地图。
                    """,
                """
                    {
                      "title": "string(8-30字)",
                      "summary": "string(40-120字)",
                      "key_points": ["string(10-40字)", "3-5项"],
                      "common_misconceptions": ["string(10-40字)", "2-3项"],
                      "suggested_sequence": ["string(8-30字)", "3-5项"]
                    }
                    """,
                """
                    - 只输出一个 JSON 对象，且字段必须严格等于 schema。
                    - 禁止输出 schema 外字段、解释性前后缀、markdown。
                    - 所有字段值必须为简体中文。
                    - 内容必须围绕当前知识点与任务目标，禁止泛泛而谈。
                    """,
                null
            )
        ));

        definitions.put(PromptTemplateKey.UNDERSTANDING_V1, new PromptDefinition(
            PromptTemplateKey.UNDERSTANDING_V1,
            new PromptSpec(
                PromptTemplateKey.UNDERSTANDING_V1.promptKey(),
                PromptTemplateKey.UNDERSTANDING_V1.promptVersion(),
                STAGE_SYSTEM_PROMPT,
                """
                    任务阶段：UNDERSTANDING
                    任务目标：{{objective}}
                    知识点：{{node_title}}
                    请输出帮助学生真正理解该知识点的讲解内容。
                    """,
                """
                    {
                      "concept_explanation": "string(80-220字)",
                      "analogy": "string(40-120字)",
                      "worked_example": "string(80-220字)",
                      "step_by_step_reasoning": ["string(12-45字)", "3-5项"],
                      "common_errors": ["string(10-40字)", "2-4项"],
                      "check_questions": ["string(12-45字)", "2-4项"]
                    }
                    """,
                """
                    - 只输出一个 JSON 对象，字段必须严格等于 schema。
                    - 禁止输出 schema 外字段、解释性前后缀、markdown。
                    - 所有字段值必须为简体中文。
                    - 每个问题和示例必须紧扣当前知识点与目标，不得空泛。
                    """,
                null
            )
        ));

        definitions.put(PromptTemplateKey.TRAINING_V1, new PromptDefinition(
            PromptTemplateKey.TRAINING_V1,
            new PromptSpec(
                PromptTemplateKey.TRAINING_V1.promptKey(),
                PromptTemplateKey.TRAINING_V1.promptVersion(),
                STAGE_SYSTEM_PROMPT,
                """
                    任务阶段：TRAINING
                    任务目标：{{objective}}
                    知识点：{{node_title}}
                    请输出训练题集合，覆盖基础理解、概念应用、推理题。
                    """,
                """
                    {
                      "questions": [
                        {
                          "id": "string(例如q1)",
                          "type": "BASIC|APPLICATION|REASONING",
                          "question": "string(20-120字)",
                          "reference_points": ["string(8-35字)", "2-4项"],
                          "difficulty": "EASY|MEDIUM|HARD"
                        }
                      ]
                    }
                    约束：questions 数量 3-5。
                    """,
                """
                    - 只输出一个 JSON 对象，字段必须严格等于 schema。
                    - 禁止输出 schema 外字段、解释性前后缀、markdown。
                    - questions 至少包含 1 道 BASIC、1 道 APPLICATION、1 道 REASONING。
                    - 所有字段值必须为简体中文（id/type/difficulty 除外）。
                    - 题目必须紧扣当前知识点与任务目标。
                    """,
                null
            )
        ));

        definitions.put(PromptTemplateKey.REFLECTION_V1, new PromptDefinition(
            PromptTemplateKey.REFLECTION_V1,
            new PromptSpec(
                PromptTemplateKey.REFLECTION_V1.promptKey(),
                PromptTemplateKey.REFLECTION_V1.promptVersion(),
                STAGE_SYSTEM_PROMPT,
                """
                    任务阶段：REFLECTION
                    任务目标：{{objective}}
                    知识点：{{node_title}}
                    请输出复盘与下一步建议。
                    """,
                """
                    {
                      "reflection_prompt": "string(30-120字)",
                      "review_checklist": ["string(10-35字)", "3-5项"],
                      "next_step_suggestion": "string(30-120字)"
                    }
                    """,
                """
                    - 只输出一个 JSON 对象，字段必须严格等于 schema。
                    - 禁止输出 schema 外字段、解释性前后缀、markdown。
                    - 所有字段值必须为简体中文。
                    - 复盘建议必须可执行，且与当前知识点和目标直接相关。
                    """,
                null
            )
        ));
    }

    private void registerEvaluatePrompt() {
        definitions.put(PromptTemplateKey.EVALUATE_V1, new PromptDefinition(
            PromptTemplateKey.EVALUATE_V1,
            new PromptSpec(
                PromptTemplateKey.EVALUATE_V1.promptKey(),
                PromptTemplateKey.EVALUATE_V1.promptVersion(),
                EVAL_SYSTEM_PROMPT,
                """
                    任务目标：{{task_objective}}
                    题目内容：{{generated_question_content}}
                    学生答案：{{user_answer}}
                    请按 rubric 评估并输出 JSON。
                    """,
                """
                    {
                      "score": "number(0-100)",
                      "normalized_score": "number(0-1, 且 = score / 100)",
                      "rubric": {
                        "concept_correctness": "number(0-40)",
                        "reasoning_quality": "number(0-30)",
                        "completeness": "number(0-20)",
                        "clarity": "number(0-10)"
                      },
                      "feedback": "string(40-180字)",
                      "error_tags": ["string", "2-4项"],
                      "strengths": ["string", "2-3项"],
                      "weaknesses": ["string", "2-3项"],
                      "suggested_next_action": "INSERT_REMEDIAL_UNDERSTANDING|INSERT_TRAINING_VARIANTS|INSERT_TRAINING_REINFORCEMENT|ADVANCE_TO_NEXT_NODE"
                    }
                    """,
                """
                    - 只输出一个 JSON 对象，字段必须严格等于 schema。
                    - 禁止输出 schema 外字段、解释性前后缀、markdown。
                    - rubric 四个维度求和必须等于 score。
                    - normalized_score 必须等于 score / 100（保留 3 位小数）。
                    - 所有文本字段值必须为简体中文。
                    """,
                null
            )
        ));

        definitions.put(PromptTemplateKey.EVALUATE_V2, new PromptDefinition(
            PromptTemplateKey.EVALUATE_V2,
            new PromptSpec(
                PromptTemplateKey.EVALUATE_V2.promptKey(),
                PromptTemplateKey.EVALUATE_V2.promptVersion(),
                EVAL_SYSTEM_PROMPT,
                definitions.get(PromptTemplateKey.EVALUATE_V1).spec().userPromptTemplate(),
                definitions.get(PromptTemplateKey.EVALUATE_V1).spec().expectedJsonSchemaText(),
                definitions.get(PromptTemplateKey.EVALUATE_V1).spec().outputRules(),
                null
            )
        ));
    }

    private void registerGoalDiagnosePrompt() {
        definitions.put(PromptTemplateKey.GOAL_DIAGNOSE_V1, new PromptDefinition(
            PromptTemplateKey.GOAL_DIAGNOSE_V1,
            new PromptSpec(
                PromptTemplateKey.GOAL_DIAGNOSE_V1.promptKey(),
                PromptTemplateKey.GOAL_DIAGNOSE_V1.promptVersion(),
                GOAL_DIAGNOSE_SYSTEM_PROMPT,
                """
                    请诊断以下学习目标。
                    course_id={{course_id}}
                    chapter_id={{chapter_id}}
                    goal_text={{goal_text}}
                    """,
                """
                    {
                      "goal_score": "number(0-100)",
                      "smart_breakdown": {
                        "specific_score": "number(0-20)",
                        "measurable_score": "number(0-20)",
                        "achievable_score": "number(0-20)",
                        "relevant_score": "number(0-20)",
                        "time_bound_score": "number(0-20)"
                      },
                      "summary": "string(40-160字)",
                      "strengths": ["string", "2-3项"],
                      "risks": ["string", "2-3项"],
                      "rewritten_goal": "string(一句话，可执行、可衡量，适合大学生学习场景)"
                    }
                    """,
                """
                    - 只输出一个 JSON 对象，字段必须严格等于 schema。
                    - 禁止输出 schema 外字段、解释性前后缀、markdown。
                    - goal_score 必须等于 smart_breakdown 五个维度之和。
                    - 所有文本字段值必须为简体中文。
                    """,
                null
            )
        ));
    }

    private void registerPathPlanPrompt() {
        definitions.put(PromptTemplateKey.PATH_PLAN_V1, new PromptDefinition(
            PromptTemplateKey.PATH_PLAN_V1,
            new PromptSpec(
                PromptTemplateKey.PATH_PLAN_V1.promptKey(),
                PromptTemplateKey.PATH_PLAN_V1.promptVersion(),
                PATH_PLAN_SYSTEM_PROMPT,
                """
                    Build a personalized plan from this context:
                    goal_text={{goal_text}}
                    goal_diagnosis={{goal_diagnosis}}
                    mastery_by_node={{mastery_by_node}}
                    recent_error_tags={{recent_error_tags}}
                    recent_scores={{recent_scores}}
                    chapter_nodes={{chapter_nodes}}
                    Constraints:
                    - Only use node_id from chapter_nodes.
                    - stage in inserted_tasks must be UNDERSTANDING or TRAINING.
                    - Prefer concise reasons and objectives.
                    """,
                """
                    {
                      "ordered_nodes": [
                        {"node_id": 101, "priority": 1, "reason": "string(6-120)"}
                      ],
                      "inserted_tasks": [
                        {
                          "node_id": 101,
                          "stage": "UNDERSTANDING|TRAINING",
                          "objective": "string(10-200)",
                          "trigger": "string(2-50)"
                        }
                      ],
                      "plan_reasoning_summary": "string(20-400)",
                      "risk_flags": ["string(4-80)"]
                    }
                    """,
                """
                    - Output exactly one JSON object.
                    - Do not output markdown code fences.
                    - No fields outside schema.
                    """,
                null
            )
        ));
    }

    private void registerTutorPrompt() {
        definitions.put(PromptTemplateKey.TUTOR_V1, new PromptDefinition(
            PromptTemplateKey.TUTOR_V1,
            new PromptSpec(
                PromptTemplateKey.TUTOR_V1.promptKey(),
                PromptTemplateKey.TUTOR_V1.promptVersion(),
                TUTOR_SYSTEM_PROMPT_TEMPLATE,
                "",
                "{}",
                "",
                null
            )
        ));
    }

    private void registerConceptDecomposePrompt() {
        definitions.put(PromptTemplateKey.CONCEPT_DECOMPOSE_V1, new PromptDefinition(
            PromptTemplateKey.CONCEPT_DECOMPOSE_V1,
            new PromptSpec(
                PromptTemplateKey.CONCEPT_DECOMPOSE_V1.promptKey(),
                PromptTemplateKey.CONCEPT_DECOMPOSE_V1.promptVersion(),
                CONCEPT_DECOMPOSE_SYSTEM_PROMPT,
                """
                    请基于以下上下文拆解 concept nodes：
                    chapter_id={{chapter_id}}
                    concept={{concept}}
                    goal={{goal}}
                    """,
                """
                    {
                      "concept_nodes": [
                        {
                          "id": "string(例如node1)",
                          "title": "string(4-20字)",
                          "description": "string(20-80字)",
                          "prerequisites": ["string(节点id)"]
                        }
                      ]
                    }
                    约束：concept_nodes 数量 3-6。
                    """,
                """
                    - 只输出一个 JSON 对象，字段必须严格等于 schema。
                    - 禁止输出 schema 外字段、解释性前后缀、markdown。
                    - 所有文本字段值必须为简体中文（id 与 prerequisites 除外）。
                    - prerequisite 关系要合理且无自依赖。
                    """,
                null
            )
        ));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(unknown)" : value.trim();
    }
}
