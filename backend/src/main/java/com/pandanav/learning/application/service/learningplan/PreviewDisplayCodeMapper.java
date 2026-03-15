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
            default -> "按你的节奏灵活安排";
        };
    }

    public String timeBudget(String code) {
        return switch (normalize(code)) {
            case "LIGHT" -> "每周 1-3 小时";
            case "STANDARD", "NORMAL" -> "每周 4-6 小时";
            case "INTENSIVE" -> "每周 7-10 小时";
            case "IMMERSIVE" -> "每周 10 小时以上";
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
            default -> "目标导向学习";
        };
    }

    public String capabilityLevel(String code) {
        return switch (normalize(code)) {
            case "BASIC", "BEGINNER", "WEAK" -> "学过相关内容，但基础还不稳定";
            case "INTERMEDIATE", "PARTIAL" -> "有一定基础，正在向稳定应用过渡";
            case "ADVANCED", "PROFICIENT", "STABLE" -> "基础较好，可开始更高阶训练";
            default -> "基础状态待进一步确认";
        };
    }

    public String foundationLevel(String code) {
        return switch (normalize(code)) {
            case "NONE", "BASIC", "BEGINNER", "WEAK" -> "学过相关内容，但基础还不稳定";
            case "INTERMEDIATE", "PARTIAL", "COURSEWORK" -> "有一定基础，仍需针对性巩固";
            case "ADVANCED", "PROFICIENT", "STABLE" -> "基础较好，可进入强化阶段";
            default -> "基础状态待进一步确认";
        };
    }

    private String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}
