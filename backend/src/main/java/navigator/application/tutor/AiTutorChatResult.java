package navigator.application.tutor;

public record AiTutorChatResult(
        String source,
        String reply,
        boolean canProceed,
        String finalDraft,
        String completionHint,
        String summary
) {
}
