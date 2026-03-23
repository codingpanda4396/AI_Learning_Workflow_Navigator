package navigator.application;

import navigator.api.dto.TaskFeedbackRequest;
import navigator.api.dto.TaskFeedbackResponse;
import navigator.application.tutor.AiTutorFeedbackResult;
import navigator.application.tutor.AiTutorService;
import org.springframework.stereotype.Service;

/**
 * 作答反馈：与 R0003 {@link AiTutorService#getFeedback} 同源（同步 LLM + 兜底）。
 */
@Service
public class TaskFeedbackService {

    private final AiTutorService aiTutorService;

    public TaskFeedbackService(AiTutorService aiTutorService) {
        this.aiTutorService = aiTutorService;
    }

    public TaskFeedbackResponse evaluate(TaskFeedbackRequest request) {
        String answer = request != null ? request.getAnswer() : "";
        String step = request != null && request.getStep() != null ? request.getStep() : "";
        String kp = request != null && request.getKnowledgePoint() != null ? request.getKnowledgePoint() : "";
        AiTutorFeedbackResult r = aiTutorService.getFeedback(step, kp, answer);
        TaskFeedbackResponse out = r.response();
        out.setSource(r.source());
        return out;
    }
}
