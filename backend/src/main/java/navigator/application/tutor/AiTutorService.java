package navigator.application.tutor;

import navigator.api.dto.AiTutorChatRequest;

public interface AiTutorService {

    /**
     * R00035：内嵌导师单轮对话（无服务端会话记忆）。
     */
    AiTutorChatResult chat(AiTutorChatRequest request);

    /**
     * 流式单轮对话；由 Controller 以 SSE 等方式写出。实现侧在异常时追加兜底片段，不向调用方抛业务异常。
     */
    void streamChat(AiTutorChatRequest request, AiTutorChatStreamHandler handler);

    AiTutorTextResult getPrompt(String step, String knowledgePoint);

    AiTutorTextResult getExplain(String step, String knowledgePoint);

    AiTutorFeedbackResult getFeedback(String step, String knowledgePoint, String userAnswer);

    void prefetch(String step, String knowledgePoint);
}
