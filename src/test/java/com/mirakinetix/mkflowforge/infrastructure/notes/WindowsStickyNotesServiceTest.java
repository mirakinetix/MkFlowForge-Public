package com.mirakinetix.mkflowforge.infrastructure.notes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WindowsStickyNotesServiceTest {

    @Test
    void reportsUnsupportedPlatformWithoutLaunchingWindowsTools() throws Exception {
        WindowsStickyNotesService service =
                new WindowsStickyNotesService("Mac OS X");

        assertEquals(
                WindowsStickyNotesService.Result.UNSUPPORTED_PLATFORM,
                service.createNote("A note")
        );
    }

    @Test
    void rejectsBlankNoteText() {
        WindowsStickyNotesService service =
                new WindowsStickyNotesService("Windows 11");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createNote(" ")
        );
    }
}
