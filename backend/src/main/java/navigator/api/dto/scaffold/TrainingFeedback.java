package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingFeedback {
    @Builder.Default
    private List<TrainingDetectedProblem> detectedProblems = new ArrayList<>();
    @Builder.Default
    private List<String> errorTypes = new ArrayList<>();
    private String revisionInstruction;
    /** 本轮是否达到通过标准（可进入下一动作或下一阶段） */
    private boolean canProceed;
}
