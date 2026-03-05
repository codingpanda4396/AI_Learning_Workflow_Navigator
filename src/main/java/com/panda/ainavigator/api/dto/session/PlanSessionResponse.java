package com.panda.ainavigator.api.dto.session;

import java.util.List;

public record PlanSessionResponse(
        Long session_id,
        List<PlannedTaskResponse> tasks
) {
}
