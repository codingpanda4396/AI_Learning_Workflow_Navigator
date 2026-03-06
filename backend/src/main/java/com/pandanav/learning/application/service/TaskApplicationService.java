package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.session.NextTaskResponse;
import com.pandanav.learning.api.dto.task.FeedbackResponse;
import com.pandanav.learning.api.dto.task.RunTaskResponse;
import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class TaskApplicationService {

    private final ObjectMapper objectMapper;

    public TaskApplicationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RunTaskResponse runTask(Long taskId) {
        JsonNode output = objectMapper.valueToTree(
            Map.of(
                "sections", List.of(
                    Map.of("type", "concepts", "title", "核心概念", "bullets", List.of("TCP 连接是逻辑连接", "SYN/ACK 用于建立确认关系")),
                    Map.of("type", "summary", "title", "一分钟总结", "text", "三次握手本质是双方初始序列号确认与状态一致性建立。")
                )
            )
        );
        return new RunTaskResponse(taskId, "UNDERSTANDING", 101L, "SUCCEEDED", output);
    }

    public SubmitTaskResponse submitTask(Long taskId, SubmitTaskRequest request) {
        return new SubmitTaskResponse(
            taskId,
            "TRAINING",
            101L,
            72,
            List.of("CONCEPT_CONFUSION", "MISSING_STEPS"),
            new FeedbackResponse(
                "对三次握手必要性的解释不完整，未覆盖旧 SYN 重放场景。",
                List.of("补充旧 SYN 重放导致半开连接的反例", "按步骤描述 ISN(c)/ISN(s) 的确认关系")
            ),
            new BigDecimal("0.55"),
            new BigDecimal("0.05"),
            new BigDecimal("0.60"),
            "INSERT_TRAINING_VARIANTS",
            new NextTaskResponse(2001L, "TRAINING", 101L)
        );
    }
}
