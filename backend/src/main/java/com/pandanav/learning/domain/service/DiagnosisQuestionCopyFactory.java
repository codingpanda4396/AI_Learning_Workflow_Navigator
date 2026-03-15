package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestionCopy;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Component;

@Component
public class DiagnosisQuestionCopyFactory {

    public DiagnosisQuestionCopy build(LearningSession session, DiagnosisDimension dimension, String type) {
        return build(session, dimension, type, null);
    }

    public DiagnosisQuestionCopy build(LearningSession session, DiagnosisDimension dimension, String type, String questionId) {
        if (questionId != null && !questionId.isBlank()) {
            return switch (questionId) {
                case "q_topic_focus" -> new DiagnosisQuestionCopy(
                    "TOPIC_FOCUS",
                    "你对「这部分内容」哪一块最模糊？",
                    "选最不确定的部分，方便系统从那里切入。",
                    "",
                    "用于定位你的起点盲区。"
                );
                case "q_topic_depth" -> new DiagnosisQuestionCopy(
                    "TOPIC_DEPTH",
                    "你对自己在「这部分内容」上的理解程度怎么估？",
                    "比如概念、原理、能否动手实现。",
                    "",
                    "用于判断是否需要补基础。"
                );
                case "q_goal_type" -> new DiagnosisQuestionCopy(
                    "GOAL_TYPE",
                    "学「这部分内容」你主要是为了？",
                    "考试/面试/工程理解，选最贴近的。",
                    "",
                    "目标不同，路径会不一样。"
                );
                case "q_skip_foundation" -> new DiagnosisQuestionCopy(
                    "SKIP_FOUNDATION",
                    "你是否希望跳过已掌握的基础直接上难度？",
                    "按真实想法选即可。",
                    "",
                    "用于判断是否做快速定档。"
                );
                case "q_time_constrained" -> new DiagnosisQuestionCopy(
                    "TIME_CONSTRAINED",
                    "你这次能稳定投入的时间大概怎样？",
                    "时间紧的话系统会精简题量、快速定档。",
                    "",
                    "用于控制诊断题量与节奏。"
                );
                case "q_interview_oriented" -> new DiagnosisQuestionCopy(
                    "INTERVIEW_ORIENTED",
                    "这次学习是否以面试/考核为导向？",
                    "面试导向会侧重考点与表达。",
                    "",
                    "用于调整后续路径侧重点。"
                );
                default -> buildByDimension(dimension);
            };
        }
        return buildByDimension(dimension);
    }

    private DiagnosisQuestionCopy buildByDimension(DiagnosisDimension dimension) {
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
