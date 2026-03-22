package navigator.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskMessageRequest {
    @NotBlank
    private String sessionId;
    /** USER */
    private String role;
    @NotBlank
    private String content;

    /** 可选；服务端仍以规则检测器为准 */
    private String userActionType;
    /** 非权威，仅遥测 */
    private String clientStageHint;
}
