package com.panda.ainavigator.api.dto.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.panda.ainavigator.domain.model.Stage;
import com.panda.ainavigator.domain.model.TaskStatus;

public record RunTaskResponse(
        Long task_id,
        Stage stage,
        Long node_id,
        TaskStatus status,
        JsonNode output
) {
}
