package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiagnosisTemplateFactory {

    private final DiagnosisQuestionCopyFactory diagnosisQuestionCopyFactory;

    public DiagnosisTemplateFactory(DiagnosisQuestionCopyFactory diagnosisQuestionCopyFactory) {
        this.diagnosisQuestionCopyFactory = diagnosisQuestionCopyFactory;
    }

    public List<DiagnosisQuestion> buildQuestions(LearningSession session) {
        return List.of(
            new DiagnosisQuestion(
                "q_foundation",
                DiagnosisDimension.FOUNDATION,
                "single_choice",
                "你觉得自己目前对这部分内容的掌握程度如何？",
                "按你现在的真实情况作答即可，这不是考试。",
                List.of("刚开始接触", "学过但还不太稳", "基础比较稳", "已经能独立应用"),
                true,
                diagnosisQuestionCopyFactory.build(session, DiagnosisDimension.FOUNDATION, "single_choice")
            ),
            new DiagnosisQuestion(
                "q_experience",
                DiagnosisDimension.EXPERIENCE,
                "multiple_choice",
                "你之前有过哪些相关学习或实践经历？",
                "可多选，我们会据此判断更适合从讲解入手还是从训练入手。",
                List.of("上过相关课程", "做过作业或实验", "做过项目或作品", "准备过考试或面试", "几乎没有相关经验"),
                true,
                diagnosisQuestionCopyFactory.build(session, DiagnosisDimension.EXPERIENCE, "multiple_choice")
            ),
            new DiagnosisQuestion(
                "q_goal_style",
                DiagnosisDimension.GOAL_STYLE,
                "single_choice",
                "这次学习你最想优先解决哪类目标？",
                "选最主要的一项，系统会据此调整后续路径的侧重点。",
                List.of("应对课程学习与作业", "准备考试或测验", "准备实习或求职面试", "完成项目或作品"),
                true,
                diagnosisQuestionCopyFactory.build(session, DiagnosisDimension.GOAL_STYLE, "single_choice")
            ),
            new DiagnosisQuestion(
                "q_time_budget",
                DiagnosisDimension.TIME_BUDGET,
                "single_choice",
                "你每周大概能为这个目标投入多少时间？",
                "不用特别精确，按你现实中能稳定执行的节奏选择即可。",
                List.of("每周 1-3 小时", "每周 4-6 小时", "每周 7-10 小时", "每周 10 小时以上"),
                true,
                diagnosisQuestionCopyFactory.build(session, DiagnosisDimension.TIME_BUDGET, "single_choice")
            ),
            new DiagnosisQuestion(
                "q_learning_preference",
                DiagnosisDimension.LEARNING_PREFERENCE,
                "single_choice",
                "你平时更适合哪种学习方式？",
                "选你最容易坚持、也最容易进入状态的一种。",
                List.of("先讲清概念，再做练习", "先看例子，再总结方法", "先做题，在反馈中查漏补缺", "边学边做项目，穿插补基础"),
                true,
                diagnosisQuestionCopyFactory.build(session, DiagnosisDimension.LEARNING_PREFERENCE, "single_choice")
            )
        );
    }
}
