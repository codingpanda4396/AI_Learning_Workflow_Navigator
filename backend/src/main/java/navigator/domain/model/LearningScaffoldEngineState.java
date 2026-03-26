package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.api.dto.scaffold.ReflectionInsight;
import navigator.api.dto.scaffold.ReflectionRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习脚手架引擎状态（JSON 存入 scaffoldJson，避免新增 DB 列）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningScaffoldEngineState {
    private String currentStageKey;
    private String currentActionId;
    /** STRUCTURE 三卡全部完成（与 completedStageKeys 同步，兼容旧 JSON） */
    private boolean structureStageComplete;
    /** UNDERSTANDING 两卡全部完成（与 completedStageKeys 同步，兼容旧 JSON） */
    private boolean understandingStageComplete;
    /** 已完成的阶段键（顺序追加，如 STRUCTURE → UNDERSTANDING → TRAINING → REFLECTION） */
    @Builder.Default
    private List<String> completedStageKeys = new ArrayList<>();
    @Builder.Default
    private Map<String, ScaffoldActionRuntimeEntry> actionRuntimeByActionId = new LinkedHashMap<>();
    /** REFLECTION 阶段全部完成后由汇编器写入 */
    private ReflectionRecord reflectionRecord;
    private ReflectionInsight reflectionInsight;

    /** STRUCTURE：已点过并生成过骨架的 promptKey（顺序保留） */
    @Builder.Default
    private List<String> structureExploredPromptKeys = new ArrayList<>();
    /** STRUCTURE：成功生成骨架的次数 */
    @Builder.Default
    private int structureGenerationCount = 0;
    /** STRUCTURE：轻反馈次数（再解释 / 相邻关系等） */
    @Builder.Default
    private int structureLightInteractionCount = 0;
    /** STRUCTURE：最近一次脚手架卡 */
    private String structureLastPromptKey;
    /** STRUCTURE：完成阶段时可选的一句话 */
    private String structureOptionalReflection;
}
