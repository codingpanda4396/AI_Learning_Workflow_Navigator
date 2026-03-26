package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 单张动作卡一次提交的轮次快照（随 {@link ScaffoldActionRuntimeEntry} 持久化）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaffoldAttemptSnapshot {
    private int attemptNo;
    private String userInput;
    /** epoch millis */
    private long submittedAt;
    private String validationSummary;
    private String tutorSummary;
    private String runtimeStatus;
    /** TRAINING 等阶段写入的错误类型标签（可选） */
    @Builder.Default
    private List<String> errorTypes = new ArrayList<>();
}
