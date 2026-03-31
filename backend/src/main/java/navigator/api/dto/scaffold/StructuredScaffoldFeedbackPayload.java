package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次提交后的结构化反馈（窄展示，避免长篇讲稿）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredScaffoldFeedbackPayload {
    private String completeness;
    @Builder.Default
    private List<String> issuePoints = new ArrayList<>();
    private String minimalRevision;
    private String nextAction;
}
