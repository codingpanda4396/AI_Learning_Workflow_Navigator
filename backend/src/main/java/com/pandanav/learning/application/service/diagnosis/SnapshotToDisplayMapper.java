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
            strengths.isEmpty() ? List.of("目标与起点已明确，将据此安排学习节奏。") : strengths,
            weaknesses.isEmpty() ? List.of("将在后续训练中继续定位薄弱点。") : weaknesses,
            learningPreference,
            timeBudgetDto,
            goalOrientation
        );
    }

    private static List<String> deriveStrengths(LearnerProfileStructuredSnapshotDto snapshot) {
        List<String> out = new ArrayList<>();
        String foundation = nullToEmpty(snapshot.foundationLevel());
        String goalType = nullToEmpty(snapshot.goalType());
        String practice = nullToEmpty(snapshot.practiceLevel());
        if ("ADVANCED".equals(foundation)) {
            out.add("基础相对扎实，可以更快进入综合应用。");
        }
        if ("MANY".equals(practice) && !"BEGINNER".equals(foundation)) {
            out.add("已有较多练习或实际使用经验，适合结合案例推进。");
        }
        if (!goalType.isBlank()) {
            out.add("学习目标明确，便于安排个性化内容。");
        }
        return out;
    }

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
                if ("FOUNDATION_GAP".equals(tag) && !out.stream().anyMatch(s -> s.contains("基础"))) {
                    continue;
                }
                if ("TRANSFER_WEAKNESS".equals(tag)) {
                    out.add("变形与迁移还不稳定，需要针对性巩固。");
                } else if ("EXPRESSION_WEAKNESS".equals(tag)) {
                    out.add("表达与归纳还需加强。");
                } else if ("INTERVIEW_FOUNDATION_RISK".equals(tag)) {
                    out.add("面试目标下基础尚需扎牢，建议先稳核心再刷题。");
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
