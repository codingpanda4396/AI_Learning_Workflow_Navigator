package com.pandanav.learning.domain.enums;

import java.util.Locale;

public enum DiagnosisStatus {
    READY,
    SUBMITTED,
    EVALUATED;

    public static DiagnosisStatus fromDb(String rawStatus) {
        String normalized = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "GENERATED" -> READY;
            case "PROFILED" -> EVALUATED;
            default -> DiagnosisStatus.valueOf(normalized);
        };
    }
}
