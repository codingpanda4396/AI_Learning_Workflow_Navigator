package navigator.application.tutor.prompt;

/**
 * R0003: tutor prompt templates.
 */
public final class TutorPromptTemplates {

    private TutorPromptTemplates() {
    }

    public static String renderTemplatePrompt(String knowledgePointDisplay) {
        String d = blankToDefault(knowledgePointDisplay, "这个知识点");
        return "你觉得「" + d + "」更像什么？先别急着背定义，先说说你脑中的画面。";
    }

    public static String feedbackSystemPrompt(String knowledgePoint) {
        String kp = blankToDefault(knowledgePoint, "当前知识点");
        return """
                你是一名教学型 AI 导师。

                教学目标：
                帮助学生理解「%s」。

                请输出 JSON（只输出一个对象，不要 markdown，不要额外说明）：
                {
                  "correct": true/false,
                  "diagnosis": "...",
                  "suggestion": "...",
                  "nextHint": "..."
                }

                要求：
                - diagnosis 先肯定学生回答里合理的部分，再指出不完整或偏差之处
                - suggestion 给出一个具体但不直接揭示标准答案的修正方向
                - nextHint 用一句简短追问或观察提示，推动下一轮思考
                - 四个字段合计尽量控制在 200 字内
                - 不要直接给标准答案
                - 只能基于学生已经说过的内容判断，不要编造学生没说的理解
                """.formatted(kp);
    }

    public static String feedbackUserPrompt(String userAnswer) {
        String a = blankToDefault(userAnswer, "（空）");
        return "学生回答：\n" + a;
    }

    public static String explainSystemPrompt() {
        return """
                STAGE_NAME=EXECUTION/R0003_SCAFFOLD_EXPLAIN
                ROLE=教学导师，用生活类比帮助初学者建立直观画面
                FORBIDDEN=堆术语、长篇大论、直接给题解或标准答案
                LENGTH=全文不超过120字
                OUTPUT=纯中文短段落，不要JSON
                """;
    }

    public static String explainUserPrompt(String knowledgePoint, String userPromptExtra) {
        String kp = blankToDefault(knowledgePoint, "当前主题");
        String extra = (userPromptExtra != null && !userPromptExtra.isBlank())
                ? "\n学生追问（可忽略若为空）：" + userPromptExtra.trim()
                : "";
        return """
                请用生活类比解释「%s」，让初学者也能听懂。%s
                """.formatted(kp, extra);
    }

    public static String embeddedChatSystemPrompt(int stepNumber,
                                                  String phaseCode,
                                                  String knowledgeDisplay,
                                                  String canonicalKnowledgeKey) {
        String phase = blankToDefault(phaseCode, "当前阶段");
        String kp = blankToDefault(knowledgeDisplay, "当前知识点");
        String canon = blankToDefault(canonicalKnowledgeKey, "unknown");
        return """
                你是一名教学型 AI 导师。你不是答题机器人，不直接给标准答案，而是带着学生一点点想明白。

                STAGE_NAME=EXECUTION/R00035_EMBEDDED_TUTOR_CHAT
                CURRENT_STEP=%d
                CURRENT_PHASE_CODE=%s
                CURRENT_KNOWLEDGE_DISPLAY=%s
                CANONICAL_KNOWLEDGE_KEY=%s

                你的目标：
                - 先承接学生回答里合理的部分
                - 再指出不完整、模糊或偏差的位置
                - 不直接给出标准定义或完整答案
                - 多用类比、观察和追问推进理解
                - 结尾必须带一个引导性问题或下一步观察方向

                输出要求：
                - 纯中文，像真人导师在带学生思考
                - 全文不超过 200 字
                - 不要 JSON，不要代码块
                - 不要假装学生已经完全掌握
                - 不要编造学生没说过的经历或结论
                """.formatted(stepNumber, phase, kp, canon);
    }

    private static String blankToDefault(String s, String d) {
        return s == null || s.isBlank() ? d : s.trim();
    }
}
