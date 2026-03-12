package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.CapabilityLevel;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.CapabilityProfileDraft;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CapabilityProfileBuilder {

    public CapabilityProfileDraft build(Map<DiagnosisDimension, List<String>> answersByDimension) {
        String foundation = firstAnswer(answersByDimension, DiagnosisDimension.FOUNDATION);
        String goalStyle = firstAnswer(answersByDimension, DiagnosisDimension.GOAL_STYLE);
        String timeBudget = firstAnswer(answersByDimension, DiagnosisDimension.TIME_BUDGET);
        String preference = firstAnswer(answersByDimension, DiagnosisDimension.LEARNING_PREFERENCE);
        List<String> experience = answersByDimension.getOrDefault(DiagnosisDimension.EXPERIENCE, List.of());

        CapabilityLevel currentLevel = mapCurrentLevel(foundation, experience);
        String goalOrientation = mapGoalOrientation(goalStyle);
        String learningPreference = mapLearningPreference(preference);

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        if (experience.contains("做过项目或作品")) {
            strengths.add("有一定动手实践经验，适合结合案例继续推进");
        }
        if (experience.contains("准备过考试或面试")) {
            strengths.add("目标意识较明确，适合按阶段设定训练节点");
        }
        if (currentLevel == CapabilityLevel.ADVANCED) {
            strengths.add("基础较扎实，可以更快进入综合应用");
        }
        if (strengths.isEmpty()) {
            strengths.add("学习目标明确，便于快速制定个性化安排");
        }

        if (currentLevel == CapabilityLevel.BEGINNER) {
            weaknesses.add("当前基础还不稳定，需要先补齐关键概念");
        }
        if (experience.contains("几乎没有相关经验")) {
            weaknesses.add("相关经验较少，刚开始更需要例子和分步练习");
        }
        if ("每周 1-3 小时".equals(timeBudget)) {
            weaknesses.add("可投入时间有限，学习节奏需要更聚焦");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("需要通过几轮训练进一步确认薄弱点");
        }

        String summary = buildFallbackSummary(currentLevel, strengths, weaknesses, learningPreference, timeBudget, goalOrientation);
        return new CapabilityProfileDraft(currentLevel, strengths, weaknesses, learningPreference, timeBudget, goalOrientation, summary);
    }

    private CapabilityLevel mapCurrentLevel(String foundation, List<String> experience) {
        if ("已经比较熟悉，希望更快进入综合应用".equals(foundation)
            || experience.contains("做过项目或作品")) {
            return CapabilityLevel.ADVANCED;
        }
        if ("了解过核心概念，但做题或应用还不稳定".equals(foundation)
            || experience.contains("上过相关课程")
            || experience.contains("做过课程作业或实验")) {
            return CapabilityLevel.INTERMEDIATE;
        }
        return CapabilityLevel.BEGINNER;
    }

    private String mapGoalOrientation(String goalStyle) {
        if ("准备考试或测验".equals(goalStyle)) {
            return "EXAM";
        }
        if ("准备实习或求职面试".equals(goalStyle)) {
            return "INTERVIEW";
        }
        if ("完成项目或作品".equals(goalStyle)) {
            return "PROJECT";
        }
        return "COURSE";
    }

    private String mapLearningPreference(String preference) {
        if ("先看例子，再总结方法".equals(preference)) {
            return "EXAMPLE_FIRST";
        }
        if ("先做题，在反馈中查漏补缺".equals(preference)) {
            return "PRACTICE_FIRST";
        }
        if ("边学边做项目，穿插补基础".equals(preference)) {
            return "PROJECT_DRIVEN";
        }
        return "CONCEPT_FIRST";
    }

    private String buildFallbackSummary(
        CapabilityLevel currentLevel,
        List<String> strengths,
        List<String> weaknesses,
        String learningPreference,
        String timeBudget,
        String goalOrientation
    ) {
        return "你目前处于%s阶段，当前优势是%s；现阶段更需要关注%s。后续建议按照%s导向推进，每周按%s安排学习，更适合%s的节奏。"
            .formatted(
                levelText(currentLevel),
                strengths.get(0),
                weaknesses.get(0),
                goalOrientationText(goalOrientation),
                timeBudget == null || timeBudget.isBlank() ? "可投入时间" : timeBudget,
                preferenceText(learningPreference)
            );
    }

    private String levelText(CapabilityLevel level) {
        return switch (level) {
            case BEGINNER -> "入门";
            case INTERMEDIATE -> "进阶前期";
            case ADVANCED -> "进阶后期";
        };
    }

    private String goalOrientationText(String value) {
        return switch (value) {
            case "EXAM" -> "考试";
            case "INTERVIEW" -> "面试";
            case "PROJECT" -> "项目";
            default -> "课程";
        };
    }

    private String preferenceText(String value) {
        return switch (value) {
            case "EXAMPLE_FIRST" -> "先例子后总结";
            case "PRACTICE_FIRST" -> "训练优先";
            case "PROJECT_DRIVEN" -> "项目驱动";
            default -> "先理解再练习";
        };
    }

    private String firstAnswer(Map<DiagnosisDimension, List<String>> answersByDimension, DiagnosisDimension dimension) {
        List<String> answers = answersByDimension.get(dimension);
        if (answers == null || answers.isEmpty()) {
            return "";
        }
        return answers.get(0);
    }
}
