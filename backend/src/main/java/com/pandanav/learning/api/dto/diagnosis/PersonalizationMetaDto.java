package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Meta for debugging / demo: question selection mode and signals used.
 * personalizationLevel 仅提交阶段有值，创建阶段为 null 且不序列化。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalizationMetaDto(
    @JsonProperty("personalizationLevel")
    String personalizationLevel,
    @JsonProperty("usedSignals")
    List<String> usedSignals,
    @JsonProperty("questionSelectionMode")
    String questionSelectionMode
) {
    public PersonalizationMetaDto {
        usedSignals = usedSignals == null ? List.of() : List.copyOf(usedSignals);
    }
}
