package navigator.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskFeedbackRequest {
    @NotBlank
    private String answer;

    /** 执行脚手架阶段标识，供 R0003 导师裁剪上下文 */
    private String step;

    /** 当前知识点标签，供 R0003 导师裁剪上下文 */
    private String knowledgePoint;
}
