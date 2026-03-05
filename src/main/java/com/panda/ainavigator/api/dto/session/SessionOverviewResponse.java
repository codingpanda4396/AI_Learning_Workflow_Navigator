package com.panda.ainavigator.api.dto.session;

import com.panda.ainavigator.domain.model.Stage;

import java.util.List;

public record SessionOverviewResponse(
        Long session_id,
        String course_id,
        String chapter_id,
        String goal_text,
        Long current_node_id,
        Stage current_stage,
        List<TimelineItemResponse> timeline,
        NextTaskResponse next_task,
        List<MasterySummaryResponse> mastery_summary
) {
}
