package navigator.domain.policy.tutor;

import navigator.domain.enums.FeedbackStyle;
import navigator.domain.enums.GuidanceIntent;
import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.model.GuidanceDecision;
import navigator.domain.model.LearnerStrategyProfile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Sprint 4 MVP：表驱动规则，后续可换 YAML 加载。
 */
@Component
public class TutorInteractionPolicy {

    public static final String POLICY_VERSION = "sprint4-mvp-1";

    public GuidanceDecision decide(LearningGuidancePhase phase,
                                   LearningActionType action,
                                   LearnerStrategyProfile strategy) {
        FeedbackStyle style = strategy != null ? strategy.getFeedbackStyle() : null;
        boolean directStyle = style == FeedbackStyle.DIRECT;

        if (action == LearningActionType.SEEK_DIRECT_ANSWER) {
            return GuidanceDecision.builder()
                    .phase(phase)
                    .intent(GuidanceIntent.REDIRECT_OFF_TASK)
                    .allowSubstantiveAnswer(false)
                    .mandatoryBehaviors(List.of("不得输出可直接照抄的最终答案或完整解题步骤"))
                    .policyRuleId("block_direct_answer")
                    .promptSlots(Map.of(
                            "focus", "引导用户说出卡点与已尝试思路",
                            "forced_reply", "直接给答案不利于形成自己的理解。请先试着用一句话说出你卡住的点，我们再从最小例子切入。"
                    ))
                    .build();
        }
        if (action == LearningActionType.OFF_TOPIC) {
            return GuidanceDecision.builder()
                    .phase(phase)
                    .intent(GuidanceIntent.REDIRECT_OFF_TASK)
                    .allowSubstantiveAnswer(false)
                    .policyRuleId("redirect_off_topic")
                    .promptSlots(Map.of(
                            "forced_reply", "" // 由编排层填入 goal
                    ))
                    .build();
        }
        if (action == LearningActionType.CONFUSION_SIGNAL) {
            return GuidanceDecision.builder()
                    .phase(phase)
                    .intent(GuidanceIntent.ASK_CLARIFYING_QUESTION)
                    .allowSubstantiveAnswer(directStyle)
                    .policyRuleId("confusion_probe")
                    .promptSlots(Map.of("focus", "用最小例子定位不懂的环节"))
                    .build();
        }

        if (action == LearningActionType.ASK_FOR_EXAMPLE
                || action == LearningActionType.ASK_FOR_COMPARISON
                || action == LearningActionType.ASK_FOR_SIMPLIFICATION
                || action == LearningActionType.ASK_FOR_EXPLANATION) {
            return GuidanceDecision.builder()
                    .phase(phase)
                    .intent(GuidanceIntent.GIVE_SCAFFOLD_HINT)
                    .allowSubstantiveAnswer(false)
                    .policyRuleId("active_questioning")
                    .promptSlots(Map.of("focus", "用提示与追问回应，不代替用户完成推导"))
                    .build();
        }

        return phaseDefault(phase, directStyle);
    }

    private static GuidanceDecision phaseDefault(LearningGuidancePhase phase, boolean directStyle) {
        return switch (phase) {
            case CLARIFY_GOAL -> GuidanceDecision.builder()
                    .phase(phase)
                    .intent(GuidanceIntent.ASK_CLARIFYING_QUESTION)
                    .allowSubstantiveAnswer(directStyle)
                    .policyRuleId("phase_clarify_goal")
                    .promptSlots(Map.of("focus", "对齐任务目标与完成标准"))
                    .build();
            case BUILD_FRAME -> GuidanceDecision.builder()
                    .phase(phase)
                    .intent(GuidanceIntent.NUDGE_DECOMPOSE)
                    .allowSubstantiveAnswer(false)
                    .policyRuleId("phase_build_frame")
                    .promptSlots(Map.of("focus", "帮助列出关键概念或步骤骨架"))
                    .build();
            case TRY_EXPRESS -> GuidanceDecision.builder()
                    .phase(phase)
                    .intent(GuidanceIntent.REQUEST_SELF_ARTICULATION)
                    .allowSubstantiveAnswer(false)
                    .policyRuleId("phase_try_express")
                    .promptSlots(Map.of("focus", "要求用户用自己的话先尝试表达"))
                    .build();
            case PROBE_GAPS -> GuidanceDecision.builder()
                    .phase(phase)
                    .intent(GuidanceIntent.ASK_CLARIFYING_QUESTION)
                    .allowSubstantiveAnswer(false)
                    .policyRuleId("phase_probe_gaps")
                    .promptSlots(Map.of("focus", "追问边界、反例或易混点"))
                    .build();
            case META_REFLECT -> GuidanceDecision.builder()
                    .phase(phase)
                    .intent(GuidanceIntent.SUMMARY_PREP)
                    .allowSubstantiveAnswer(false)
                    .policyRuleId("phase_meta_reflect")
                    .promptSlots(Map.of("focus", "引导反思仍模糊处与下一步练习"))
                    .build();
            case TRANSITION_HINT -> GuidanceDecision.builder()
                    .phase(phase)
                    .intent(GuidanceIntent.SUMMARY_PREP)
                    .allowSubstantiveAnswer(false)
                    .policyRuleId("phase_transition_hint")
                    .promptSlots(Map.of(
                            "focus", "提示可以进入自我解释环节巩固理解",
                            "next_step", "调用 self-explanation 接口提交用自己的话总结"
                    ))
                    .build();
        };
    }
}
