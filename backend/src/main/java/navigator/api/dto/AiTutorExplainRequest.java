package navigator.api.dto;

import lombok.Data;

@Data
public class AiTutorExplainRequest {
    private String step;
    private String knowledgePoint;
    private String userPrompt;
}
