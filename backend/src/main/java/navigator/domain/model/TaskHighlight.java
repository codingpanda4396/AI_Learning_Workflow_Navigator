package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskHighlight {
    private String taskId;
    private String title;
    private String completionStatus;
    private String learned;
    private String issue;
}
