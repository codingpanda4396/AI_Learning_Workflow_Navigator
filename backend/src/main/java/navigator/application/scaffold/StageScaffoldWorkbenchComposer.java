package navigator.application.scaffold;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.api.dto.scaffold.ExpressionSchemaPayload;
import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.PromptScaffold;
import navigator.api.dto.scaffold.PromptScaffoldBlock;
import navigator.api.dto.scaffold.StageScaffold;
import navigator.api.dto.scaffold.StageScaffoldWorkbenchPayload;
import navigator.api.dto.scaffold.WorkbenchFeedbackSchemaPayload;
import navigator.application.llm.LlmProperties;
import navigator.application.llm.MockLlmGateway;
import navigator.application.llm.OpenAiCompatibleLlmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 组装 {@link StageScaffoldWorkbenchPayload}：结构来自动作卡与阶段，软文案优先 LLM JSON，失败则仅保留受控 fallback。
 */
@Component
public class StageScaffoldWorkbenchComposer {

    private static final Logger log = LoggerFactory.getLogger(StageScaffoldWorkbenchComposer.class);

    private static final int MAX_SOFT_LEN = 480;
    private static final int MAX_BLOCK_PROMPT_LEN = 320;

    private final MockLlmGateway mockLlmGateway;
    private final OpenAiCompatibleLlmGateway openAiCompatibleLlmGateway;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public StageScaffoldWorkbenchComposer(MockLlmGateway mockLlmGateway,
                                          OpenAiCompatibleLlmGateway openAiCompatibleLlmGateway,
                                          LlmProperties llmProperties,
                                          ObjectMapper objectMapper) {
        this.mockLlmGateway = mockLlmGateway;
        this.openAiCompatibleLlmGateway = openAiCompatibleLlmGateway;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    public StageScaffoldWorkbenchPayload composeWorkbench(String packId, StageScaffold stage) {
        if (!LearningScaffoldPackRegistry.supportsLearningScaffoldEngine(packId) || stage == null) {
            return null;
        }
        LearningActionCard card = resolveCurrentCard(stage);
        if (card == null) {
            return null;
        }
        String stageKey = stage.getStageKey() != null ? stage.getStageKey() : "";
        String emphasis = mapEmphasis(stageKey);

        LlmSoftWorkbench soft = fetchSoftContent(packId, stageKey, card);

        PromptScaffoldBlock mainBlock = PromptScaffoldBlock.builder()
                .id("main")
                .title(trimTo(card.getUserOutputLabel(), 80, "你的输出"))
                .intent(trimTo(card.getGoal(), 120, ""))
                .prompt(trimTo(
                        coalesce(soft.blockPrompt, card.getSystemPrompt(), card.getInstructions()),
                        MAX_BLOCK_PROMPT_LEN,
                        card.getInstructions()))
                .placeholder(trimTo(coalesce(soft.placeholder, card.getSingleAction()), 200, ""))
                .constraint(forbiddenSummary(card))
                .maxLength(2000)
                .sentenceLimit(sentenceLimitFor(emphasis))
                .required(true)
                .kind("paragraph")
                .build();

        List<PromptScaffoldBlock> blocks = new ArrayList<>();
        blocks.add(mainBlock);
        if (card.getAllowedPrompts() != null && !card.getAllowedPrompts().isEmpty()) {
            blocks.add(PromptScaffoldBlock.builder()
                    .id("hints")
                    .title("允许的思考角度")
                    .kind("readonly")
                    .prompt(String.join("；", card.getAllowedPrompts().stream()
                            .filter(s -> s != null && !s.isBlank())
                            .limit(4)
                            .collect(Collectors.toList())))
                    .required(false)
                    .build());
        }

        List<String> starters = new ArrayList<>();
        if (soft.starterPrompts != null) {
            starters.addAll(soft.starterPrompts);
        }
        if (starters.isEmpty() && card.getAllowedPrompts() != null) {
            starters.addAll(card.getAllowedPrompts().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .limit(4)
                    .collect(Collectors.toList()));
        }

        List<String> hints = new ArrayList<>();
        if (soft.hintPrompts != null) {
            hints.addAll(soft.hintPrompts);
        }
        if (hints.isEmpty()) {
            hints.add("用一句话说出你最不确定的点。");
        }

        List<String> completion = new ArrayList<>();
        if (card.getCompletionCriteria() != null) {
            completion.addAll(card.getCompletionCriteria().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toList()));
        }
        if (completion.isEmpty() && card.getPassCriteria() != null) {
            completion.addAll(card.getPassCriteria().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .limit(3)
                    .collect(Collectors.toList()));
        }
        if (completion.isEmpty()) {
            completion.add("满足当前卡片的完成标准。");
        }

        ExpressionSchemaPayload expr = ExpressionSchemaPayload.builder()
                .mode(expressionModeFor(emphasis))
                .fieldIds(List.of("main"))
                .minChars(8)
                .maxChars(2000)
                .build();

        WorkbenchFeedbackSchemaPayload fbSchema = WorkbenchFeedbackSchemaPayload.builder()
                .completenessLabel("完成度")
                .issuePointsLabel("需要修的点")
                .minimalRevisionLabel("最小修改")
                .nextActionLabel("下一步")
                .maxIssuePoints(3)
                .build();

        return StageScaffoldWorkbenchPayload.builder()
                .stageKey(stageKey)
                .cognitiveAction(trimTo(coalesce(soft.cognitiveAction, defaultCognitiveAction(emphasis)), 48, defaultCognitiveAction(emphasis)))
                .stageGoal(trimTo(stage.getPhaseGoal() != null ? stage.getPhaseGoal() : stage.getStageGoal(), 200, ""))
                .currentTaskTitle(trimTo(card.getTitle(), 120, "当前动作"))
                .currentTaskInstruction(trimTo(card.getInstructions(), MAX_SOFT_LEN, ""))
                .deliverable(trimTo(coalesce(card.getSingleAction(), card.getGoal()), 200, ""))
                .completionCriteria(completion)
                .promptScaffold(PromptScaffold.builder().blocks(blocks).build())
                .expressionSchema(expr)
                .starterPrompts(starters.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).limit(6).collect(Collectors.toList()))
                .hintPrompts(hints.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).limit(4).collect(Collectors.toList()))
                .feedbackSchema(fbSchema)
                .llmGeneratedGuide(trimTo(soft.llmGeneratedGuide, MAX_SOFT_LEN, null))
                .llmGeneratedMicroHint(trimTo(soft.llmGeneratedMicroHint, 200, null))
                .llmGeneratedExampleBoundary(trimTo(soft.llmGeneratedExampleBoundary, 200, null))
                .submitConstraint(trimTo("输出将按当前阶段规则校验；通过后可进入下一张动作卡。", 160, null))
                .emphasisMode(emphasis)
                .build();
    }

    private static LearningActionCard resolveCurrentCard(StageScaffold stage) {
        String id = stage.getCurrentActionId();
        if (id == null || stage.getActionCards() == null) {
            return null;
        }
        return stage.getActionCards().stream()
                .filter(c -> id.equals(c.getActionId()))
                .findFirst()
                .orElse(null);
    }

    private static String mapEmphasis(String stageKey) {
        if (stageKey == null) {
            return "STRUCTURE";
        }
        String u = stageKey.toUpperCase(Locale.ROOT);
        if (u.contains("STRUCTURE")) {
            return "STRUCTURE";
        }
        if (u.contains("UNDERSTANDING")) {
            return "UNDERSTANDING";
        }
        if (u.contains("TRAINING")) {
            return "TRAINING";
        }
        if (u.contains("REFLECTION")) {
            return "REFLECTION";
        }
        return "STRUCTURE";
    }

    private static String defaultCognitiveAction(String emphasis) {
        return switch (emphasis) {
            case "UNDERSTANDING" -> "解释机制";
            case "TRAINING" -> "迁移表达";
            case "REFLECTION" -> "收束规则";
            default -> "搭好结构";
        };
    }

    private static String expressionModeFor(String emphasis) {
        return switch (emphasis) {
            case "STRUCTURE" -> "one_sentence_rule";
            case "REFLECTION" -> "error_reflection";
            default -> "paragraph";
        };
    }

    private static Integer sentenceLimitFor(String emphasis) {
        if ("STRUCTURE".equals(emphasis)) {
            return 3;
        }
        return null;
    }

    private static String forbiddenSummary(LearningActionCard card) {
        if (card.getForbiddenActions() == null || card.getForbiddenActions().isEmpty()) {
            return null;
        }
        return card.getForbiddenActions().stream()
                .filter(s -> s != null && !s.isBlank())
                .limit(3)
                .collect(Collectors.joining("；"));
    }

    private LlmSoftWorkbench fetchSoftContent(String packId, String stageKey, LearningActionCard card) {
        String system = """
                你是学习脚手架文案生成器。只输出一个 JSON 对象，不要 Markdown。
                字段与要求：
                - cognitiveAction: 字符串，5～12 字，面向动作，不要空泛
                - llmGeneratedGuide: 字符串，1～2 句，告知用户当下要完成的认知动作（不要给标准答案）
                - llmGeneratedMicroHint: 字符串，一句，最短起步提示
                - llmGeneratedExampleBoundary: 字符串，一句，说明例子边界（不要直接给解题答案）
                - starterPrompts: 字符串数组，2～4 条，可点击开头的短提示（不完整答案）
                - hintPrompts: 字符串数组，1～2 条，卡住时可用的轻量提示
                - blockPrompt: 字符串，用来作为主输入区上方的引导语（可与 systemPrompt 呼应，仍禁止给最终答案）
                - placeholder: 字符串，输入框占位示例（示意句式，不要完整范例答案）
                """;

        String user = """
                packId=%s
                stageKey=%s
                actionId=%s
                cardTitle=%s
                cardGoal=%s
                cardInstructions=%s
                systemPrompt=%s
                allowedPrompts=%s
                """.formatted(
                packId,
                stageKey,
                card.getActionId(),
                nz(card.getTitle()),
                nz(card.getGoal()),
                nz(card.getInstructions()),
                nz(card.getSystemPrompt()),
                card.getAllowedPrompts() != null ? String.join(" | ", card.getAllowedPrompts()) : "");

        String raw = callLlm(system, user);
        LlmSoftWorkbench parsed = parseSoftJson(raw);
        if (parsed != null) {
            return parsed;
        }
        log.debug("workbench soft content: parse failed, using card-only fallback");
        return new LlmSoftWorkbench();
    }

    private String callLlm(String system, String user) {
        if (llmProperties != null && llmProperties.isEnabled()) {
            try {
                return openAiCompatibleLlmGateway.generateReply(system, user);
            } catch (Exception ex) {
                log.warn("workbench LLM failed: {} — {}", ex.getClass().getSimpleName(), ex.getMessage());
                return mockLlmGateway.generateReply(system, user);
            }
        }
        return mockLlmGateway.generateReply(system, user);
    }

    private LlmSoftWorkbench parseSoftJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String json = extractJsonObject(raw.trim());
        try {
            JsonNode n = objectMapper.readTree(json);
            LlmSoftWorkbench w = new LlmSoftWorkbench();
            w.cognitiveAction = text(n, "cognitiveAction");
            w.llmGeneratedGuide = text(n, "llmGeneratedGuide");
            w.llmGeneratedMicroHint = text(n, "llmGeneratedMicroHint");
            w.llmGeneratedExampleBoundary = text(n, "llmGeneratedExampleBoundary");
            w.blockPrompt = text(n, "blockPrompt");
            w.placeholder = text(n, "placeholder");
            w.starterPrompts = stringList(n, "starterPrompts", 6);
            w.hintPrompts = stringList(n, "hintPrompts", 4);
            if (w.llmGeneratedGuide == null && w.cognitiveAction == null && w.starterPrompts.isEmpty()) {
                return null;
            }
            return w;
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) {
            return null;
        }
        String s = n.get(field).asText("").trim();
        return s.isEmpty() ? null : s;
    }

    private static List<String> stringList(JsonNode n, String field, int max) {
        List<String> out = new ArrayList<>();
        if (n == null || !n.has(field) || !n.get(field).isArray()) {
            return out;
        }
        Iterator<JsonNode> it = n.get(field).elements();
        while (it.hasNext() && out.size() < max) {
            String s = it.next().asText("").trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private static String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String coalesce(String... parts) {
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                return p;
            }
        }
        return "";
    }

    private static String trimTo(String s, int max, String orElse) {
        if (s == null || s.isBlank()) {
            return orElse;
        }
        String t = s.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }

    private static final class LlmSoftWorkbench {
        String cognitiveAction;
        String llmGeneratedGuide;
        String llmGeneratedMicroHint;
        String llmGeneratedExampleBoundary;
        String blockPrompt;
        String placeholder;
        List<String> starterPrompts = new ArrayList<>();
        List<String> hintPrompts = new ArrayList<>();
    }
}
