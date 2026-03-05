package com.panda.ainavigator.api.dto.session;

public record MasterySummaryResponse(
        Long node_id,
        String node_name,
        double mastery_value
) {
}
