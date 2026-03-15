package com.pandanav.learning.application.service.diagnosis;

import com.pandanav.learning.api.contract.ContractCatalog;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionOption;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 固定 8 题：5 个通用骨架题 + 3 个主题增强题。不调用 LLM，适合首屏快速加载。
 * 主题题围绕 goalText/topic 做自然表述，通用题短、口语化。
 */
@Component
public class StructuredDiagnosisQuestionFactory {

    static final String Q_FOUNDATION = "q_foundation";
    static final String Q_BLOCKER = "q_blocker";
    static final String Q_PREFERENCE = "q_preference";
    static final String Q_GOAL = "q_goal";
    static final String Q_TIME_BUDGET = "q_time_budget";
    static final String Q_TOPIC_CORE = "q_topic_core";
    static final String Q_TOPIC_OPERATION = "q_topic_operation";
    static final String Q_TOPIC_FOCUS = "q_topic_focus";

    /** 兼容提交阶段解析：若历史数据含 q_practice 仍可读，推导时按空处理。 */
    static final String Q_PRACTICE = "q_practice";

    private final TopicQuestionBank topicQuestionBank;

    public StructuredDiagnosisQuestionFactory(TopicQuestionBank topicQuestionBank) {
        this.topicQuestionBank = topicQuestionBank;
    }

    /**
     * 8 题：5 通用（基础/卡点/偏好/目标/时间）+ 3 主题（概念/应用卡点/学习侧重）。
     */
    public List<DiagnosisQuestion> build(String goalText, String topicTitle) {
        String topic = topicTitle == null ? "当前主题" : topicTitle.trim();
        String topicCoreTitle = topicQuestionBank.topicCoreTitle(goalText, topic);
        String topicOperationTitle = topicQuestionBank.topicOperationTitle(goalText, topic);
        String topicFocusTitle = topicQuestionBank.topicFocusTitle(goalText, topic);
        List<DiagnosisQuestionOption> foundationOpts = ContractCatalog.diagnosisQuestionOptions(DiagnosisDimension.FOUNDATION);
        List<DiagnosisQuestionOption> blockerOpts = ContractCatalog.diagnosisQuestionOptions(DiagnosisDimension.BLOCKER);
        List<DiagnosisQuestionOption> preferenceOpts = ContractCatalog.diagnosisQuestionOptions(DiagnosisDimension.PREFERENCE);
        List<DiagnosisQuestionOption> goalOpts = ContractCatalog.diagnosisQuestionOptions(DiagnosisDimension.GOAL);
        List<DiagnosisQuestionOption> timeBudgetOpts = List.of(
            new DiagnosisQuestionOption("SHORT_10", "10 分钟左右", 1),
            new DiagnosisQuestionOption("MEDIUM_30", "20~30 分钟", 2),
            new DiagnosisQuestionOption("LONG_60", "40~60 分钟", 3),
            new DiagnosisQuestionOption("SYSTEMATIC", "可以系统学一轮", 4)
        );
        List<DiagnosisQuestionOption> topicCoreOpts = ContractCatalog.diagnosisQuestionOptions(DiagnosisDimension.TOPIC_CORE);
        List<DiagnosisQuestionOption> topicOperationOpts = ContractCatalog.diagnosisQuestionOptions(DiagnosisDimension.TOPIC_OPERATION);
        List<DiagnosisQuestionOption> topicFocusOpts = ContractCatalog.diagnosisQuestionOptions(DiagnosisDimension.TOPIC_FOCUS);

        return List.of(
            new DiagnosisQuestion(
                Q_FOUNDATION,
                DiagnosisDimension.FOUNDATION,
                "SINGLE_CHOICE",
                true,
                foundationOpts,
                "你目前对这个主题的掌握更接近？",
                null, null, null, "基础",
                List.of(), Map.of()
            ),
            new DiagnosisQuestion(
                Q_BLOCKER,
                DiagnosisDimension.BLOCKER,
                "SINGLE_CHOICE",
                true,
                blockerOpts,
                "你最大的卡点更接近哪一种？",
                null, null, null, "卡点",
                List.of(), Map.of()
            ),
            new DiagnosisQuestion(
                Q_PREFERENCE,
                DiagnosisDimension.PREFERENCE,
                "SINGLE_CHOICE",
                true,
                preferenceOpts,
                "你更适合从哪种方式开始？",
                null, null, null, "偏好",
                List.of(), Map.of()
            ),
            new DiagnosisQuestion(
                Q_GOAL,
                DiagnosisDimension.GOAL,
                "SINGLE_CHOICE",
                true,
                goalOpts,
                "这次你更想优先解决什么？",
                null, null, null, "目标",
                List.of(), Map.of()
            ),
            new DiagnosisQuestion(
                Q_TIME_BUDGET,
                DiagnosisDimension.TIME_BUDGET,
                "SINGLE_CHOICE",
                true,
                timeBudgetOpts,
                "这轮你打算先投入多少时间？",
                null, null, null, "时间",
                List.of(), Map.of()
            ),
            new DiagnosisQuestion(
                Q_TOPIC_CORE,
                DiagnosisDimension.TOPIC_CORE,
                "SINGLE_CHOICE",
                true,
                topicCoreOpts,
                topicCoreTitle,
                null, null, null, "概念",
                List.of(), Map.of()
            ),
            new DiagnosisQuestion(
                Q_TOPIC_OPERATION,
                DiagnosisDimension.TOPIC_OPERATION,
                "SINGLE_CHOICE",
                true,
                topicOperationOpts,
                topicOperationTitle,
                null, null, null, "应用",
                List.of(), Map.of()
            ),
            new DiagnosisQuestion(
                Q_TOPIC_FOCUS,
                DiagnosisDimension.TOPIC_FOCUS,
                "SINGLE_CHOICE",
                true,
                topicFocusOpts,
                topicFocusTitle,
                null, null, null, "侧重",
                List.of(), Map.of()
            )
        );
    }
}
