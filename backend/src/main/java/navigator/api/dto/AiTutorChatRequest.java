package navigator.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiTutorChatRequest {

    @Valid
    @NotNull
    @Size(min = 1, max = 40)
    private List<Message> messages = new ArrayList<>();

    @Valid
    @NotNull
    private Context context;

    @Data
    public static class Message {
        @NotBlank
        private String role;

        @NotBlank
        @Size(max = 4000)
        private String content;
    }

    @Data
    public static class Context {
        private Integer step;

        @NotBlank
        private String knowledge;

        @NotBlank
        private String phase;

        private String knowledgeLabel;

        private String sessionId;

        private String taskId;
    }
}
