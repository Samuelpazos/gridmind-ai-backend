package com.gridmind.ai;

public class ClaudeNotConfiguredException extends RuntimeException {
    public ClaudeNotConfiguredException() {
        super("Claude is not configured. Set CLAUDE_API_KEY in the backend environment.");
    }
}
