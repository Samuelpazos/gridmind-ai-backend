package com.gridmind.domain;

public enum GridStatus {
    NORMAL(1),
    WARNING(2),
    CRITICAL(3),
    FAILED(4);

    private final int severity;

    GridStatus(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    public static GridStatus worstOf(GridStatus s1, GridStatus s2) {
        if (s1 == null) return s2 != null ? s2 : NORMAL;
        if (s2 == null) return s1;
        return s1.getSeverity() >= s2.getSeverity() ? s1 : s2;
    }
}
