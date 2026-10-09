package com.gridmind.ai;

public class TransformerNotFoundException extends RuntimeException {
    public TransformerNotFoundException(String transformerId) {
        super("Transformer not found: " + transformerId);
    }
}
