package navigator.application.tutor.prompt;

/**
 * Tutor prompt templates.
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
                教学目标：帮助学生理解「%s」。
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
                LENGTH=全文不超过 120 字
                OUTPUT=纯中文短段落，不要 JSON
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

    public static String conversationSystemPrompt(String phaseCode,
                                                  String knowledgeDisplay,
                                                  String canonicalKnowledgeKey) {
        String phase = blankToDefault(phaseCode, "UNDERSTANDING").trim().toUpperCase();
        String kp = blankToDefault(knowledgeDisplay, "当前知识点");
        String canon = blankToDefault(canonicalKnowledgeKey, "unknown");
        String phaseInstructions = switch (phase) {
            case "TRAINING" -> """
                    你在表达训练阶段（演示/闯关友好）。
                    目标：
                    - 学生只要用一句话表明对 DFS 与 BFS（或深度优先与广度优先）的基本区分，**必须**在 <can_proceed> 中输出 true，并在 <final_draft> 中给出学生这句话的原文或轻微润色版，不要留空
                    - 最多再指出 1 个薄弱点，不要堆很多要求，不要让学生多轮重写才能进入下一步
                    - 若学生回答过短或完全空泛，再给一次简短追问，但仍应尽量少轮完成
                    """;
            default -> """
                    你在机制理解阶段。
                    目标：
                    - 承接学生当前问题，解释 DFS / BFS 背后的推进机制
                    - 优先帮助学生理解“为什么会这样”，而不是直接背定义
                    - 还没理解透时，继续追问或换角度解释
                    - 说明：是否进入下一阶段由学生自己在界面选择；can_proceed 与 completion_hint 仅作学习反馈参考，不得当作“通关许可”
                    """;
        };

        return """
                你是一名教学型 AI 导师。
                CURRENT_PHASE_CODE=%s
                CURRENT_KNOWLEDGE_DISPLAY=%s
                CANONICAL_KNOWLEDGE_KEY=%s

                %s

                输出格式必须严格遵守，且只能输出下面这些标签：
                <reply>给学生展示的中文回复，80-220 字，像真人导师，先承接再推进</reply>
                <can_proceed>true 或 false</can_proceed>
                <completion_hint>一句中文提示，说明当前为什么能/不能进入下一步</completion_hint>
                <summary>一句中文总结，概括这轮学生达到了什么</summary>
                <final_draft>仅在 TRAINING 且 can_proceed=true 时填写最终版表达，否则留空</final_draft>

                额外要求：
                - reply 不要出现 XML、标签名或 JSON
                - 不要伪装学生已经掌握没有说清的内容
                - 不要输出代码块
                - 不要直接贴标准答案后结束，要保持教学感
                - UNDERSTANDING 阶段：can_proceed 仅描述你对学习进度的判断，界面不会用它锁定「下一步」按钮
                - TRAINING 阶段如果 can_proceed=false，final_draft 必须留空
                """.formatted(phase, kp, canon, phaseInstructions);
    }

    public static String conversationUserPrompt(String phaseCode,
                                                String knowledgePoint,
                                                java.util.List<? extends navigator.api.dto.AiTutorChatRequest.Message> messages) {
        String phase = blankToDefault(phaseCode, "UNDERSTANDING").trim().toUpperCase();
        String kp = blankToDefault(knowledgePoint, "当前知识点");
        StringBuilder sb = new StringBuilder();
        sb.append("知识点：").append(kp).append('\n');
        sb.append("阶段：").append(phase).append('\n');
        sb.append("以下是当前阶段到目前为止的对话，请基于上下文连续回复。\n");
        for (navigator.api.dto.AiTutorChatRequest.Message message : messages) {
            if (message == null) continue;
            String role = blankToDefault(message.getRole(), "user").trim().toLowerCase();
            String content = blankToDefault(message.getContent(), "").trim();
            if (content.isEmpty()) continue;
            String speaker = switch (role) {
                case "assistant", "ai" -> "导师";
                case "system" -> "系统";
                default -> "学生";
            };
            sb.append(speaker).append("：").append(content).append('\n');
        }
        sb.append("请只按规定标签输出。");
        return sb.toString();
    }

    private static String blankToDefault(String s, String d) {
        return s == null || s.isBlank() ? d : s.trim();
    }
}
