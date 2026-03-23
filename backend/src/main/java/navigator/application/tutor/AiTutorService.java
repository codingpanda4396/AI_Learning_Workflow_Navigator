package navigator.application.tutor;

public interface AiTutorService {

    AiTutorTextResult getPrompt(String step, String knowledgePoint);

    AiTutorTextResult getExplain(String step, String knowledgePoint);

    AiTutorFeedbackResult getFeedback(String step, String knowledgePoint, String userAnswer);

    void prefetch(String step, String knowledgePoint);
}
