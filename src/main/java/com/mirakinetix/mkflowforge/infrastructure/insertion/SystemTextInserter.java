package com.mirakinetix.mkflowforge.infrastructure.insertion;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.Locale;

public final class SystemTextInserter {

    public void paste(String text) throws AWTException {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text to insert must not be blank.");
        }

        // Copy first so users retain the transcript if automated paste fails.
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);

        Robot robot = new Robot();
        robot.setAutoDelay(35);
        robot.delay(180);

        boolean macOS = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("mac");

        int modifier = macOS ? KeyEvent.VK_META : KeyEvent.VK_CONTROL;

        robot.keyPress(modifier);
        try {
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
        } finally {
            robot.keyRelease(modifier);
        }
    }
}