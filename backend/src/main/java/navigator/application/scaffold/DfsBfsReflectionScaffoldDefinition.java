package navigator.application.scaffold;

import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.StageScaffold;

import java.util.List;

/**
 * DFS/BFS reflection stage cards.
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
                .stageTitle("反思收束")
                .stageGoal("把这次暴露出的错误压成以后可复用的判断规则。")
                .phaseGoal("我要把这次暴露出的错误压成以后可复用的判断规则。")
                .stageDescription("每张卡只回答一个点，系统只打回空话、泛话和定义复读。")
                .validatorType("REFLECTION_RULES")
                .tutorMode("REFLECTION_CONSTRAINED")
                .actionCards(List.of(
                        LearningActionCard.builder()
                                .actionId(ACTION_ERROR_RECALL)
                                .title("我最容易犯的一个错")
                                .goal("回忆一个具体错误，而不是写空泛总结。")
                                .singleAction("写出一个与这轮 DFS / BFS 训练直接相关的具体错误。")
                                .instructions("用 2-3 句写清『错在哪』。")
                                .systemPrompt("不要写『我理解更深了』。请写一个具体被抓到的错误。")
                                .llmRole("反思压缩器")
                                .userOutputLabel("典型错误")
                                .allowedPrompts(List.of(
                                        "我曾把……当成……",
                                        "我写的时候漏掉了……"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要写『我不太会』这类空话",
                                        "不要只写一句口号"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止空泛总结",
                                        "禁止只写『我不会 / 我不太懂』而不指出具体错误"
                                ))
                                .passCriteria(List.of(
                                        "错误描述具体",
                                        "与本轮训练相关"
                                ))
                                .completionCriteria(List.of(
                                        "错误描述具体，且与本轮训练相关"
                                ))
                                .nextActionHint("下一张：这个错为什么会发生。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_ROOT_CAUSE)
                                .title("这个错为什么会发生")
                                .goal("把错误对应到真正的根因，而不是空因。")
                                .singleAction("用一句根因句说明：这个错为什么会发生。")
                                .instructions("优先使用『因为……所以……』或『根因是……』。")
                                .systemPrompt("不要写『粗心』『基础不好』这种空因。请对应上一张的错误，把根因写出来。")
                                .llmRole("反思压缩器")
                                .userOutputLabel("错误根因")
                                .allowedPrompts(List.of(
                                        "根因是我只会背……",
                                        "因果链在……断了"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要把上一张原话重复一遍",
                                        "不要只写『粗心』"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止把『错误』原句原样重复",
                                        "禁止写空因"
                                ))
                                .passCriteria(List.of(
                                        "根因清楚",
                                        "根因能对应上一张错误"
                                ))
                                .completionCriteria(List.of(
                                        "根因不是『粗心』『基础不好』这种空因"
                                ))
                                .nextActionHint("下一张：以后看到什么情况优先想到 DFS / BFS。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_DECISION_RULE)
                                .title("以后看到什么情况优先想到 DFS / BFS")
                                .goal("形成能迁移的判断规则，而不是复读定义。")
                                .singleAction("写成条件句：遇到什么情况时，我优先想到 DFS 或 BFS。")
                                .instructions("尽量写成『遇到……时，我优先……，因为……』。")
                                .systemPrompt("不要复读定义。请写成『遇到____时，我优先____，因为____』。")
                                .llmRole("反思压缩器")
                                .userOutputLabel("判断规则")
                                .allowedPrompts(List.of(
                                        "当问题强调层次扩展……",
                                        "如果需要先处理更近的……"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要只写『DFS 深度 BFS 广度』",
                                        "不要没有场景条件"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止复读定义",
                                        "禁止不带条件和场景的空规则"
                                ))
                                .passCriteria(List.of(
                                        "规则包含条件",
                                        "规则包含方法映射"
                                ))
                                .completionCriteria(List.of(
                                        "判断规则包含条件与方法映射"
                                ))
                                .nextActionHint("最后一张：我这次真正获得的能力是什么。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_CAPABILITY_NAME)
                                .title("我这次真正获得的能力是什么")
                                .goal("把能力写成能检验、可复用的句子。")
                                .singleAction("用『我能……』写出一条可检验的能力表述。")
                                .instructions("能力必须具体到判断、解释、对比或因果链。")
                                .systemPrompt("不要写『学会了 DFS/BFS』。请写你以后能独立做出的判断。")
                                .llmRole("反思压缩器")
                                .userOutputLabel("获得的能力")
                                .allowedPrompts(List.of(
                                        "我能根据……判断……",
                                        "我能把……的因果链讲清楚"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要写知识点名词",
                                        "不要写泛化鸡汤"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止把『能力』写成知识点名词",
                                        "禁止脱离本轮训练内容写泛化鸡汤"
                                ))
                                .passCriteria(List.of(
                                        "以『我能……』开头",
                                        "能力可检验、可复用"
                                ))
                                .completionCriteria(List.of(
                                        "能力表述以『我能……』开头，并可被检验",
                                        "最终可沉淀为报告页中的一条『下次可复用规则』"
                                ))
                                .nextActionHint("完成后生成反思沉淀。")
                                .build()
                ))
                .build();
    }
}
