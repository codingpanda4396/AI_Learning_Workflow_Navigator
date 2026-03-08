package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SessionHistoryResponse(
    int page,
    @JsonProperty("page_size")
    int pageSize,
    long total,
    @JsonProperty("total_pages")
    int totalPages,
    List<SessionHistoryItemResponse> items
) {
}
