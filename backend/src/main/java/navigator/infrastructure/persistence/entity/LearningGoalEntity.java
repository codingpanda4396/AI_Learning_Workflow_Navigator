package navigator.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearningGoalEntity {
    private Long id;
    private String rawGoalText;
    private String timeBudget;
    private String selfReportedLevel;
    private String preferenceTagsJson;
    private String goalTypeHint;
    private String subjectHint;
    private String topicHintsJson;
    private String sourceContext;
    private String structuredGoalJson;
    private String goalContextJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

