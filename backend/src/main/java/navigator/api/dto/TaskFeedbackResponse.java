package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskFeedbackResponse {
    private boolean correct;
    private String comment;
    private String suggestion;
    /** 展示用：肯定要点（可与 comment 并存，供前端分层展示） */
    private String praise;
    /** 展示用：缺口提醒 */
    private String gap;
    /** 展示用：建议下一步补充 */
    private String nextHint;
    /** R0003：LLM | FALLBACK | CACHE（若复用） */
    private String source;
}
