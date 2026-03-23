package navigator.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiTutorChatRequest {

    @NotBlank
    @Size(max = 2000)
    private String message;

    @Valid
    @NotNull
    private Context context;

    @Data
    public static class Context {
        private Integer step;

        @NotBlank
        private String knowledge;

        @NotBlank
        private String phase;

        /** 人读标签，优先用于 system prompt 展示 */
        private String knowledgeLabel;
    }
}
