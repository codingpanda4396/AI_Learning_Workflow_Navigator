package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTutorEnvelopeResponse {
    /** CACHE | FALLBACK | LLM */
    private String source;
    private String content;
}
