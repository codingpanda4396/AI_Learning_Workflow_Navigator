package com.panda.ainavigator.api.dto.task;

import com.panda.ainavigator.domain.model.ErrorTag;
import com.panda.ainavigator.domain.model.NextAction;
import com.panda.ainavigator.domain.model.Stage;

import java.util.List;

public record SubmitTaskResponse(
        Long task_id,
        Stage stage,
        Long node_id,
        int score,
        List<ErrorTag> error_tags,
        FeedbackResponse feedback,
        double mastery_before,
        double mastery_delta,
        double mastery_after,
        NextAction next_action,
        com.panda.ainavigator.api.dto.session.NextTaskResponse next_task
) {
}
