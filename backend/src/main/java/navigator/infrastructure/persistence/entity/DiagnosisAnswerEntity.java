package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("diagnosis_answer")
public class DiagnosisAnswerEntity {
    private Long id;
    private Long diagnosisSessionId;
    private String questionId;
    /**
     * 序列化后的诊断回答 JSON（包含选项/自由文本等）
     */
    private String answerJson;
    private LocalDateTime createdAt;
}

