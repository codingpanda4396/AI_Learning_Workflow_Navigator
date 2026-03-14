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

        if (experience.contains("PROJECTS")) {
            strengths.add("有一定动手实践经验，适合结合案例继续推进。");
        }
        if (experience.contains("EXAM_PREP")) {
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
        if (experience.contains("NO_EXPERIENCE")) {
            weaknesses.add("相关经验较少，起步阶段更需要例子和分步练习。");
        }
        if ("LIGHT".equals(timeBudget)) {
            weaknesses.add("可投入时间有限，学习内容需要更聚焦。");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("还需要通过后续训练进一步确认薄弱点。");
        }

        return new CapabilityProfileDraft(currentLevel, strengths, weaknesses, learningPreference, timeBudget, goalOrientation, "系统已根据你的回答整理出当前能力画像。");
    }

    private CapabilityLevel mapCurrentLevel(String foundation, List<String> experience) {
        if ("ADVANCED".equals(foundation) || experience.contains("PROJECTS")) {
            return CapabilityLevel.ADVANCED;
        }
        if ("BASIC".equals(foundation)
            || "PROFICIENT".equals(foundation)
            || experience.contains("COURSEWORK")
            || experience.contains("ASSIGNMENTS")) {
            return CapabilityLevel.INTERMEDIATE;
        }
        return CapabilityLevel.BEGINNER;
    }

    private String mapGoalOrientation(String goalStyle) {
        if ("EXAM".equals(goalStyle)) {
            return "EXAM";
        }
        if ("INTERVIEW".equals(goalStyle)) {
            return "INTERVIEW";
        }
        if ("PROJECT".equals(goalStyle)) {
            return "PROJECT";
        }
        return "COURSE";
    }

    private String mapLearningPreference(String preference) {
        if ("EXAMPLE_FIRST".equals(preference)) {
            return "EXAMPLE_FIRST";
        }
        if ("PRACTICE_FIRST".equals(preference)) {
            return "PRACTICE_FIRST";
        }
        if ("PROJECT_DRIVEN".equals(preference)) {
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
