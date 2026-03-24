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
import navigator.application.tutor.AiTutorChatResult;
import navigator.application.tutor.AiTutorFeedbackResult;
import navigator.application.tutor.AiTutorService;
import navigator.application.tutor.AiTutorStreamEvent;
import navigator.application.tutor.AiTutorTextResult;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-tutor")
public class AiTutorController {

    private static final String STREAM_ERROR_MESSAGE = "\u6D41\u5F0F\u8F93\u51FA\u5931\u8D25";

    private final AiTutorService aiTutorService;

    public AiTutorController(AiTutorService aiTutorService) {
        this.aiTutorService = aiTutorService;
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

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, String>>> chatStream(@Valid @RequestBody AiTutorChatRequest request) {
        return aiTutorService.streamChat(request)
                .concatWithValues(AiTutorStreamEvent.done())
                .map(this::toSse)
                .onErrorResume(ex -> Flux.just(toSse(AiTutorStreamEvent.error(
                        ex.getMessage() != null ? ex.getMessage() : STREAM_ERROR_MESSAGE))));
    }

    private ServerSentEvent<Map<String, String>> toSse(AiTutorStreamEvent event) {
        return ServerSentEvent.<Map<String, String>>builder()
                .event(event.event())
                .data(event.data())
                .build();
    }
}
