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
              "title":"连接建立机制结构图",
              "summary":"先确认连接双方状态和前置条件，再按步骤发送控制报文并校验确认号，最终完成连接建立。",
              "key_points":["说明三次握手设计目的","梳理状态转换前置条件","解释确认号在可靠性中的作用"],
              "common_misconceptions":["认为两次握手已经足够安全","忽略 ACK 对状态确认的意义"],
              "suggested_sequence":["先明确核心定义与边界", "再推导完整流程机制", "最后分析异常与边界场景"]
            }
            """;
        assertTrue(validator.validateStage(Stage.STRUCTURE, mapper.readTree(json)).isEmpty());
    }

    @Test
    void shouldRejectUnexpectedStageField() throws Exception {
        String json = """
            {
              "title":"x",
              "summary":"这是一段明显不满足长度要求的文本",
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
                  "objective": "补齐关键步骤推导，完成一次错误对照解释。",
                  "trigger": "MISSING_STEPS"
                }
              ],
              "plan_reasoning_summary": "Prioritize weak foundational node and add one remedial understanding task.",
              "risk_flags": ["LOW_CONFIDENCE_DIAGNOSIS"]
            }
            """;
        assertTrue(validator.validatePersonalizedPathPlan(mapper.readTree(json)).isEmpty());
    }

    @Test
    void shouldAllowStageTextAndLeaveStageValidationToBusinessValidator() throws Exception {
        String json = """
            {
              "ordered_nodes": [
                {"node_id": 101, "priority": 1, "reason": "Start from weak concept."}
              ],
              "inserted_tasks": [
                {
                  "node_id": 101,
                  "stage": "REFLECTION",
                  "objective": "Provide additional reflective notes for this node.",
                  "trigger": "CONCEPT_CONFUSION"
                }
              ],
              "plan_reasoning_summary": "Prioritize weak node and attach one remediation task for confusion diagnosis.",
              "risk_flags": []
            }
            """;
        assertTrue(validator.validatePersonalizedPathPlan(mapper.readTree(json)).isEmpty());
    }
}
