package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiagnosisTemplateFactory {

    public List<DiagnosisQuestion> buildQuestions(String goalText) {
        String goalHint = goalText == null || goalText.isBlank() ? "这次学习目标" : goalText.trim();
        return List.of(
            new DiagnosisQuestion(
                "q_foundation",
                DiagnosisDimension.FOUNDATION,
                "single_choice",
                "你目前的基础更接近哪种情况？",
                "结合“%s”，选一个最贴近你当前状态的描述。".formatted(goalHint),
                List.of("刚开始接触，需要从基础概念学起", "了解过核心概念，但做题或应用还不稳定", "已经比较熟悉，希望更快进入综合应用"),
                true
            ),
            new DiagnosisQuestion(
                "q_experience",
                DiagnosisDimension.EXPERIENCE,
                "multiple_choice",
                "你以前有过哪些相关学习或实践经历？",
                "可多选，我们会据此判断你更适合从讲解入手还是从训练入手。",
                List.of("上过相关课程", "做过课程作业或实验", "做过项目或作品", "准备过考试或面试", "几乎没有相关经验"),
                true
            ),
            new DiagnosisQuestion(
                "q_goal_style",
                DiagnosisDimension.GOAL_STYLE,
                "single_choice",
                "这次学习你最想解决哪类目标？",
                "选择最主要的一项，系统会据此调整后续学习路径的侧重点。",
                List.of("应对课程学习与作业", "准备考试或测验", "准备实习或求职面试", "完成项目或作品"),
                true
            ),
            new DiagnosisQuestion(
                "q_time_budget",
                DiagnosisDimension.TIME_BUDGET,
                "single_choice",
                "你每周大概能投入多少时间？",
                "不用特别精确，按通常情况选择即可。",
                List.of("每周 1-3 小时", "每周 4-6 小时", "每周 7-10 小时", "每周 10 小时以上"),
                true
            ),
            new DiagnosisQuestion(
                "q_learning_preference",
                DiagnosisDimension.LEARNING_PREFERENCE,
                "single_choice",
                "你更喜欢哪种学习方式？",
                "选择你最容易坚持、也最容易进入状态的一种。",
                List.of("先讲清概念，再做练习", "先看例子，再总结方法", "先做题，在反馈中查漏补缺", "边学边做项目，穿插补基础"),
                true
            )
        );
    }
}
