package com.panda.ainavigator.api.dto.task;

import java.util.List;

public record FeedbackResponse(String diagnosis, List<String> fixes) {
}
