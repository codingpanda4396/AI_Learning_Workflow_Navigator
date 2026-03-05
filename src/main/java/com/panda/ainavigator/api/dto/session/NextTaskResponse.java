package com.panda.ainavigator.api.dto.session;

import com.panda.ainavigator.domain.model.Stage;

public record NextTaskResponse(
        Long task_id,
        Stage stage,
        Long node_id
) {
}
