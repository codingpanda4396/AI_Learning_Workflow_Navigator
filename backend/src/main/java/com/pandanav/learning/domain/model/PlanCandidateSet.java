package com.pandanav.learning.domain.model;

import java.util.List;

public record PlanCandidateSet(
    List<EntryCandidate> entries,
    List<StrategyCandidate> strategies,
    List<IntensityCandidate> intensities,
    List<ActionTemplate> actionTemplates
) {
}
