package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_goal")
public class LearningGoalEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
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
