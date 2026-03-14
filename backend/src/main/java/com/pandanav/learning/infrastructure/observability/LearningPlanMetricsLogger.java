package com.pandanav.learning.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LearningPlanMetricsLogger {

    private final MeterRegistry meterRegistry;

    public LearningPlanMetricsLogger(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    public void logPreviewBuilt(boolean fallbackApplied, int evidenceCount, int alternativeCount) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("learning.plan.preview.total").increment();
        meterRegistry.counter(
            "learning.plan.preview.fallback",
            tags(List.of(
                Tag.of("applied", String.valueOf(fallbackApplied)),
                Tag.of("evidence_bucket", evidenceBucket(evidenceCount)),
                Tag.of("divergence_bucket", divergenceBucket(alternativeCount))
            ))
        ).increment();
        meterRegistry.summary("learning.plan.preview.evidence.count").record(evidenceCount);
        meterRegistry.summary("learning.plan.preview.candidate.alternative.count").record(alternativeCount);
    }

    public void logPreviewAccepted(boolean hasFirstTask) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(
            "learning.plan.preview.accepted",
            tags(List.of(Tag.of("has_first_task", String.valueOf(hasFirstTask))))
        ).increment();
    }

    public void logFirstTaskCompletion() {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("learning.plan.first.task.completed").increment();
    }

    private Tags tags(List<Tag> tags) {
        return Tags.of(tags);
    }

    private String evidenceBucket(int evidenceCount) {
        if (evidenceCount <= 0) {
            return "none";
        }
        if (evidenceCount == 1) {
            return "low";
        }
        if (evidenceCount == 2) {
            return "medium";
        }
        return "high";
    }

    private String divergenceBucket(int alternativeCount) {
        if (alternativeCount <= 0) {
            return "single";
        }
        if (alternativeCount == 1) {
            return "narrow";
        }
        if (alternativeCount == 2) {
            return "medium";
        }
        return "wide";
    }
}
