package com.mirakinetix.mkflowforge.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostItCommandParserTest {

    private final PostItCommandParser parser = new PostItCommandParser();

    @Test
    void removesTrailingPostItCommandAndSeparator() {
        PostItCommandParser.ParsedTranscript parsed =
                parser.parse("Buy milk, post it.");

        assertTrue(parsed.postItCommand());
        assertEquals("Buy milk", parsed.text());
    }

    @Test
    void recognizesPostItWithHyphenAndMixedCase() {
        PostItCommandParser.ParsedTranscript parsed =
                parser.parse("Call Sam POST-IT!");

        assertTrue(parsed.postItCommand());
        assertEquals("Call Sam", parsed.text());
    }

    @Test
    void doesNotTreatPostItInTheMiddleAsACommand() {
        PostItCommandParser.ParsedTranscript parsed =
                parser.parse("Post it on the desk tomorrow");

        assertFalse(parsed.postItCommand());
        assertEquals("Post it on the desk tomorrow", parsed.text());
    }

    @Test
    void detectsCommandWithoutNoteContent() {
        PostItCommandParser.ParsedTranscript parsed = parser.parse("Post it");

        assertTrue(parsed.postItCommand());
        assertEquals("", parsed.text());
    }

    @Test
    void acceptsNullAndBlankTranscripts() {
        assertEquals(
                new PostItCommandParser.ParsedTranscript(false, ""),
                parser.parse(null)
        );
        assertEquals(
                new PostItCommandParser.ParsedTranscript(false, ""),
                parser.parse("  ")
        );
    }
}
