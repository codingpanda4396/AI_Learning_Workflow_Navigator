package com.pandanav.learning.domain.model;

public record PlanAdjustments(
    String intensity,
    String learningMode,
    boolean preferPrerequisite
) {

    public static PlanAdjustments defaults() {
        return new PlanAdjustments("STANDARD", "LEARN_THEN_PRACTICE", true);
    }

    public PlanAdjustments normalized() {
        return new PlanAdjustments(
            normalizeIntensity(intensity),
            normalizeLearningMode(learningMode),
            preferPrerequisite
        );
    }

    private static String normalizeIntensity(String value) {
        if (value == null || value.isBlank()) {
            return "STANDARD";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "LIGHT", "STANDARD", "INTENSIVE" -> normalized;
            default -> "STANDARD";
        };
    }

    private static String normalizeLearningMode(String value) {
        if (value == null || value.isBlank()) {
            return "LEARN_THEN_PRACTICE";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "LEARN_THEN_PRACTICE", "PRACTICE_DRIVEN", "MIXED" -> normalized;
            default -> "LEARN_THEN_PRACTICE";
        };
    }
}
