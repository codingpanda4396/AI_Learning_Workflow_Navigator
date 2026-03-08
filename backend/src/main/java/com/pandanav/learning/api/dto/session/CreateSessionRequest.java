package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateSessionRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateSessionRequest(
    @NotBlank
    @JsonProperty("course_id")
    @Schema(name = "course_id", example = "computer_network")
    String courseId,
    @NotBlank
    @JsonProperty("chapter_id")
    @Schema(name = "chapter_id", example = "tcp")
    String chapterId,
    @JsonProperty("goal_text")
    @Schema(name = "goal_text", example = "理解 TCP 可靠传输机制并能做题")
    String goalText
) {
}
