package navigator.application.scaffold;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.api.dto.scaffold.StructureSkeletonBlock;
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

/**
 * STRUCTURE 阶段：调用 LLM 生成 JSON 骨架，失败时使用模板兜底。
 */
@Component
public class StructureSkeletonComposer {

    private static final Logger log = LoggerFactory.getLogger(StructureSkeletonComposer.class);

    private static final String FOLLOW_CLARIFY = "CLARIFY";
    private static final String FOLLOW_ADJACENT = "ADJACENT";

    private final MockLlmGateway mockLlmGateway;
    private final OpenAiCompatibleLlmGateway openAiCompatibleLlmGateway;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public StructureSkeletonComposer(MockLlmGateway mockLlmGateway,
                                     OpenAiCompatibleLlmGateway openAiCompatibleLlmGateway,
                                     LlmProperties llmProperties,
                                     ObjectMapper objectMapper) {
        this.mockLlmGateway = mockLlmGateway;
        this.openAiCompatibleLlmGateway = openAiCompatibleLlmGateway;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    public StructureSkeletonBlock compose(String promptKey, String followUpKind) {
        if (!DfsBfsStructureScaffoldDefinition.isValidPromptKey(promptKey)) {
            throw new IllegalArgumentException("invalid promptKey: " + promptKey);
        }
        String system = buildSystemPrompt();
        String user = buildUserPrompt(promptKey, followUpKind);
        String raw = callLlm(system, user);
        StructureSkeletonBlock parsed = tryParseJson(raw);
        if (parsed != null) {
            return normalizeBlock(parsed);
        }
        log.debug("structure skeleton: JSON parse failed, using template fallback");
        return templateFallback(promptKey, followUpKind);
    }

    private String callLlm(String system, String user) {
        if (llmProperties != null && llmProperties.isEnabled()) {
            try {
                return openAiCompatibleLlmGateway.generateReply(system, user);
            } catch (Exception ex) {
                log.warn("structure skeleton LLM failed: {} — {}", ex.getClass().getSimpleName(), ex.getMessage());
                return mockLlmGateway.generateReply(system, user);
            }
        }
        return mockLlmGateway.generateReply(system, user);
    }

    private static String buildSystemPrompt() {
        return """
                你是计算机数据结构学习导师。只输出一个 JSON 对象，不要 Markdown，不要前后缀。
                字段要求（均为中文短句，条目前加力度）：
                - module: 字符串，一句话说明 DFS/BFS 在知识结构中的位置或所属模块
                - prerequisites: 字符串数组，2～4 条，前置概念
                - connections: 字符串数组，2～4 条，后续会连接到的主题或用法
                - deferTopics: 字符串数组，2～4 条，本轮先不要展开的细节（如实现、复杂度、具体题型）
                总字数克制，像「骨架卡」而非讲义。""";
    }

    private String buildUserPrompt(String promptKey, String followUpKind) {
        String angle = switch (promptKey) {
            case DfsBfsStructureScaffoldDefinition.ACTION_POSITION ->
                    "聚焦：DFS 与 BFS 在「图/树遍历」知识图谱中的位置与角色。";
            case DfsBfsStructureScaffoldDefinition.ACTION_PREREQ ->
                    "聚焦：在系统学习 DFS/BFS 之前，学习者应已具备哪些最小前置概念（如图表示、邻接概念等）。";
            case DfsBfsStructureScaffoldDefinition.ACTION_NEXT ->
                    "聚焦：掌握 DFS/BFS 之后，自然衔接哪些后续主题（如应用、变体），保持粗粒度。";
            case DfsBfsStructureScaffoldDefinition.ACTION_DEFER ->
                    "聚焦：在结构建立阶段应暂时跳过哪些细节（实现、栈队列细节、复杂度、刷题套路）。";
            default -> "聚焦 DFS 与 BFS 的知识结构。";
        };
        String follow = "";
        if (followUpKind != null && !followUpKind.isBlank()) {
            String fk = followUpKind.trim().toUpperCase(Locale.ROOT);
            if (FOLLOW_CLARIFY.equals(fk)) {
                follow = "用户刚才没完全看懂，请用更短、更直观的条目重写 JSON，换角度但仍保持骨架粒度。";
            } else if (FOLLOW_ADJACENT.equals(fk)) {
                follow = "用户想看清与相邻概念的关系：在 module 与 connections 中明确写出与树、图、递归、队列等的邻接关系（仍不写实现细节）。";
            }
        }
        return angle + "\n" + follow + "\n请输出 JSON：{\"module\":\"...\",\"prerequisites\":[],\"connections\":[],\"deferTopics\":[]}";
    }

    private StructureSkeletonBlock tryParseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = extractJsonObject(raw);
        if (text == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(text);
            return StructureSkeletonBlock.builder()
                    .module(textOrEmpty(root, "module"))
                    .prerequisites(readStringArray(root, "prerequisites"))
                    .connections(readStringArray(root, "connections"))
                    .deferTopics(readStringArray(root, "deferTopics"))
                    .build();
        } catch (Exception e) {
            log.debug("structure skeleton JSON parse error: {}", e.getMessage());
            return null;
        }
    }

    private static String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return raw.substring(start, end + 1);
    }

    private static List<String> readStringArray(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isArray()) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        for (Iterator<JsonNode> it = n.elements(); it.hasNext(); ) {
            JsonNode x = it.next();
            if (x.isTextual()) {
                String s = x.asText().trim();
                if (!s.isEmpty()) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    private static String textOrEmpty(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isTextual()) {
            return "";
        }
        return n.asText().trim();
    }

    private static StructureSkeletonBlock normalizeBlock(StructureSkeletonBlock b) {
        if (b.getModule() == null) {
            b.setModule("");
        }
        if (b.getPrerequisites() == null) {
            b.setPrerequisites(new ArrayList<>());
        }
        if (b.getConnections() == null) {
            b.setConnections(new ArrayList<>());
        }
        if (b.getDeferTopics() == null) {
            b.setDeferTopics(new ArrayList<>());
        }
        return b;
    }

    private static StructureSkeletonBlock templateFallback(String promptKey, String followUpKind) {
        String fk = followUpKind != null ? followUpKind.trim().toUpperCase(Locale.ROOT) : "";
        String suffix = FOLLOW_CLARIFY.equals(fk) ? "（再压缩一版）" : FOLLOW_ADJACENT.equals(fk) ? "（补充与相邻概念的关系）" : "";

        return switch (promptKey) {
            case DfsBfsStructureScaffoldDefinition.ACTION_POSITION -> StructureSkeletonBlock.builder()
                    .module("DFS/BFS 属于图搜索与遍历的基础工具，通常接在「图表示」之后，用于系统性访问顶点与边。" + suffix)
                    .prerequisites(List.of("图与邻接表示的基本概念", "顶点与边的含义", "知道「遍历」与「路径」的直觉"))
                    .connections(List.of("遍历应用（连通性、分层）", "更专门的图算法（如最短路等，先知道方向即可）"))
                    .deferTopics(List.of("栈/队列实现细节", "递归与显式栈", "复杂度与证明", "具体题型模板"))
                    .build();
            case DfsBfsStructureScaffoldDefinition.ACTION_PREREQ -> StructureSkeletonBlock.builder()
                    .module("在正式用 DFS/BFS 之前，先具备「图长什么样」与「沿边走路」的直觉。" + suffix)
                    .prerequisites(List.of("图的基本术语：顶点、边、有向/无向", "邻接表/邻接矩阵的直觉（不必背代码）", "树作为特殊图的前置印象"))
                    .connections(List.of("从图表示过渡到「如何有序访问」", "再谈 DFS/BFS 两种典型走法"))
                    .deferTopics(List.of("优化细节", "剪枝与回溯", "具体题解与模板"))
                    .build();
            case DfsBfsStructureScaffoldDefinition.ACTION_NEXT -> StructureSkeletonBlock.builder()
                    .module("掌握 DFS/BFS 后，会自然接到更多「在图上做事」的主题。" + suffix)
                    .prerequisites(List.of("已能区分两种遍历的搜索形态"))
                    .connections(List.of("基于遍历的应用层问题", "与其他算法思想的组合（先保持粗粒度）"))
                    .deferTopics(List.of("实现细节", "证明", "竞赛向技巧"))
                    .build();
            case DfsBfsStructureScaffoldDefinition.ACTION_DEFER -> StructureSkeletonBlock.builder()
                    .module("结构建立阶段先把边界划清：先认位置与关系，不进入实现与题型。" + suffix)
                    .prerequisites(List.of("你已知道本轮目标是「骨架」而非「细节」"))
                    .connections(List.of("下一阶段再进入机制与过程"))
                    .deferTopics(List.of("代码与伪代码", "递归栈与队列操作", "时间空间复杂度", "刷题套路与模板"))
                    .build();
            default -> StructureSkeletonBlock.builder()
                    .module("DFS 与 BFS 是图遍历的基础骨架。")
                    .prerequisites(List.of("图的基本概念"))
                    .connections(List.of("遍历应用"))
                    .deferTopics(List.of("实现细节"))
                    .build();
        };
    }
}
