package navigator.application.tutor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * LLM 反馈 JSON 反序列化（字段名与 prompt 约定一致）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiTutorFeedbackPayload {
    private boolean correct;
    private String diagnosis;
    private String suggestion;
    private String nextHint;
}
