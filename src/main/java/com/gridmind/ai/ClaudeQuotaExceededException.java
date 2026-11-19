package com.gridmind.ai;

public class ClaudeQuotaExceededException extends RuntimeException {

    public ClaudeQuotaExceededException(int maxRequestsPerDay) {
        super("Se alcanzó el límite diario configurado de " + maxRequestsPerDay + " solicitudes a Claude.");
    }
}
