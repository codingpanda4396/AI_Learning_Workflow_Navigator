package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningMethodReview {
    private String headline;
    private String summary;
    private List<String> strengths;
    private List<String> risks;
    private List<String> nextFocus;
}
