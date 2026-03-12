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
            strengths.add("有一定动手实践经验，适合结合案例继续推进。");
        }
        if (experience.contains("准备过考试或面试")) {
            strengths.add("目标感比较明确，适合按阶段设置训练重点。");
        }
        if (currentLevel == CapabilityLevel.ADVANCED) {
            strengths.add("基础相对扎实，可以更快进入综合应用。");
        }
        if (strengths.isEmpty()) {
            strengths.add("目标比较明确，便于快速安排个性化学习内容。");
        }

        if (currentLevel == CapabilityLevel.BEGINNER) {
            weaknesses.add("当前基础还不够稳定，需要先补齐关键概念。");
        }
        if (experience.contains("几乎没有相关经验")) {
            weaknesses.add("相关经验较少，开始阶段更需要例子和分步练习。");
        }
        if ("每周 1-3 小时".equals(timeBudget)) {
            weaknesses.add("可投入时间有限，学习内容需要更聚焦。");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("还需要通过后续训练进一步确认薄弱点。");
        }

        String summary = "系统已根据你的回答整理出当前能力画像，后续学习会尽量贴合你的当前基础和节奏。";
        return new CapabilityProfileDraft(currentLevel, strengths, weaknesses, learningPreference, timeBudget, goalOrientation, summary);
    }

    private CapabilityLevel mapCurrentLevel(String foundation, List<String> experience) {
        if ("已经能独立应用".equals(foundation) || experience.contains("做过项目或作品")) {
            return CapabilityLevel.ADVANCED;
        }
        if ("学过但还不太稳".equals(foundation)
            || "基础比较稳".equals(foundation)
            || experience.contains("上过相关课程")
            || experience.contains("做过作业或实验")) {
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

    private String firstAnswer(Map<DiagnosisDimension, List<String>> answersByDimension, DiagnosisDimension dimension) {
        List<String> answers = answersByDimension.get(dimension);
        if (answers == null || answers.isEmpty()) {
            return "";
        }
        return answers.get(0);
    }
}
