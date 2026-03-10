package com.pandanav.learning.application.service.llm;

import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.ConceptDecomposeContext;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.GoalDiagnosisContext;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.PersonalizedPathContext;
import com.pandanav.learning.domain.llm.model.PracticeGenerationContext;
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
        你是学习任务结构化生成器。
        只返回一个合法 JSON 对象。
        不要解释，不要过程，不要额外文本。
        """;

    private static final String EVAL_SYSTEM_PROMPT = """
        你是训练结果结构化评估器。
        只返回一个合法 JSON 对象。
        不要解释，不要过程，不要额外文本。
        """;

    private static final String GOAL_DIAGNOSE_SYSTEM_PROMPT = """
        你是学习目标诊断助手。
        只返回一个合法 JSON 对象。
        不要解释，不要额外文本。
        """;

    private static final String TUTOR_SYSTEM_PROMPT_TEMPLATE = """
        你是学习流程导师。
        先定位卡点，再给短提示。
        单次回复尽量短。

        hint_mode: {{hint_mode}}
        direct_answer_mode: {{direct_answer_mode}}
        stage: {{task_stage}}
        objective: {{task_objective}}
        concept: {{node_name}}
        learning_goal: {{session_goal}}
        """;

    private static final String CONCEPT_DECOMPOSE_SYSTEM_PROMPT = """
        你是概念拆解器。
        只返回一个合法 JSON 对象。
        不要解释，不要额外文本。
        """;

    private static final String PATH_PLAN_SYSTEM_PROMPT = """
        You are a learning path planner.
        Return one compact JSON object only.
        No explanation.
        """;

    private static final String PRACTICE_GENERATION_SYSTEM_PROMPT = """
        你是训练题结构化生成器。
        只返回一个合法 JSON 对象。
        不要解释，不要过程，不要额外文本。
        """;

    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer();
    private final Map<PromptTemplateKey, PromptDefinition> definitions = new EnumMap<>(PromptTemplateKey.class);

    public DefaultPromptTemplateProvider() {
        registerStagePrompts();
        registerEvaluatePrompt();
        registerGoalDiagnosePrompt();
        registerPathPlanPrompt();
        registerPracticeGenerationPrompt();
        registerTutorPrompt();
        registerConceptDecomposePrompt();
    }

    @Override
    public LlmPrompt buildStagePrompt(PromptTemplateKey key, StageGenerationContext context) {
        PromptDefinition definition = getDefinition(key);
        Map<String, String> variables = Map.of(
            "stage", safe(context.stage() == null ? null : context.stage().name()),
            "objective", safe(context.objective()),
            "node_title", safe(context.nodeTitle())
        );
        return toPrompt(definition, variables);
    }

    @Override
    public LlmPrompt buildEvaluationPrompt(PromptTemplateKey key, EvaluationContext context) {
        PromptDefinition definition = getDefinition(key);
        Map<String, String> variables = Map.of(
            "task_objective", safe(context.taskObjective()),
            "generated_question_content", safe(context.generatedQuestionContent()),
            "user_answer", safe(context.userAnswer())
        );
        return toPrompt(definition, variables);
    }

    @Override
    public LlmPrompt buildGoalDiagnosisPrompt(GoalDiagnosisContext context) {
        PromptDefinition definition = getDefinition(PromptTemplateKey.GOAL_DIAGNOSE_V1);
        return toPrompt(definition, Map.of(
            "course_id", safe(context.courseId()),
            "chapter_id", safe(context.chapterId()),
            "goal_text", safe(context.goalText())
        ));
    }

    @Override
    public LlmPrompt buildPersonalizedPathPlanPrompt(PersonalizedPathContext context) {
        PromptDefinition definition = getDefinition(PromptTemplateKey.PATH_PLAN_V1);
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
        variables.put("weak_points_summary", safe(context.weakPointsSummary()));
        variables.put("chapter_nodes", safe(context.chapterNodes().toString()));
        return toPrompt(definition, variables);
    }

    @Override
    public LlmPrompt buildConceptDecomposePrompt(ConceptDecomposeContext context) {
        PromptDefinition definition = getDefinition(PromptTemplateKey.CONCEPT_DECOMPOSE_V1);
        return toPrompt(definition, Map.of(
            "chapter_id", safe(context.chapterId()),
            "concept", safe(context.concept()),
            "goal", safe(context.goal())
        ));
    }

    @Override
    public LlmPrompt buildPracticeGenerationPrompt(PracticeGenerationContext context) {
        PromptDefinition definition = getDefinition(PromptTemplateKey.PRACTICE_GENERATION_V1);
        return toPrompt(definition, Map.of(
            "task_objective", safe(context.taskObjective()),
            "node_title", safe(context.nodeTitle()),
            "stage_content_json", safe(context.stageContentJson())
        ));
    }

    @Override
    public String buildTutorSystemPrompt(TutorPromptContext context) {
        PromptDefinition definition = getDefinition(PromptTemplateKey.TUTOR_V1);
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
        String userPrompt = renderer.render(spec.userPromptTemplate(), variables).trim()
            + "\nJSON:\n" + spec.expectedJsonSchemaText().trim();

        return new LlmPrompt(
            definition.templateKey(),
            spec.promptKey(),
            spec.promptVersion(),
            resolveProfile(definition.templateKey()),
            spec.systemPrompt(),
            userPrompt,
            spec.expectedJsonSchemaText(),
            spec.outputRules(),
            spec.modelHint(),
            null
        );
    }

    private PromptDefinition getDefinition(PromptTemplateKey key) {
        PromptDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported prompt key: " + key);
        }
        return definition;
    }

    private LlmInvocationProfile resolveProfile(PromptTemplateKey key) {
        return switch (key) {
            case PATH_PLAN_V1 -> LlmInvocationProfile.HEAVY_REASONING_TASK;
            case TUTOR_V1 -> LlmInvocationProfile.CHAT_TASK;
            default -> LlmInvocationProfile.LIGHT_JSON_TASK;
        };
    }

    private void registerStagePrompts() {
        definitions.put(PromptTemplateKey.STRUCTURE_V1, new PromptDefinition(
            PromptTemplateKey.STRUCTURE_V1,
            new PromptSpec(
                "STRUCTURE",
                "v2",
                STAGE_SYSTEM_PROMPT,
                """
                    阶段：{{stage}}
                    知识点：{{node_title}}
                    目标：{{objective}}

                    生成字段：
                    - title：1-20字
                    - summary：1-50字
                    - key_points：1-4条，每条1-12字
                    - common_misconceptions：0-2条，每条1-12字
                    - suggested_sequence：1-4条，每条1-10字
                    不需要推理过程，不需要解释，不确定就用最保守表达。
                    """,
                "{\"title\":\"\",\"summary\":\"\",\"key_points\":[],\"common_misconceptions\":[],\"suggested_sequence\":[]}",
                "short_json_only",
                null
            )
        ));

        definitions.put(PromptTemplateKey.UNDERSTANDING_V1, new PromptDefinition(
            PromptTemplateKey.UNDERSTANDING_V1,
            new PromptSpec(
                "UNDERSTANDING",
                "v2",
                STAGE_SYSTEM_PROMPT,
                """
                    阶段：{{stage}}
                    知识点：{{node_title}}
                    目标：{{objective}}

                    生成字段：
                    - concept_explanation：20-80字
                    - analogy：10-40字
                    - worked_example：20-80字
                    - step_by_step_reasoning：2-4条，每条1-16字
                    - common_errors：1-3条，每条1-14字
                    - check_questions：1-3条，每条1-18字
                    不需要推理过程，不需要解释，不确定就用最通用表达。
                    """,
                "{\"concept_explanation\":\"\",\"analogy\":\"\",\"worked_example\":\"\",\"step_by_step_reasoning\":[],\"common_errors\":[],\"check_questions\":[]}",
                "short_json_only",
                null
            )
        ));

        definitions.put(PromptTemplateKey.TRAINING_V1, new PromptDefinition(
            PromptTemplateKey.TRAINING_V1,
            new PromptSpec(
                "TRAINING",
                "v2",
                STAGE_SYSTEM_PROMPT,
                """
                    阶段：{{stage}}
                    知识点：{{node_title}}
                    目标：{{objective}}

                    生成 3 个短题目：
                    - BASIC / APPLICATION / REASONING 各 1 题
                    - question：1-36字
                    - reference_points：1-2条，每条1-10字
                    - difficulty：EASY|MEDIUM|HARD
                    不要解释，不要长题干。
                    """,
                "{\"questions\":[{\"id\":\"q1\",\"type\":\"BASIC\",\"question\":\"\",\"reference_points\":[],\"difficulty\":\"EASY\"},{\"id\":\"q2\",\"type\":\"APPLICATION\",\"question\":\"\",\"reference_points\":[],\"difficulty\":\"MEDIUM\"},{\"id\":\"q3\",\"type\":\"REASONING\",\"question\":\"\",\"reference_points\":[],\"difficulty\":\"HARD\"}]}",
                "short_json_only",
                null
            )
        ));

        definitions.put(PromptTemplateKey.REFLECTION_V1, new PromptDefinition(
            PromptTemplateKey.REFLECTION_V1,
            new PromptSpec(
                "REFLECTION",
                "v2",
                STAGE_SYSTEM_PROMPT,
                """
                    阶段：{{stage}}
                    知识点：{{node_title}}
                    目标：{{objective}}

                    生成字段：
                    - reflection_prompt：10-50字
                    - review_checklist：2-4条，每条1-12字
                    - next_step_suggestion：10-50字
                    不要解释，不要过程。
                    """,
                "{\"reflection_prompt\":\"\",\"review_checklist\":[],\"next_step_suggestion\":\"\"}",
                "short_json_only",
                null
            )
        ));
    }

    private void registerEvaluatePrompt() {
        PromptSpec spec = new PromptSpec(
            "EVALUATE",
            "v2",
            EVAL_SYSTEM_PROMPT,
            """
                objective={{task_objective}}
                question={{generated_question_content}}
                answer={{user_answer}}
                只做短 JSON 填充，不要解释。
                """,
            "{\"score\":0,\"normalized_score\":0,\"rubric\":{\"concept_correctness\":0,\"reasoning_quality\":0,\"completeness\":0,\"clarity\":0},\"feedback\":\"\",\"error_tags\":[],\"strengths\":[],\"weaknesses\":[],\"suggested_next_action\":\"INSERT_TRAINING_REINFORCEMENT\"}",
            "short_json_only",
            null
        );
        definitions.put(PromptTemplateKey.EVALUATE_V1, new PromptDefinition(PromptTemplateKey.EVALUATE_V1, spec));
        definitions.put(PromptTemplateKey.EVALUATE_V2, new PromptDefinition(PromptTemplateKey.EVALUATE_V2, spec));
    }

    private void registerGoalDiagnosePrompt() {
        definitions.put(PromptTemplateKey.GOAL_DIAGNOSE_V1, new PromptDefinition(
            PromptTemplateKey.GOAL_DIAGNOSE_V1,
            new PromptSpec(
                "GOAL_DIAGNOSE",
                "v1",
                GOAL_DIAGNOSE_SYSTEM_PROMPT,
                """
                    course={{course_id}}
                    chapter={{chapter_id}}
                    goal={{goal_text}}
                    给出紧凑诊断 JSON。
                    """,
                "{\"goal_score\":0,\"smart_breakdown\":{\"specific_score\":0,\"measurable_score\":0,\"achievable_score\":0,\"relevant_score\":0,\"time_bound_score\":0},\"summary\":\"\",\"strengths\":[],\"risks\":[],\"rewritten_goal\":\"\"}",
                "short_json_only",
                null
            )
        ));
    }

    private void registerPathPlanPrompt() {
        definitions.put(PromptTemplateKey.PATH_PLAN_V1, new PromptDefinition(
            PromptTemplateKey.PATH_PLAN_V1,
            new PromptSpec(
                "PATH_PLAN",
                "v2",
                PATH_PLAN_SYSTEM_PROMPT,
                """
                    goal_text={{goal_text}}
                    goal_diagnosis={{goal_diagnosis}}
                    mastery_by_node={{mastery_by_node}}
                    recent_error_tags={{recent_error_tags}}
                    recent_scores={{recent_scores}}
                    weak_points_summary={{weak_points_summary}}
                    chapter_nodes={{chapter_nodes}}

                    Constraints:
                    - ordered_nodes reason <= 40 chars
                    - inserted_tasks <= 3
                    - objective <= 60 chars
                    - risk_flags <= 6
                    """,
                "{\"ordered_nodes\":[{\"node_id\":101,\"priority\":1,\"reason\":\"\"}],\"inserted_tasks\":[{\"node_id\":101,\"stage\":\"UNDERSTANDING\",\"objective\":\"\",\"trigger\":\"\"}],\"plan_reasoning_summary\":\"\",\"risk_flags\":[]}",
                "compact_json_only",
                null
            )
        ));
    }

    private void registerPracticeGenerationPrompt() {
        definitions.put(PromptTemplateKey.PRACTICE_GENERATION_V1, new PromptDefinition(
            PromptTemplateKey.PRACTICE_GENERATION_V1,
            new PromptSpec(
                "PRACTICE_GENERATION",
                "v2",
                PRACTICE_GENERATION_SYSTEM_PROMPT,
                """
                    知识点：{{node_title}}
                    目标：{{task_objective}}
                    参考：{{stage_content_json}}

                    只生成 3 题：
                    - SINGLE_CHOICE / TRUE_FALSE / SHORT_ANSWER 各 1 题
                    - stem：8-40字
                    - standard_answer：1-30字
                    - explanation：8-40字
                    不要长解释。
                    """,
                "{\"items\":[{\"question_type\":\"SINGLE_CHOICE\",\"stem\":\"\",\"options\":[],\"standard_answer\":\"\",\"explanation\":\"\",\"difficulty\":\"EASY\"},{\"question_type\":\"TRUE_FALSE\",\"stem\":\"\",\"options\":[\"True\",\"False\"],\"standard_answer\":\"False\",\"explanation\":\"\",\"difficulty\":\"MEDIUM\"},{\"question_type\":\"SHORT_ANSWER\",\"stem\":\"\",\"options\":[],\"standard_answer\":\"\",\"explanation\":\"\",\"difficulty\":\"HARD\"}]}",
                "short_json_only",
                null
            )
        ));
    }

    private void registerTutorPrompt() {
        definitions.put(PromptTemplateKey.TUTOR_V1, new PromptDefinition(
            PromptTemplateKey.TUTOR_V1,
            new PromptSpec("TUTOR", "v1", TUTOR_SYSTEM_PROMPT_TEMPLATE, "", "{}", "", null)
        ));
    }

    private void registerConceptDecomposePrompt() {
        definitions.put(PromptTemplateKey.CONCEPT_DECOMPOSE_V1, new PromptDefinition(
            PromptTemplateKey.CONCEPT_DECOMPOSE_V1,
            new PromptSpec(
                "CONCEPT_DECOMPOSE",
                "v2",
                CONCEPT_DECOMPOSE_SYSTEM_PROMPT,
                """
                    chapter={{chapter_id}}
                    concept={{concept}}
                    goal={{goal}}
                    输出 3-5 个最小节点。
                    """,
                "{\"concept_nodes\":[{\"id\":\"node1\",\"title\":\"\",\"description\":\"\",\"prerequisites\":[]}]}",
                "short_json_only",
                null
            )
        ));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(unknown)" : value.trim();
    }
}
