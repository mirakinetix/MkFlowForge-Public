package com.mirakinetix.mkflowforge.application;

public final class LocalTextRefiner {

    public String refine(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String text = input.strip().replaceAll("\\s+", " ");
        text = text.replaceAll("\\s+([,.;:!?])", "$1");
        text = text.replaceAll(
                "([,;:!?])(?=\\S)(?![,;:!?])",
                "$1 "
        );
        text = text.replaceAll(
                "(?<!\\.)\\.(?!\\.)(?=\\S)",
                ". "
        );

        return text.replaceAll("\\s+", " ").strip();
    }
}