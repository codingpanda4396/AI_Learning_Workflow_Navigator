package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 单张动作卡在引擎中的运行时快照（随 {@link TaskScaffold} 持久化）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaffoldActionRuntimeEntry {
    private String actionId;
    private String userInput;
    /** PENDING | PASS | FAIL */
    private String validationStatus;
    private String lastTutorFeedback;
    private int retryCount;
    private boolean completed;
    /**
     * NOT_STARTED | IN_PROGRESS | REVISION_REQUIRED | PASSED | COMPLETED
     */
    private String runtimeStatus;
    /** 当前动作下累计提交次数 */
    private int attemptNo;
    @Builder.Default
    private List<ScaffoldAttemptSnapshot> attemptSnapshots = new ArrayList<>();
}
