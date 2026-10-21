package com.mirakinetix.mkflowforge.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalTextRefinerTest {

    private final LocalTextRefiner refiner = new LocalTextRefiner();

    @Test
    void normalizesWhitespace() {
        assertEquals("hello world", refiner.refine("  hello    world  "));
    }

    @Test
    void normalizesSpacingAroundPunctuation() {
        assertEquals(
                "Hello, world! Next",
                refiner.refine("Hello ,world!Next")
        );
    }

    @Test
    void addsSpacingAfterSentencePeriods() {
        assertEquals(
                "First. Second.",
                refiner.refine("First.Second.")
        );
    }

    @Test
    void handlesNullAndBlankInput() {
        assertAll(
                () -> assertEquals("", refiner.refine(null)),
                () -> assertEquals("", refiner.refine("   \n  "))
        );
    }

    @Test
    void preservesEllipses() {
        assertEquals(
                "Thinking... perhaps.",
                refiner.refine("Thinking... perhaps.")
        );
    }
}