package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestionCopy;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Component;

@Component
public class DiagnosisQuestionCopyFactory {

    public DiagnosisQuestionCopy build(LearningSession session, DiagnosisDimension dimension, String type) {
        return switch (dimension) {
            case FOUNDATION -> new DiagnosisQuestionCopy(
                "KNOWLEDGE_FOUNDATION",
                "你觉得自己目前对这部分内容的掌握程度如何？",
                "按你现在的真实情况作答即可，这不是考试。",
                "",
                "你的回答会帮助系统判断起点和后续安排。"
            );
            case EXPERIENCE -> new DiagnosisQuestionCopy(
                "PAST_EXPERIENCE",
                "你之前接触过哪些相关学习或实践？",
                "可多选，按真实经历选择就可以。",
                "",
                "这些经历会帮助系统判断更适合的切入方式。"
            );
            case GOAL_STYLE -> new DiagnosisQuestionCopy(
                "GOAL_ORIENTATION",
                "这次学习你最想优先解决什么目标？",
                "选最主要的一项，方便系统安排后续重点。",
                "",
                "系统会根据你的目标调整后续路径侧重点。"
            );
            case TIME_BUDGET -> new DiagnosisQuestionCopy(
                "TIME_BUDGET",
                "你现在大概能为这个目标投入多少学习时间？",
                "不用理想化，按你现实能稳定投入的节奏来选。",
                "",
                "系统会尽量把安排控制在你能坚持的节奏里。"
            );
            case LEARNING_PREFERENCE -> new DiagnosisQuestionCopy(
                "LEARNING_PREFERENCE",
                "说说你平时更适合什么样的学习方式。",
                "比如先看总结、再做题，或者边学边练。",
                "你可以简单描述自己更容易进入状态的方式。",
                "这会影响系统讲解和练习的组织方式。"
            );
            case DIFFICULTY_PAIN_POINT -> new DiagnosisQuestionCopy(
                "DIFFICULTY_PAIN_POINT",
                "你目前最容易卡住的是哪一类问题？",
                "请选择最符合的一项。",
                "",
                "这会帮助系统识别你最需要突破的难点。"
            );
        };
    }
}
