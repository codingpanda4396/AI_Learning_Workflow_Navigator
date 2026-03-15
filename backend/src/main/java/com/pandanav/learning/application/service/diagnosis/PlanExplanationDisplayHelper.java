package com.pandanav.learning.application.service.diagnosis;

import java.util.List;

/**
 * 将 planHints / riskTags 转为用户可见文案，禁止直接输出内部 code。
 * 与 PreviewDisplayCodeMapper 语义一致，供 submissions 的 planExplanation 使用。
 */
public final class PlanExplanationDisplayHelper {

    private PlanExplanationDisplayHelper() {}

    public static String entryModeToLabel(String entryMode) {
        if (entryMode == null || entryMode.isBlank()) return "按当前起点稳步推进";
        return switch (entryMode.trim().toUpperCase()) {
            case "FOUNDATION_FIRST" -> "先补基础再推进";
            case "EXAMPLE_FIRST" -> "先看示例再动手";
            case "EXAMPLE_THEN_RULE" -> "示例归纳再规则";
            case "VISUAL_WITH_MINI_EXAMPLE", "VISUAL_ENTRY" -> "图解优先进入";
            case "CODE_WITH_MINI_VISUAL" -> "代码示例优先";
            default -> "按当前起点稳步推进";
        };
    }

    public static String paceToLabel(String pace) {
        if (pace == null || pace.isBlank()) return "";
        return switch (pace.trim().toUpperCase()) {
            case "FAST" -> "节奏稍快";
            case "DEEP" -> "节奏稍慢、把每一步走稳";
            case "NORMAL" -> "常规节奏";
            default -> "";
        };
    }

    public static String taskGranularityToLabel(String taskGranularity) {
        if (taskGranularity == null || taskGranularity.isBlank()) return "";
        return switch (taskGranularity.trim().toUpperCase()) {
            case "SMALL" -> "小步拆解";
            case "MEDIUM" -> "中等步长";
            case "LARGE" -> "较大单元";
            default -> "";
        };
    }

    public static String riskTagToLabel(String riskTag) {
        if (riskTag == null || riskTag.isBlank()) return "";
        return switch (riskTag.trim().toUpperCase()) {
            case "FOUNDATION_GAP" -> "基础仍需补齐";
            case "TRANSFER_WEAKNESS" -> "一到变形应用就不稳定";
            case "EXPRESSION_WEAKNESS" -> "会做但表达不清";
            case "BOUNDARY_WEAKNESS" -> "边界条件容易错";
            case "INTERVIEW_FOUNDATION_RISK" -> "面试目标但基础尚不稳";
            case "PROCESS_CONFUSION" -> "操作步骤容易混淆";
            case "INDEPENDENT_SOLVING_WEAKNESS" -> "独立解题还不足";
            case "EXAM_ORIENTED_SURFACE_LEARNING_RISK" -> "考试导向下需先稳概念";
            case "CONCEPT_NOT_STABLE" -> "核心概念还不稳";
            default -> "";
        };
    }

    public static List<String> riskTagsToLabels(List<String> riskTags) {
        if (riskTags == null) return List.of();
        return riskTags.stream()
            .map(PlanExplanationDisplayHelper::riskTagToLabel)
            .filter(s -> !s.isBlank())
            .distinct()
            .toList();
    }
}
