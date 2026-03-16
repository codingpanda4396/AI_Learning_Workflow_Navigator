package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.TaskType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskBlueprint {
    private String taskId;
    private String title;
    private TaskType taskType;
    private String goal;
    private Integer estimatedMinutes;
    private String promptScaffold;
    private List<String> completionCriteria;
    private List<String> evidenceToCollect;
    private String fallbackAction;
}
