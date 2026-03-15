package com.pandanav.learning.application.service.diagnosis;

import com.pandanav.learning.api.contract.ContractCatalog;
import com.pandanav.learning.api.dto.CodeLabelDto;
import com.pandanav.learning.api.dto.diagnosis.CapabilityProfileDto;
import com.pandanav.learning.api.dto.diagnosis.LearnerProfileStructuredSnapshotDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 learnerProfileSnapshot 映射为 submissions 接口的展示层 capabilityProfile。
 * capabilityProfile 仅为 snapshot 的展示映射，不是第二套推导逻辑；与 snapshot 语义一致，不产生冲突。
 */
@Component
public class SnapshotToDisplayMapper {

    /**
     * 从唯一主画像 learnerProfileSnapshot 派生展示用 capabilityProfile。
     */
    public CapabilityProfileDto toCapabilityProfileDto(LearnerProfileStructuredSnapshotDto snapshot) {
        if (snapshot == null) {
            return emptyProfile();
        }
        String foundation = nullToEmpty(snapshot.foundationLevel());
        String goalType = nullToEmpty(snapshot.goalType());
        String preference = nullToEmpty(snapshot.learningPreference());
        String timeBudget = nullToEmpty(snapshot.timeBudget());

        CodeLabelDto currentLevel = ContractCatalog.snapshotFoundationLevel(foundation);
        CodeLabelDto goalOrientation = ContractCatalog.snapshotGoalType(goalType);
        CodeLabelDto learningPreference = ContractCatalog.snapshotPreference(preference);
        CodeLabelDto timeBudgetDto = ContractCatalog.snapshotTimeBudgetSession(timeBudget);
        if (timeBudgetDto.code().isBlank() && !timeBudget.isBlank()) {
            timeBudgetDto = ContractCatalog.timeBudget(timeBudget);
        }

        List<String> strengths = deriveStrengths(snapshot);
        List<String> weaknesses = deriveWeaknesses(snapshot);

        return new CapabilityProfileDto(
            currentLevel,
            strengths.isEmpty() ? List.of("已根据你的选择确定起点与目标，将据此安排学习节奏。") : strengths,
            weaknesses.isEmpty() ? List.of("将在后续训练中继续定位薄弱点。") : weaknesses,
            learningPreference,
            timeBudgetDto,
            goalOrientation
        );
    }

    /** 从用户回答提炼对学习有帮助的真实优势，避免空泛表述。 */
    private static List<String> deriveStrengths(LearnerProfileStructuredSnapshotDto snapshot) {
        List<String> out = new ArrayList<>();
        String foundation = nullToEmpty(snapshot.foundationLevel());
        String goalType = nullToEmpty(snapshot.goalType());
        String practice = nullToEmpty(snapshot.practiceLevel());
        String preference = nullToEmpty(snapshot.learningPreference());
        String timeBudget = nullToEmpty(snapshot.timeBudget());
        if ("ADVANCED".equals(foundation) || "PROFICIENT".equals(foundation)) {
            out.add("基础相对扎实，可以更快进入综合应用。");
        }
        if ("MANY".equals(practice) && !"BEGINNER".equals(foundation)) {
            out.add("已有较多练习或实际使用经验，适合结合案例推进。");
        }
        if (!goalType.isBlank()) {
            if ("INTERVIEW".equals(goalType) || "EXAM".equals(goalType)) {
                out.add("目标清晰（面试/考试导向），便于安排针对性训练。");
            } else if ("PROJECT".equals(goalType)) {
                out.add("以项目或实践为导向，适合按场景推进。");
            } else {
                out.add("学习目标明确，便于安排个性化内容。");
            }
        }
        if (!preference.isBlank()) {
            out.add("已选择学习偏好，后续讲解与练习会按此调整。");
        }
        if (!timeBudget.isBlank() && !"SHORT_10".equals(timeBudget)) {
            out.add("时间投入较充足，可支持更完整的学习节奏。");
        }
        return out;
    }

    /** 与风险标签联动、具体可感，不制造焦虑。 */
    private static List<String> deriveWeaknesses(LearnerProfileStructuredSnapshotDto snapshot) {
        List<String> out = new ArrayList<>();
        List<String> riskTags = snapshot.riskTags();
        String foundation = nullToEmpty(snapshot.foundationLevel());
        String blocker = nullToEmpty(snapshot.primaryBlocker());
        String practice = nullToEmpty(snapshot.practiceLevel());
        if ("BEGINNER".equals(foundation)) {
            out.add("当前基础还不够稳定，需要先补齐关键概念。");
        }
        if ("NONE".equals(practice)) {
            out.add("相关练习较少，起步阶段更需要例子和分步练习。");
        }
        if ("FOLLOW_BUT_CANNOT_DO".equals(blocker)) {
            out.add("看懂例子但独立完成还不足，需要更多从模仿到独立的练习。");
        }
        if (riskTags != null) {
            for (String tag : riskTags) {
                if ("FOUNDATION_GAP".equals(tag) && out.stream().noneMatch(s -> s.contains("基础"))) {
                    continue;
                }
                switch (tag) {
                    case "TRANSFER_WEAKNESS" -> out.add("变形与迁移还不稳定，需要针对性巩固。");
                    case "EXPRESSION_WEAKNESS" -> out.add("表达与归纳还需加强。");
                    case "INTERVIEW_FOUNDATION_RISK" -> out.add("面试目标下基础尚需扎牢，建议先稳核心再刷题。");
                    case "PROCESS_CONFUSION" -> out.add("操作步骤容易混淆，需要先理清流程再练。");
                    case "INDEPENDENT_SOLVING_WEAKNESS" -> {
                        if (out.stream().noneMatch(s -> s.contains("模仿到独立"))) {
                            out.add("独立解题还不足，会多安排从模仿到独立的练习。");
                        }
                    }
                    case "EXAM_ORIENTED_SURFACE_LEARNING_RISK" -> out.add("考试导向下建议先稳概念再刷题，避免只记套路。");
                    case "CONCEPT_NOT_STABLE" -> out.add("核心概念还不稳，建议先巩固定义与结构。");
                    case "BOUNDARY_WEAKNESS" -> out.add("边界与特殊情况容易出错，后续会加强这类练习。");
                    default -> { }
                }
            }
        }
        return out;
    }

    private static CapabilityProfileDto emptyProfile() {
        return new CapabilityProfileDto(
            ContractCatalog.snapshotFoundationLevel(""),
            List.of(),
            List.of(),
            ContractCatalog.snapshotPreference(""),
            ContractCatalog.snapshotTimeBudgetSession(""),
            ContractCatalog.snapshotGoalType("")
        );
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
