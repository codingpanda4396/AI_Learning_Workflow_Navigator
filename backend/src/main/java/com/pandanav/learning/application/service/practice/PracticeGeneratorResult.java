package com.pandanav.learning.application.service.practice;

import java.util.List;

public record PracticeGeneratorResult(
    List<PracticeDraftItem> items,
    String source,
    boolean fallbackTriggered,
    boolean llmParseSucceeded,
    String promptVersion,
    String provider,
    String model,
    Integer tokenInput,
    Integer tokenOutput,
    Integer latencyMs
) {
}
