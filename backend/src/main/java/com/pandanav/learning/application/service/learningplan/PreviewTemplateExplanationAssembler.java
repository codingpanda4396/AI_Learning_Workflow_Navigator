package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.PlanAlternative;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.observability.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
public class PreviewTemplateExplanationAssembler {

    private static final Logger log = LoggerFactory.getLogger(PreviewTemplateExplanationAssembler.class);
    private static final int ENTRY_MAX_OUTPUT_TOKENS = 220;
    private static final int STRATEGY_MAX_OUTPUT_TOKENS = 220;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired(required = false)
    private LlmGateway llmGateway;
    @Autowired(required = false)
    private LlmProperties llmProperties;

    public PreviewExplanations build(LearningPlanPreview preview, LearningPlanPlanningContext context) {
        PreviewExplanations template = buildTemplate(preview, context);
        if (!llmReady()) {
            return template.withLlm(false, true, 0, 0, 0, 0);
        }
        Instant llmStart = Instant.now();
        try {
            EntryBlock entryBlockSeed = new EntryBlock(template.recommendedEntryReason(), template.learnerEvidence());
            StrategyBlock strategyBlockSeed = new StrategyBlock(template.strategyExplanation(), template.alternatives());
            CompletableFuture<BlockResult<EntryBlock>> entryFuture = CompletableFuture.supplyAsync(
                () -> runEntryBlock(preview, context, template, entryBlockSeed)
            );
            CompletableFuture<BlockResult<StrategyBlock>> strategyFuture = CompletableFuture.supplyAsync(
                () -> runStrategyBlock(preview, context, template, strategyBlockSeed)
            );
            BlockResult<EntryBlock> entryResult = entryFuture.join();
            BlockResult<StrategyBlock> strategyResult = strategyFuture.join();
            int promptTokens = entryResult.promptTokens() + strategyResult.promptTokens();
            int completionTokens = entryResult.completionTokens() + strategyResult.completionTokens();
            int totalTokens = entryResult.totalTokens() + strategyResult.totalTokens();
            int latencyMs = (int) Duration.between(llmStart, Instant.now()).toMillis();
            return new PreviewExplanations(
                clamp(entryResult.value().recommendedEntryReason(), 72),
                template.learnerCurrentState(),
                sanitizeEvidence(entryResult.value().learnerEvidence()),
                template.strategyCode(),
                template.strategyLabel(),
                clamp(strategyResult.value().strategyExplanation(), 56),
                strategyResult.value().alternatives(),
                true,
                false,
                promptTokens,
                completionTokens,
                totalTokens,
                latencyMs
            );
        } catch (CompletionException ex) {
            log.warn("Preview explanation generation failed (completion). traceId={} reason={}", TraceContext.traceId(), ex.getMessage());
            return template.withLlm(false, true, 0, 0, 0, (int) Duration.between(llmStart, Instant.now()).toMillis());
        } catch (Exception ex) {
            log.warn("Preview explanation generation failed. traceId={} reason={}", TraceContext.traceId(), ex.getMessage());
            return template.withLlm(false, true, 0, 0, 0, (int) Duration.between(llmStart, Instant.now()).toMillis());
        }
    }

    private PreviewExplanations buildTemplate(LearningPlanPreview preview, LearningPlanPlanningContext context) {
        String recommendedTitle = preview.summary().recommendedStartNodeName();
        String reason = clamp(
            "先从「" + safe(recommendedTitle, "当前关键知识点") + "」开始，因为它是你当前目标里最容易影响后续推进的一段。",
            72
        );
        String currentState = context != null && context.learnerStateSnapshot() != null
            ? safe(context.learnerStateSnapshot().primaryBlockDescription(), "你当前最需要先补稳一段关键基础。")
            : "你当前最需要先补稳一段关键基础。";
        List<String> evidence = new ArrayList<>();
        if (context != null && context.weakPointLabels() != null) {
            for (String item : context.weakPointLabels()) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                evidence.add(clamp("近期薄弱点集中在「" + item.trim() + "」。", 48));
                if (evidence.size() >= 3) {
                    break;
                }
            }
        }
        if (evidence.isEmpty() && context != null && context.recentErrorTags() != null) {
            for (String item : context.recentErrorTags()) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                evidence.add(clamp("最近错误标签反复出现「" + item.trim() + "」。", 48));
                if (evidence.size() >= 3) {
                    break;
                }
            }
        }
        while (evidence.size() < 3) {
            evidence.add("这一步是后续学习路径的前置环节。");
        }

        String strategyCode = resolveRecommendedStrategyCode(context, preview);
        String strategyLabel = strategyLabel(strategyCode);
        String strategyExplanation = clamp(
            "当前先走「" + strategyLabel + "」，是为了降低后续反复卡住的风险。",
            56
        );

        List<LearningPlanPreviewResponse.AlternativeStrategyResponse> alternatives = new ArrayList<>();
        for (PlanAlternative item : preview.summary().alternatives()) {
            String code = safe(item.strategy(), "UNKNOWN");
            if (code.equals(strategyCode)) {
                continue;
            }
            alternatives.add(new LearningPlanPreviewResponse.AlternativeStrategyResponse(
                code,
                strategyLabel(code),
                clamp("这条路这次不优先，因为当前证据更支持先稳住关键薄弱点。", 56)
            ));
            if (alternatives.size() >= 2) {
                break;
            }
        }

        return new PreviewExplanations(
            reason,
            currentState,
            evidence,
            strategyCode,
            strategyLabel,
            strategyExplanation,
            alternatives,
            false,
            true,
            0,
            0,
            0,
            0
        );
    }

    private BlockResult<EntryBlock> runEntryBlock(
        LearningPlanPreview preview,
        LearningPlanPlanningContext context,
        PreviewExplanations template,
        EntryBlock fallback
    ) {
        Instant start = Instant.now();
        LlmTextResult result = llmGateway.generate(LlmStage.LEARNING_PLAN, buildEntryPrompt(preview, context, template));
        int latencyMs = (int) Duration.between(start, Instant.now()).toMillis();
        JsonNode json = parseJson(result.text());
        String recommendedReason = clamp(readText(json, "recommended_entry_reason", fallback.recommendedEntryReason()), 72);
        List<String> evidence = sanitizeEvidence(readTextList(json, "learner_evidence", fallback.learnerEvidence()));
        return new BlockResult<>(
            new EntryBlock(recommendedReason, evidence),
            usagePromptTokens(result),
            usageCompletionTokens(result),
            usageTotalTokens(result),
            latencyMs
        );
    }

    private BlockResult<StrategyBlock> runStrategyBlock(
        LearningPlanPreview preview,
        LearningPlanPlanningContext context,
        PreviewExplanations template,
        StrategyBlock fallback
    ) {
        Instant start = Instant.now();
        LlmTextResult result = llmGateway.generate(LlmStage.LEARNING_PLAN, buildStrategyPrompt(preview, context, template));
        int latencyMs = (int) Duration.between(start, Instant.now()).toMillis();
        JsonNode json = parseJson(result.text());
        String strategyExplanation = clamp(readText(json, "strategy_explanation", fallback.strategyExplanation()), 56);
        Map<String, String> reasonByCode = new LinkedHashMap<>();
        JsonNode alternativesNode = json.path("alternatives");
        if (alternativesNode.isArray()) {
            alternativesNode.forEach(item -> {
                String code = readText(item, "code", "");
                if (code.isBlank()) {
                    return;
                }
                reasonByCode.put(
                    code.toUpperCase(),
                    clamp(readText(item, "not_recommended_reason", "这次不优先，因为当前证据更支持先稳住关键薄弱点。"), 56)
                );
            });
        }
        List<LearningPlanPreviewResponse.AlternativeStrategyResponse> alternatives = new ArrayList<>();
        for (LearningPlanPreviewResponse.AlternativeStrategyResponse item : fallback.alternatives()) {
            String reason = reasonByCode.getOrDefault(item.code().toUpperCase(), item.notRecommendedReason());
            alternatives.add(new LearningPlanPreviewResponse.AlternativeStrategyResponse(item.code(), item.label(), reason));
        }
        return new BlockResult<>(
            new StrategyBlock(strategyExplanation, alternatives),
            usagePromptTokens(result),
            usageCompletionTokens(result),
            usageTotalTokens(result),
            latencyMs
        );
    }

    private LlmPrompt buildEntryPrompt(
        LearningPlanPreview preview,
        LearningPlanPlanningContext context,
        PreviewExplanations template
    ) {
        String system = """
            你是学习规划文案助手。
            只输出一个 JSON 对象，不要输出任何解释。
            严禁解释内部策略码、枚举值、系统实现细节。
            禁止空话套话、禁止重复 goal 文本。
            """;
        String user = """
            goal=%s
            recommended_concept=%s
            current_state=%s
            key_evidence=%s
            recommended_strategy_label=%s

            只生成以下 JSON:
            {
              "recommended_entry_reason":"string, <=48字, 人话可执行",
              "learner_evidence":["string, <=36字", "string, <=36字", "string, <=36字"]
            }
            规则：
            - learner_evidence 最多 3 条，至少 2 条。
            - 不要出现 FAST_TRACK / FOUNDATION_FIRST / PRACTICE_FIRST / COMPRESSED_10_MIN。
            - 不要重复推荐知识点名称超过 1 次。
            - 直接说用户下一步为何先学，不要写系统自夸。
            """.formatted(
            safe(context == null ? null : context.goalText(), "(none)"),
            safe(preview.summary().recommendedStartNodeName(), "(none)"),
            safe(template.learnerCurrentState(), "(none)"),
            template.learnerEvidence(),
            safe(template.strategyLabel(), "(none)")
        );
        return new LlmPrompt(
            PromptTemplateKey.LEARNING_PLAN_V2,
            PromptTemplateKey.LEARNING_PLAN_V2.promptKey(),
            PromptTemplateKey.LEARNING_PLAN_V2.promptVersion(),
            LlmInvocationProfile.LIGHT_JSON_TASK,
            system,
            user,
            "{\"recommended_entry_reason\":\"\",\"learner_evidence\":[\"\"]}",
            "json_only",
            null,
            ENTRY_MAX_OUTPUT_TOKENS
        );
    }

    private LlmPrompt buildStrategyPrompt(
        LearningPlanPreview preview,
        LearningPlanPlanningContext context,
        PreviewExplanations template
    ) {
        List<String> alternativesSummary = template.alternatives().stream()
            .map(item -> item.code() + ":" + item.label())
            .toList();
        String system = """
            你是学习规划文案助手。
            只输出一个 JSON 对象，不要输出任何解释。
            严禁解释内部策略码、枚举值、系统实现细节。
            禁止空话套话、禁止重复 goal 文本。
            """;
        String user = """
            goal=%s
            recommended_concept=%s
            recommended_strategy=%s
            alternatives=%s

            只生成以下 JSON:
            {
              "strategy_explanation":"string, <=44字",
              "alternatives":[
                {"code":"string","not_recommended_reason":"string, <=40字"}
              ]
            }
            规则：
            - alternatives 只覆盖给定 alternatives，最多 2 条。
            - 不要出现 FAST_TRACK / FOUNDATION_FIRST / PRACTICE_FIRST / COMPRESSED_10_MIN 的解释。
            - not_recommended_reason 只说“这次为什么不优先”，不要贬低用户。
            - 不要重复推荐策略词超过 1 次。
            """.formatted(
            safe(context == null ? null : context.goalText(), "(none)"),
            safe(preview.summary().recommendedStartNodeName(), "(none)"),
            safe(template.strategyLabel(), "(none)"),
            alternativesSummary
        );
        return new LlmPrompt(
            PromptTemplateKey.LEARNING_PLAN_V2,
            PromptTemplateKey.LEARNING_PLAN_V2.promptKey(),
            PromptTemplateKey.LEARNING_PLAN_V2.promptVersion(),
            LlmInvocationProfile.LIGHT_JSON_TASK,
            system,
            user,
            "{\"strategy_explanation\":\"\",\"alternatives\":[{\"code\":\"\",\"not_recommended_reason\":\"\"}]}",
            "json_only",
            null,
            STRATEGY_MAX_OUTPUT_TOKENS
        );
    }

    private JsonNode parseJson(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (Exception ex) {
            throw new IllegalStateException("Preview explanation JSON parse failed.");
        }
    }

    private String readText(JsonNode node, String key, String fallback) {
        if (node == null || node.isMissingNode()) {
            return fallback;
        }
        JsonNode value = node.path(key);
        if (value.isMissingNode() || value.isNull()) {
            return fallback;
        }
        String text = value.asText("");
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    private List<String> readTextList(JsonNode node, String key, List<String> fallback) {
        if (node == null || node.isMissingNode()) {
            return fallback;
        }
        JsonNode value = node.path(key);
        if (!value.isArray()) {
            return fallback;
        }
        List<String> items = new ArrayList<>();
        value.forEach(item -> {
            String text = item == null ? "" : item.asText("");
            if (text == null || text.isBlank()) {
                return;
            }
            items.add(text.trim());
        });
        return items.isEmpty() ? fallback : items;
    }

    private int usagePromptTokens(LlmTextResult result) {
        return result != null && result.usage() != null && result.usage().tokenInput() != null
            ? Math.max(result.usage().tokenInput(), 0)
            : 0;
    }

    private int usageCompletionTokens(LlmTextResult result) {
        return result != null && result.usage() != null && result.usage().tokenOutput() != null
            ? Math.max(result.usage().tokenOutput(), 0)
            : 0;
    }

    private int usageTotalTokens(LlmTextResult result) {
        return result != null && result.usage() != null && result.usage().totalTokens() != null
            ? Math.max(result.usage().totalTokens(), 0)
            : 0;
    }

    private List<String> sanitizeEvidence(List<String> evidence) {
        List<String> result = new ArrayList<>();
        for (String item : evidence) {
            if (item == null || item.isBlank()) {
                continue;
            }
            result.add(clamp(item.trim(), 48));
            if (result.size() >= 3) {
                break;
            }
        }
        while (result.size() < 3) {
            result.add("这一步是后续学习路径的前置环节。");
        }
        return result;
    }

    private boolean llmReady() {
        return llmGateway != null && llmProperties != null && llmProperties.isEnabled() && llmProperties.isReady();
    }

    private String resolveRecommendedStrategyCode(LearningPlanPlanningContext context, LearningPlanPreview preview) {
        if (context != null && context.requestedStrategy() != null && !context.requestedStrategy().isBlank()) {
            return context.requestedStrategy().trim();
        }
        if (preview != null && preview.summary() != null && preview.summary().recommendedPace() != null) {
            String pace = preview.summary().recommendedPace().trim().toUpperCase();
            if ("LIGHT".equals(pace)) {
                return "COMPRESSED_10_MIN";
            }
            if ("INTENSIVE".equals(pace)) {
                return "FAST_TRACK";
            }
        }
        return "FOUNDATION_FIRST";
    }

    public String strategyLabel(String code) {
        return switch (safe(code, "").toUpperCase()) {
            case "FAST_TRACK" -> "快速推进";
            case "PRACTICE_FIRST" -> "先练后学";
            case "COMPRESSED_10_MIN" -> "10 分钟压缩版";
            default -> "先补基础";
        };
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String clamp(String value, int maxChars) {
        String text = safe(value, "").replaceAll("\\s+", " ");
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars - 1) + "…";
    }

    public record PreviewExplanations(
        String recommendedEntryReason,
        String learnerCurrentState,
        List<String> learnerEvidence,
        String strategyCode,
        String strategyLabel,
        String strategyExplanation,
        List<LearningPlanPreviewResponse.AlternativeStrategyResponse> alternatives,
        boolean explanationGenerated,
        boolean templateFallback,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        int llmLatencyMs
    ) {
        public PreviewExplanations withLlm(
            boolean explanationGenerated,
            boolean templateFallback,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            int llmLatencyMs
        ) {
            return new PreviewExplanations(
                recommendedEntryReason,
                learnerCurrentState,
                learnerEvidence,
                strategyCode,
                strategyLabel,
                strategyExplanation,
                alternatives,
                explanationGenerated,
                templateFallback,
                promptTokens,
                completionTokens,
                totalTokens,
                llmLatencyMs
            );
        }
    }

    private record EntryBlock(String recommendedEntryReason, List<String> learnerEvidence) {
    }

    private record StrategyBlock(
        String strategyExplanation,
        List<LearningPlanPreviewResponse.AlternativeStrategyResponse> alternatives
    ) {
    }

    private record BlockResult<T>(
        T value,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        int latencyMs
    ) {
    }
}
