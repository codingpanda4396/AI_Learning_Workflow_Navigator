package navigator.application.task;

import lombok.Data;
import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.TaskExecutionState;
import navigator.domain.model.TaskScaffold;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 单会话单任务的执行期运行时（内存）。
 */
@Data
public class TaskExecutionRuntime {
    private TaskScaffold scaffold;
    private TaskExecutionState state = TaskExecutionState.INIT;
    private int exploreTurnCount;
    private String checkpointQuestion;
    private String selfExplanationEvaluation;
    private final List<LearningActionType> actionHistory = new ArrayList<>();
    private final List<String> stateTransitionReasons = new ArrayList<>();

    public void recordAction(LearningActionType type) {
        if (type != null) {
            actionHistory.add(type);
        }
    }

    public void transitionTo(TaskExecutionState to, String reason) {
        this.state = to;
        if (reason != null) {
            stateTransitionReasons.add(to.name() + ":" + reason);
        }
    }

    public static String newScaffoldId() {
        return "scf_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
