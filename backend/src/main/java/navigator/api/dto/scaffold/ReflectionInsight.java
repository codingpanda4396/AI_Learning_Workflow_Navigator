package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统从 TRAINING 过程证据中汇总的观察（与用户自述反思并列）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReflectionInsight {
    @Builder.Default
    private List<String> repeatedErrorTypes = new ArrayList<>();
    private String mostDifficultActionId;
    private int totalAttempts;
    @Builder.Default
    private List<String> improvedAspects = new ArrayList<>();
}
