package com.mirakinetix.mkflowforge.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognizes the spoken "post it" command only when it ends the transcript.
 */
public final class PostItCommandParser {

    private static final Pattern COMMAND_AT_END = Pattern.compile(
            "(?iu)(?:^|\\s+)post[\\s-]?it[.!?,;:…\\s]*$"
    );

    private static final Pattern TRAILING_SEPARATOR = Pattern.compile("[,;:]\\s*$");

    public ParsedTranscript parse(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return new ParsedTranscript(false, "");
        }

        Matcher matcher = COMMAND_AT_END.matcher(transcript.strip());

        if (!matcher.find()) {
            return new ParsedTranscript(false, transcript.strip());
        }

        String noteText = transcript.strip().substring(0, matcher.start()).strip();
        noteText = TRAILING_SEPARATOR.matcher(noteText).replaceFirst("").strip();
        return new ParsedTranscript(true, noteText);
    }

    public record ParsedTranscript(boolean postItCommand, String text) {}
}
