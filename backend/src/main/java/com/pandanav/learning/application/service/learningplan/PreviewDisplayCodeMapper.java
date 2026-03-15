package com.pandanav.learning.application.service.learningplan;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class PreviewDisplayCodeMapper {

    /** 与 submissions / ContractCatalog.SNAPSHOT_PREFERENCE_LABELS 一致，单一事实源。 */
    public String learningPreference(String code) {
        return switch (normalize(code)) {
            case "TEXT_FIRST" -> "先看文字讲解";
            case "VISUAL_FIRST" -> "先看图解或结构示意";
            case "CODE_FIRST" -> "先看代码示例";
            case "PRACTICE_FIRST", "PRACTICE_THEN_LEARN" -> "先做小题再总结";
            case "CONCEPT_FIRST", "LEARN_THEN_PRACTICE" -> "先理解概念再练习";
            case "EXAMPLE_FIRST" -> "先看例子再总结";
            case "PROJECT_DRIVEN" -> "边做边学";
            case "MIXED" -> "讲练结合";
            default -> "";
        };
    }

    public String timeBudget(String code) {
        return switch (normalize(code)) {
            case "LIGHT" -> "每周 1-3 小时";
            case "STANDARD", "NORMAL" -> "每周 4-6 小时";
            case "INTENSIVE" -> "每周 7-10 小时";
            case "IMMERSIVE" -> "每周 10 小时以上";
            case "SHORT_10" -> "10 分钟左右";
            case "MEDIUM_30" -> "20~30 分钟";
            case "LONG_60" -> "40~60 分钟";
            case "SYSTEMATIC" -> "可以系统学一轮";
            default -> "时间节奏待补充";
        };
    }

    public String goalOrientation(String code) {
        return switch (normalize(code)) {
            case "EXAM", "EXAM_PREP" -> "准备考试或测验";
            case "COURSE" -> "应对课程学习与作业";
            case "INTERVIEW" -> "准备实习或求职面试";
            case "PROJECT", "PROJECT_DRIVEN", "QUICK_START" -> "完成项目或作品";
            case "UNDERSTAND_PRINCIPLE" -> "系统理解核心原理";
            case "REVIEW_FIX" -> "查漏补缺巩固基础";
            default -> "";
        };
    }

    /** 与 foundationLevel 表述一致，供无 snapshot.summary 时兜底。 */
    public String capabilityLevel(String code) {
        return switch (normalize(code)) {
            case "NONE", "BEGINNER", "WEAK" -> "刚开始接触";
            case "BASIC" -> "学过但还不太熟";
            case "INTERMEDIATE", "PARTIAL" -> "有一定基础，正在向稳定应用过渡";
            case "ADVANCED", "PROFICIENT", "STABLE" -> "基础较好，可开始更高阶训练";
            default -> "";
        };
    }

    /** 主要卡点 → 展示（与 snapshot.primaryBlocker 一致）. */
    public String primaryBlockerLabel(String code) {
        return switch (normalize(code)) {
            case "CONCEPT_CONFUSION" -> "概念本身不太清楚";
            case "FOLLOW_BUT_CANNOT_DO" -> "看懂例子但不会独立做";
            case "BASIC_OK_BUT_FAIL_ON_VARIATION" -> "一变形就容易卡住";
            case "CAN_DO_BUT_CANNOT_EXPLAIN" -> "会做但表达不清";
            case "BOUNDARY_WEAKNESS" -> "边界条件容易错";
            default -> "";
        };
    }

    /** 与 submissions 画像一致：BEGINNER 对应「刚开始接触」，单一事实源。 */
    public String foundationLevel(String code) {
        return switch (normalize(code)) {
            case "NONE", "BEGINNER", "WEAK" -> "刚开始接触";
            case "BASIC" -> "学过但还不太熟";
            case "INTERMEDIATE", "PARTIAL", "COURSEWORK" -> "有一定基础，仍需针对性巩固";
            case "ADVANCED", "PROFICIENT", "STABLE" -> "基础较好，可进入强化阶段";
            default -> "";
        };
    }

    /** riskTags 中文展示（与 snapshot.riskTags 一致，preview 强约束输入源）. */
    public String riskFlagLabel(String code) {
        return switch (normalize(code)) {
            case "FOUNDATION_GAP" -> "基础仍需补齐";
            case "TRANSFER_WEAKNESS" -> "一到变形应用就不稳定";
            case "EXPRESSION_WEAKNESS" -> "会做但表达不清";
            case "BOUNDARY_WEAKNESS" -> "边界条件容易错";
            case "INTERVIEW_FOUNDATION_RISK" -> "面试目标但基础尚不稳";
            case "PROCESS_CONFUSION" -> "操作步骤容易混淆";
            case "INDEPENDENT_SOLVING_WEAKNESS" -> "独立解题还不足";
            case "EXAM_ORIENTED_SURFACE_LEARNING_RISK" -> "考试导向下需先稳概念";
            case "CONCEPT_NOT_STABLE" -> "核心概念还不稳";
            default -> code != null && !code.isBlank() ? code : "";
        };
    }

    /** 节奏/任务粒度展示，与 submissions planExplanation 一致. */
    public String paceLabel(String code) {
        return switch (normalize(code)) {
            case "FAST" -> "节奏稍快";
            case "DEEP" -> "节奏稍慢、把每一步走稳";
            case "NORMAL" -> "常规节奏";
            default -> "";
        };
    }

    public String taskGranularityLabel(String code) {
        return switch (normalize(code)) {
            case "SMALL" -> "小步拆解";
            case "MEDIUM" -> "中等步长";
            case "LARGE" -> "较大单元";
            default -> "";
        };
    }

    /** 诊断画像推荐策略：入口模式 → 中文标题。 */
    public String recommendedStrategyTitle(String code) {
        return switch (normalize(code)) {
            case "FOUNDATION_FIRST" -> "先补基础再推进";
            case "VISUAL_ENTRY", "VISUAL_WITH_MINI_EXAMPLE" -> "图解优先进入";
            case "EXAMPLE_FIRST" -> "先看示例再动手";
            case "EXAMPLE_THEN_RULE", "EXAMPLE_TO_RULE" -> "示例归纳再规则";
            case "INTERVIEW_ORIENTED", "HIGH_FREQUENCY_PATTERNS" -> "面试高频优先";
            case "PROJECT_DRIVEN", "IMPLEMENTATION_FLOW" -> "按实现流程推进";
            default -> "按当前起点稳步推进";
        };
    }

    /** 诊断画像推荐策略：入口模式 → 推荐理由（一句中文）。 */
    public String recommendedStrategyReason(String code) {
        return switch (normalize(code)) {
            case "FOUNDATION_FIRST" -> "你当前更适合先把概念和结构搞清楚，再做题会更稳。";
            case "VISUAL_ENTRY", "VISUAL_WITH_MINI_EXAMPLE" -> "你更适合先看图解或结构示意，所以先从图示入手。";
            case "EXAMPLE_FIRST" -> "你看懂例子后自己动手容易卡住，先多跟几个示例再独立练。";
            case "EXAMPLE_THEN_RULE", "EXAMPLE_TO_RULE" -> "你基础题会但变形易卡，先通过示例归纳规律再练变形。";
            case "INTERVIEW_ORIENTED", "HIGH_FREQUENCY_PATTERNS" -> "你目标是面试，优先练高频考点和常见套路。";
            case "PROJECT_DRIVEN", "IMPLEMENTATION_FLOW" -> "你更偏向项目实践，按实现流程和常见场景安排。";
            default -> "根据你的诊断结果，先完成这一步更有利于后续推进。";
        };
    }

    /** 非推荐策略的 notRecommendedReason，与策略含义一致，避免泛泛而谈。 */
    public String alternativeNotRecommendedReason(String strategyCode) {
        return switch (normalize(strategyCode)) {
            case "FAST_TRACK" -> "你当前基础更适合先稳一步再推进，直接快跑容易在概念连接处卡住。";
            case "PRACTICE_FIRST" -> "概念和表示还没稳，先练容易反复试错，建议先理清再练。";
            case "EXAMPLE_FIRST" -> "当前更需要先建立结构和路径的直观认识，再看示例会更顺。";
            default -> "当前证据更支持先稳住这一步，再考虑其他方式。";
        };
    }

    private static final Pattern FOUNDATION_OF = Pattern.compile("(?i)Foundation of\\s+(\\S+)");

    /** 清除用户文案中的内部概念泄漏（如 "Foundation of 图"），统一为中文表达。 */
    public static String sanitizeUserFacingText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return FOUNDATION_OF.matcher(text.trim()).replaceAll("$1 基础");
    }

    private String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}
