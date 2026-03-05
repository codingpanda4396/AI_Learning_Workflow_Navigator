package com.panda.ainavigator.api.dto.session;

import com.panda.ainavigator.domain.model.Stage;
import com.panda.ainavigator.domain.model.TaskStatus;

public record PlannedTaskResponse(
        Long task_id,
        Stage stage,
        Long node_id,
        String objective,
        TaskStatus status
) {
}
