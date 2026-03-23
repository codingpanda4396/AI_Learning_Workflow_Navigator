package navigator.api.controller;

import jakarta.validation.Valid;
import navigator.api.GlobalResponse;
import navigator.api.dto.AiTutorChatRequest;
import navigator.api.dto.AiTutorChatResponse;
import navigator.api.dto.AiTutorEnvelopeResponse;
import navigator.api.dto.AiTutorExplainRequest;
import navigator.api.dto.AiTutorExplainResponse;
import navigator.api.dto.AiTutorPrefetchRequest;
import navigator.api.dto.TaskFeedbackRequest;
import navigator.api.dto.TaskFeedbackResponse;
import navigator.application.llm.LlmProperties;
import navigator.application.tutor.AiTutorChatResult;
import navigator.application.tutor.AiTutorChatStreamHandler;
import navigator.application.tutor.AiTutorFeedbackResult;
import navigator.application.tutor.AiTutorService;
import navigator.application.tutor.AiTutorTextResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai-tutor")
public class AiTutorController {

    private final AiTutorService aiTutorService;
    private final LlmProperties llmProperties;

    public AiTutorController(AiTutorService aiTutorService, LlmProperties llmProperties) {
        this.aiTutorService = aiTutorService;
        this.llmProperties = llmProperties;
    }

    @GetMapping("/prompt")
    public GlobalResponse<AiTutorEnvelopeResponse> prompt(
            @RequestParam String step,
            @RequestParam String knowledgePoint) {
        AiTutorTextResult r = aiTutorService.getPrompt(step, knowledgePoint);
        return GlobalResponse.ok(new AiTutorEnvelopeResponse(r.source(), r.content()));
    }

    @GetMapping("/explain")
    public GlobalResponse<AiTutorEnvelopeResponse> explainGet(
            @RequestParam String step,
            @RequestParam String knowledgePoint) {
        AiTutorTextResult r = aiTutorService.getExplain(step, knowledgePoint);
        return GlobalResponse.ok(new AiTutorEnvelopeResponse(r.source(), r.content()));
    }

    /**
     * 兼容旧前端：POST body；行为与 GET 一致（忽略 userPrompt，同步路径不调用 LLM）。
     */
    @PostMapping("/explain")
    public GlobalResponse<AiTutorExplainResponse> explainPost(@RequestBody AiTutorExplainRequest req) {
        String step = req.getStep() != null ? req.getStep() : "";
        String kp = req.getKnowledgePoint() != null ? req.getKnowledgePoint() : "";
        AiTutorTextResult r = aiTutorService.getExplain(step, kp);
        return GlobalResponse.ok(new AiTutorExplainResponse(r.source(), r.content()));
    }

    @PostMapping("/feedback")
    public GlobalResponse<TaskFeedbackResponse> feedback(@Valid @RequestBody TaskFeedbackRequest request) {
        AiTutorFeedbackResult r = aiTutorService.getFeedback(
                request.getStep() != null ? request.getStep() : "",
                request.getKnowledgePoint() != null ? request.getKnowledgePoint() : "",
                request.getAnswer());
        TaskFeedbackResponse out = r.response();
        out.setSource(r.source());
        return GlobalResponse.ok(out);
    }

    @PostMapping("/prefetch")
    public GlobalResponse<Void> prefetch(@Valid @RequestBody AiTutorPrefetchRequest body) {
        aiTutorService.prefetch(body.getStep(), body.getKnowledgePoint());
        return GlobalResponse.ok(null);
    }

    @PostMapping("/chat")
    public GlobalResponse<AiTutorChatResponse> chat(@Valid @RequestBody AiTutorChatRequest request) {
        AiTutorChatResult r = aiTutorService.chat(request);
        return GlobalResponse.ok(new AiTutorChatResponse(r.reply(), r.source()));
    }

    /**
     * 内嵌导师单轮对话（流式）：SSE，事件名 meta / delta / done / error；data 为 JSON。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AiTutorChatRequest request) {
        long ttlMs = Math.max(120_000L, (long) llmProperties.getTimeoutMs() + 60_000L);
        SseEmitter emitter = new SseEmitter(ttlMs);
        CompletableFuture.runAsync(() -> {
            try {
                aiTutorService.streamChat(request, new AiTutorChatStreamHandler() {
                    @Override
                    public void onMeta(String source) {
                        sseSend(emitter, "meta", Map.of("source", source != null ? source : ""));
                    }

                    @Override
                    public void onDelta(String text) {
                        if (text == null || text.isEmpty()) {
                            return;
                        }
                        sseSend(emitter, "delta", Map.of("text", text));
                    }
                });
                sseSend(emitter, "done", Map.of());
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("message",
                                    ex.getMessage() != null ? ex.getMessage() : "流式输出失败")));
                } catch (IOException ignored) {
                    // client gone
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private static void sseSend(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
