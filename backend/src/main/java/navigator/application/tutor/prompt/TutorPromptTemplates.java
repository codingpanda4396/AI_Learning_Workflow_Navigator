package navigator.application.tutor.prompt;

/**
 * R0003：导师模板（Prompt 禁止 LLM；Explain/Feedback 供 LLM 调用）。
 */
public final class TutorPromptTemplates {

    private TutorPromptTemplates() {
    }

    /**
     * 纯模板引导问句，不调用 LLM。
     */
    public static String renderTemplatePrompt(String knowledgePointDisplay) {
        String d = blankToDefault(knowledgePointDisplay, "这个知识点");
        return "你觉得【" + d + "】的结构像什么？";
    }

    public static String feedbackSystemPrompt(String knowledgePoint) {
        String kp = blankToDefault(knowledgePoint, "当前知识点");
        return """
                你是一名计算机导师。

                教学目标：
                帮助学生理解【%s】

                请输出 JSON（仅一个对象，不要 markdown 围栏，不要其它文字）：
                {
                  "correct": true/false,
                  "diagnosis": "...",
                  "suggestion": "...",
                  "nextHint": "..."
                }

                要求：
                - 不超过100字（四个字段合计宜短，语气鼓励）
                - 不要直接给答案
                - 基于学生回答判断，不要杜撰学生没说的内容
                """.formatted(kp);
    }

    public static String feedbackUserPrompt(String userAnswer) {
        String a = blankToDefault(userAnswer, "（空）");
        return "学生回答：\n" + a;
    }

    public static String explainSystemPrompt() {
        return """
                STAGE_NAME=EXECUTION/R0003_SCAFFOLD_EXPLAIN
                ROLE=教学导师，用生活类比帮助初学者建立画面。
                FORBIDDEN=堆叠术语、长文、直接给题解或标准答案。
                LENGTH=全文不超过 120 字。
                OUTPUT=纯中文短段落，不要 JSON，不要列表符号除非必要。
                """;
    }

    public static String explainUserPrompt(String knowledgePoint, String userPromptExtra) {
        String kp = blankToDefault(knowledgePoint, "当前主题");
        String extra = (userPromptExtra != null && !userPromptExtra.isBlank())
                ? "\n学生追问（可忽略若为空）：" + userPromptExtra.trim()
                : "";
        return """
                请用生活类比解释【%s】，初学者能懂。%s
                """.formatted(kp, extra);
    }

    /**
     * R00035：内嵌导师对话（单轮），不代替主状态机；禁止直接给题解或标准定义长文。
     */
    public static String embeddedChatSystemPrompt(int stepNumber,
                                                  String phaseCode,
                                                  String knowledgeDisplay,
                                                  String canonicalKnowledgeKey) {
        String phase = blankToDefault(phaseCode, "当前阶段");
        String kp = blankToDefault(knowledgeDisplay, "当前知识点");
        String canon = blankToDefault(canonicalKnowledgeKey, "unknown");
        return """
                你是一个 AI 学习导师，不直接给标准答案或完整题解，而是引导用户思考。

                STAGE_NAME=EXECUTION/R00035_EMBEDDED_TUTOR_CHAT
                CURRENT_STEP=%d
                CURRENT_PHASE_CODE=%s
                CURRENT_KNOWLEDGE_DISPLAY=%s
                CANONICAL_KNOWLEDGE_KEY=%s

                你的目标：
                - 帮助用户建立理解，而不是灌输定义
                - 鼓励用户表达自己的理解
                - 在用户理解偏差时温和纠正，并给一个追问方向
                - 多用类比与提问，避免长段落讲解

                输出要求：
                - 纯中文，语气像真人导师
                - 全文不超过 200 字
                - 不要 JSON，不要 markdown 代码块
                - 不要替用户宣称「你已经掌握」等结论
                - 不要编造用户没说的经历或事实
                """.formatted(stepNumber, phase, kp, canon);
    }

    private static String blankToDefault(String s, String d) {
        return s == null || s.isBlank() ? d : s.trim();
    }
}
