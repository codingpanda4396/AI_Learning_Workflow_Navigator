package navigator.application.tutor;

import navigator.api.dto.TaskFeedbackResponse;

public record AiTutorFeedbackResult(String source, TaskFeedbackResponse response) {
}
