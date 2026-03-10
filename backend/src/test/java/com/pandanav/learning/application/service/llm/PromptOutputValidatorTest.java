package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.enums.Stage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptOutputValidatorTest {

    private final PromptOutputValidator validator = new PromptOutputValidator();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldPassStructureSchemaValidation() throws Exception {
        String json = """
            {
              "title":"TCP结构",
              "summary":"建立 TCP 的核心结构认识",
              "key_points":["定义边界","核心关系"],
              "common_misconceptions":["只记结论"],
              "suggested_sequence":["先定义","再分层","做总结"]
            }
            """;
        assertTrue(validator.validateStage(Stage.STRUCTURE, mapper.readTree(json)).isEmpty());
    }

    @Test
    void shouldTrimOverlongStructureOutput() throws Exception {
        String json = """
            {
              "title":"这是一个明显超过二十字的结构标题用于测试裁剪",
              "summary":"这是一个明显超过五十字的总结内容用于测试裁剪逻辑是否生效并且仍然保持合法 JSON 输出",
              "key_points":["第一条很长很长很长","第二条很长很长很长","第三条","第四条","第五条"],
              "common_misconceptions":["误区一很长很长很长","误区二","误区三"],
              "suggested_sequence":["第一步很长","第二步","第三步","第四步","第五步"]
            }
            """;
        var sanitized = validator.sanitizeStage(Stage.STRUCTURE, mapper.readTree(json));
        assertTrue(sanitized.truncated());
        assertTrue(sanitized.errors().isEmpty());
    }

    @Test
    void shouldRejectUnexpectedStageField() throws Exception {
        String json = """
            {
              "title":"x",
              "summary":"这是一段不合法文本",
              "key_points":[],
              "common_misconceptions":[],
              "suggested_sequence":[],
              "extra":"bad"
            }
            """;
        var errors = validator.validateStage(Stage.STRUCTURE, mapper.readTree(json));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(v -> v.contains("unexpected field")));
    }

    @Test
    void shouldValidatePersonalizedPathPlanSchema() throws Exception {
        String json = """
            {
              "ordered_nodes": [
                {"node_id": 101, "priority": 1, "reason": "Start from weakest prerequisite first."},
                {"node_id": 102, "priority": 2, "reason": "Then consolidate concept variants."}
              ],
              "inserted_tasks": [
                {
                  "node_id": 101,
                  "stage": "UNDERSTANDING",
                  "objective": "Fill the missing prerequisite reasoning first.",
                  "trigger": "MISSING_STEPS"
                }
              ],
              "plan_reasoning_summary": "Prioritize the weak foundational node and insert one short remedial task.",
              "risk_flags": ["LOW_CONFIDENCE_DIAGNOSIS"]
            }
            """;
        assertTrue(validator.validatePersonalizedPathPlan(mapper.readTree(json)).isEmpty());
    }
}
