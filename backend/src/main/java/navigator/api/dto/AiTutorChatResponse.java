package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTutorChatResponse {
    private String reply;
    /** LLM 或 FALLBACK */
    private String source;
}
