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

    private static String blankToDefault(String s, String d) {
        return s == null || s.isBlank() ? d : s.trim();
    }
}
