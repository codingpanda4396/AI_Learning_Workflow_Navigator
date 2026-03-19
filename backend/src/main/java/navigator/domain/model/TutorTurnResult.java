package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.LearningActionType;

import java.util.List;

/**
 * 单回合导师产物（结构化）：允许 LLM 辅助生成表达，但必须可被执行引擎消费。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorTurnResult {
    private String assistantReply;
    private LearningActionType detectedAction;

    /** 对用户输入的语义归纳（执行证据/报告可消费） */
    private String semanticSummary;
    /** 候选追问/下一步提示（受 scaffoldPolicy 约束） */
    private List<String> suggestedFollowups;

    /** 教学解释草稿（可选，允许 LLM） */
    private String explanationDraft;
    /** 从用户输入中抽取的证据条目 */
    private List<EvidenceItem> evidenceExtracts;

    /** LLM 不可用/降级标记：NONE/MOCK/TEMPLATE/... */
    private String fallbackMode;
}

