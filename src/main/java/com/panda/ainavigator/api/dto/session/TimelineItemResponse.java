package com.panda.ainavigator.api.dto.session;

public record TimelineItemResponse(
        Long task_id,
        com.panda.ainavigator.domain.model.Stage stage,
        Long node_id,
        com.panda.ainavigator.domain.model.TaskStatus status
) {
}
