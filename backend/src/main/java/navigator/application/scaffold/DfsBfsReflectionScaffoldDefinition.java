package navigator.application.scaffold;

import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.StageScaffold;

import java.util.List;

/**
 * DFS/BFS REFLECTION：四卡收敛，错误 → 根因 → 规律 → 能力。
 */
public final class DfsBfsReflectionScaffoldDefinition {

    public static final String STAGE_KEY = "REFLECTION";

    public static final String ACTION_ERROR_RECALL = "reflection_dfs_bfs_error_recall";
    public static final String ACTION_ROOT_CAUSE = "reflection_dfs_bfs_root_cause";
    public static final String ACTION_DECISION_RULE = "reflection_dfs_bfs_decision_rule";
    public static final String ACTION_CAPABILITY_NAME = "reflection_dfs_bfs_capability_name";

    private DfsBfsReflectionScaffoldDefinition() {
    }

    public static List<String> orderedActionIds() {
        return List.of(ACTION_ERROR_RECALL, ACTION_ROOT_CAUSE, ACTION_DECISION_RULE, ACTION_CAPABILITY_NAME);
    }

    public static StageScaffold buildStage() {
        return StageScaffold.builder()
                .stageKey(STAGE_KEY)
                .stageTitle("反思收敛")
                .stageGoal("把训练里暴露的问题压成可带走的认知资产：错误、根因、判断规则、能力命名与下一步策略。")
                .stageDescription("每张卡只追问一个点；不通过时会给一条轻量提示，请改写到够具体为止。")
                .validatorType("REFLECTION_RULES")
                .tutorMode("REFLECTION_CONSTRAINED")
                .actionCards(List.of(
                        LearningActionCard.builder()
                                .actionId(ACTION_ERROR_RECALL)
                                .title("回顾一下，你前面最容易犯的一个错误是什么？")
                                .goal("指出一个与 DFS/BFS 表达或理解相关的具体错误，而不是泛泛说「我不太会」。")
                                .instructions("写 2～6 句：说清楚「错在哪」；可对比你当时怎么想。")
                                .userOutputLabel("我的典型错误")
                                .allowedPrompts(List.of(
                                        "我曾把…当成…",
                                        "我写的时候漏说了…"
                                ))
                                .forbiddenPrompts(List.of(
                                        "空泛：我不太会 / 理解不深",
                                        "只写一句口号"
                                ))
                                .passCriteria(List.of(
                                        "具体错误描述",
                                        "与前面学习过程相关"
                                ))
                                .nextActionHint("下一张：这个错误为什么会发生。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_ROOT_CAUSE)
                                .title("这个错误为什么会发生？")
                                .goal("说出根因：背定义、机制不清、因果链断、表达模糊等，并对应上一张的错误。")
                                .instructions("写 2～6 句：用「因为…所以…」或「根因是…」。")
                                .userOutputLabel("错误根因")
                                .allowedPrompts(List.of(
                                        "根因是我只会背…",
                                        "因果链在…断了"
                                ))
                                .forbiddenPrompts(List.of(
                                        "重复上一张原句不改",
                                        "只说「粗心」"
                                ))
                                .passCriteria(List.of(
                                        "至少一个清晰原因",
                                        "能对应典型错误"
                                ))
                                .nextActionHint("下一张：形成可迁移的判断规则。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_DECISION_RULE)
                                .title("以后你怎么判断该优先想到 DFS 还是 BFS？")
                                .goal("用「遇到什么情况 → 优先想哪种搜索」写规则，而不是复述定义。")
                                .instructions("写 3～8 句：尽量包含条件词（当/如果/优先）并同时提到两种思路的特征。")
                                .userOutputLabel("我的判断规则")
                                .allowedPrompts(List.of(
                                        "当问题强调层次扩展…",
                                        "如果需要先处理更近的…"
                                ))
                                .forbiddenPrompts(List.of(
                                        "只写「DFS 深度 BFS 广度」",
                                        "纯背术语无场景"
                                ))
                                .passCriteria(List.of(
                                        "可迁移规则",
                                        "场景/特征 → 方法"
                                ))
                                .nextActionHint("下一张：给这次能力命名。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_CAPABILITY_NAME)
                                .title("你这次真正获得的能力是什么？")
                                .goal("用一句可复用的能力表述收尾，而不是「学会了 DFS/BFS」。")
                                .instructions("写 2～5 句：能力要具体到能解释机制、对比或因果链。")
                                .userOutputLabel("我获得的能力")
                                .allowedPrompts(List.of(
                                        "我能根据…判断…",
                                        "我能把…的因果链讲清楚"
                                ))
                                .forbiddenPrompts(List.of(
                                        "泛泛：学会了 DFS/BFS",
                                        "只列知识点名词"
                                ))
                                .passCriteria(List.of(
                                        "具体能力表述",
                                        "可复用、可检验"
                                ))
                                .nextActionHint("完成后将生成反思沉淀并进入探索。")
                                .build()
                ))
                .build();
    }
}
