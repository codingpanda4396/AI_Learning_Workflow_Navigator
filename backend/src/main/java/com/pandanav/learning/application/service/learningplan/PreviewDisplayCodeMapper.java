package com.pandanav.learning.application.service.learningplan;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PreviewDisplayCodeMapper {

    public String learningPreference(String code) {
        return switch (normalize(code)) {
            case "PRACTICE_FIRST", "PRACTICE_THEN_LEARN" -> "先练再纠偏";
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

    public String capabilityLevel(String code) {
        return switch (normalize(code)) {
            case "BASIC", "BEGINNER", "WEAK" -> "学过相关内容，但基础还不稳定";
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

    public String foundationLevel(String code) {
        return switch (normalize(code)) {
            case "NONE", "BASIC", "BEGINNER", "WEAK" -> "学过相关内容，但基础还不稳定";
            case "INTERMEDIATE", "PARTIAL", "COURSEWORK" -> "有一定基础，仍需针对性巩固";
            case "ADVANCED", "PROFICIENT", "STABLE" -> "基础较好，可进入强化阶段";
            default -> "";
        };
    }

    /** riskTags 中文展示（与 snapshot.riskTags 一致）. */
    public String riskFlagLabel(String code) {
        return switch (normalize(code)) {
            case "FOUNDATION_GAP" -> "基础仍需补齐";
            case "TRANSFER_WEAKNESS" -> "一到变形应用就不稳定";
            case "EXPRESSION_WEAKNESS" -> "会做但表达不清";
            case "BOUNDARY_WEAKNESS" -> "边界条件容易错";
            case "INTERVIEW_FOUNDATION_RISK" -> "面试目标但基础尚不稳";
            default -> code != null && !code.isBlank() ? code : "";
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

    private String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}
