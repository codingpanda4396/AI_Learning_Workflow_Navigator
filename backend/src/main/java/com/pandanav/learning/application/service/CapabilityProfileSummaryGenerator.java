package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.enums.CapabilityLevel;
import com.pandanav.learning.domain.model.CapabilityProfileDraft;
import com.pandanav.learning.domain.model.CapabilityProfileSummaryCopy;
import org.springframework.stereotype.Component;

@Component
public class CapabilityProfileSummaryGenerator {

    public CapabilityProfileSummaryCopy buildFallback(CapabilityProfileDraft draft) {
        String level = switch (draft.currentLevel()) {
            case BEGINNER -> "更适合从基础起步";
            case INTERMEDIATE -> "已经有一定基础，可以边巩固边推进";
            case ADVANCED -> "基础相对扎实，可以更快进入综合应用";
        };
        String summary = "从这轮诊断来看，你目前%s，后续学习会尽量贴合你的当前基础和节奏。".formatted(level);
        String planExplanation = "系统接下来会结合你的基础、目标和学习习惯安排后续内容，并逐步调整训练难度。";
        return new CapabilityProfileSummaryCopy(summary, planExplanation);
    }
}
