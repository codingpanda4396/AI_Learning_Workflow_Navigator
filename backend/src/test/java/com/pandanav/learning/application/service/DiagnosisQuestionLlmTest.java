package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionOption;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiagnosisQuestionLlmTest {

    private final LlmJsonParser parser = new LlmJsonParser(new ObjectMapper());
    private final QuestionStructureAssembler assembler = new QuestionStructureAssembler();

    @Test
    void shouldParseNormalJson() {
        JsonNode parsed = parser.parse("""
            {
              "questions": [
                {"questionId":"q_foundation","title":"基础如何","description":"选择最接近你的情况","submitHint":"用于判断起点","sectionLabel":"基础掌握"},
                {"questionId":"q_experience","title":"经验情况","description":"你之前接触过多少","submitHint":"用于估算学习曲线","sectionLabel":"过往经验"},
                {"questionId":"q_goal_style","title":"目标导向","description":"本次更偏向什么目标","submitHint":"用于推荐任务类型","sectionLabel":"目标导向"},
                {"questionId":"q_time_budget","title":"时间预算","description":"每周可投入时间","submitHint":"用于安排节奏","sectionLabel":"时间预算"},
                {"questionId":"q_learning_preference","title":"学习偏好","description":"你更偏好的方式","submitHint":"用于匹配学习形式","sectionLabel":"学习偏好"}
              ]
            }
            """);

        assertEquals(5, parsed.path("questions").size());
        assertEquals("q_foundation", parsed.path("questions").get(0).path("questionId").asText());
    }

    @Test
    void shouldRepairTruncatedJson() {
        JsonNode parsed = parser.parse("""
            {
              "questions": [
                {"questionId":"q_foundation","title":"基础如何","description":"选择最接近你的情况","submitHint":"用于判断起点","sectionLabel":"基础掌握"},
                {"questionId":"q_experience","title":"经验情况","description":"你之前接触过多少","submitHint":"用于估算学习曲线","sectionLabel":"过往经验"},
                {"questionId":"q_goal_style","title":"目标导向","description":"本次更偏向什么目标","submitHint":"用于推荐任务类型","sectionLabel":"目标
            """);

        assertEquals("q_foundation", parsed.path("questions").get(0).path("questionId").asText());
        assertEquals("q_experience", parsed.path("questions").get(1).path("questionId").asText());
    }

    @Test
    void shouldRepairJsonWithExtraText() {
        JsonNode parsed = parser.parse("这里是结果：" +
            "{\"questions\":[{\"questionId\":\"q_foundation\",\"title\":\"基础如何\",\"description\":\"选择最接近你的情况\",\"submitHint\":\"用于判断起点\",\"sectionLabel\":\"基础掌握\"}]}" +
            " 以上。");

        assertEquals(1, parsed.path("questions").size());
        assertEquals("q_foundation", parsed.path("questions").get(0).path("questionId").asText());
    }

    @Test
    void shouldFillMissingFieldsFromRuleStructure() {
        List<DiagnosisQuestion> sourceQuestions = List.of(
            sourceQuestion("q_foundation", DiagnosisDimension.FOUNDATION),
            sourceQuestion("q_experience", DiagnosisDimension.EXPERIENCE),
            sourceQuestion("q_goal_style", DiagnosisDimension.GOAL_STYLE),
            sourceQuestion("q_time_budget", DiagnosisDimension.TIME_BUDGET),
            sourceQuestion("q_learning_preference", DiagnosisDimension.LEARNING_PREFERENCE)
        );

        JsonNode parsed = parser.parse("""
            {
              "questions": [
                {"questionId":"q_foundation","title":"基础如何","description":"选择最接近你的情况","sectionLabel":"基础掌握"},
                {"questionId":"q_experience","title":"经验情况","submitHint":"用于估算学习曲线","sectionLabel":"过往经验"},
                {"questionId":"q_goal_style","title":"目标导向","description":"本次更偏向什么目标","submitHint":"用于推荐任务类型","sectionLabel":"目标导向"},
                {"questionId":"q_time_budget","title":"时间预算","description":"每周可投入时间","submitHint":"用于安排节奏","sectionLabel":"时间预算"},
                {"questionId":"q_learning_preference","title":"学习偏好","description":"你更偏好的方式","submitHint":"用于匹配学习形式","sectionLabel":"学习偏好"}
              ]
            }
            """);

        List<DiagnosisQuestion> assembled = assembler.assemble(sourceQuestions, parsed.path("questions"));
        assertEquals(5, assembled.size());
        assertEquals("默认提示-q_foundation", assembled.get(0).submitHint());
        assertEquals("默认描述-q_experience", assembled.get(1).description());
        assertEquals("默认占位-q_time_budget", assembled.get(3).placeholder());
        assertEquals(1, assembled.get(4).options().get(0).order());
    }

    private DiagnosisQuestion sourceQuestion(String questionId, DiagnosisDimension dimension) {
        return new DiagnosisQuestion(
            questionId,
            dimension,
            "single_choice",
            true,
            List.of(new DiagnosisQuestionOption("OPT_A", "A", 1), new DiagnosisQuestionOption("OPT_B", "B", 2)),
            "默认标题-" + questionId,
            "默认描述-" + questionId,
            "默认占位-" + questionId,
            "默认提示-" + questionId,
            "默认分组-" + questionId
        );
    }
}
