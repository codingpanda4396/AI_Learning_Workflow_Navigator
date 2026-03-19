package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.TaskType;

import java.util.List;
import java.util.Map;

/**
 * 可执行任务规格：执行引擎与脚手架/导师编排的合同对象。
 *
 * 与 TaskBlueprint 的关系：
 * - TaskBlueprint 面向“计划展示与摘要”
 * - ExecutableTaskSpec 面向“执行约束与可判定完成”
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutableTaskSpec {
    private String taskId;
    private TaskType taskType;
    private String title;

    private Integer estimatedMinutes;
    private TimeBoxPolicy timeBoxPolicy;

    private InputsRequired inputsRequired;
    private OutputsExpected outputsExpected;

    /** 可判定的完成标准（尽量枚举化/短句化） */
    private List<String> completionCriteria;
    /** 执行期需要沉淀的证据类型/条目 */
    private List<String> evidenceToCollect;

    /** 评测规则（checkpoint/self-explain 维度与阈值） */
    private EvaluationRubric evaluationRubric;
    /** 执行流策略（是否启用 orient/explore/self-explain/checkpoint 以及上限） */
    private ScaffoldPolicy scaffoldPolicy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputsRequired {
        /** 允许使用的资源/工具（示例：IDE, 官方文档, LLM） */
        private List<String> allowedResources;
        /** 前置条件（示例：已完成某任务/已掌握某概念） */
        private List<String> prerequisites;
        /** 限制条款（示例：不允许直接复制答案） */
        private List<String> constraints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputsExpected {
        /** 产物类型（示例：代码片段/笔记/对比表/口头复述） */
        private String outputType;
        /** 必要要素（示例：必须包含 2 个例子/必须给出复杂度分析） */
        private List<String> requiredElements;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationRubric {
        /** 维度 -> 要求（简单 map 先落地，后续可演进为结构化维度对象） */
        private Map<String, String> dimensions;
        /** 通过阈值（示例：覆盖 2/3 关键点） */
        private String passThreshold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScaffoldPolicy {
        private boolean enableOrient;
        private boolean enableExplore;
        private boolean enableSelfExplain;
        private boolean enableCheckpoint;

        private Integer maxExploreTurns;
        private Integer maxRemedialTurns;
    }

    public enum TimeBoxPolicy {
        SOFT,
        HARD
    }
}

