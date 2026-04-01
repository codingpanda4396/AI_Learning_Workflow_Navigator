package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTutorChatResponse {
    private String reply;
    private String source;
    private Boolean canProceed;
    private String finalDraft;
    private String completionHint;
    private String summary;
}
