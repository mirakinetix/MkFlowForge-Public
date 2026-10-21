package com.mirakinetix.mkflowforge.infrastructure.hotkey;

import org.junit.jupiter.api.Test;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalHotkeyServiceTest {

    @Test
    void acceptsWindowsShortcut() {
        int modifiers = NativeInputEvent.CTRL_MASK
                | NativeInputEvent.ALT_MASK;

        assertTrue(GlobalHotkeyService.matchesHotkey(
                NativeKeyEvent.VC_F9, modifiers, false
        ));
    }

    @Test
    void acceptsMacShortcut() {
        int modifiers = NativeInputEvent.META_MASK
                | NativeInputEvent.ALT_MASK;

        assertTrue(GlobalHotkeyService.matchesHotkey(
                NativeKeyEvent.VC_F9, modifiers, true
        ));
    }

    @Test
    void rejectsShortcutWithoutAlt() {
        assertFalse(GlobalHotkeyService.matchesHotkey(
                NativeKeyEvent.VC_F9,
                NativeInputEvent.CTRL_MASK,
                false
        ));
    }

    @Test
    void rejectsWrongKey() {
        assertFalse(GlobalHotkeyService.matchesHotkey(
                NativeKeyEvent.VC_F8,
                NativeInputEvent.CTRL_MASK | NativeInputEvent.ALT_MASK,
                false
        ));
    }

    @Test
    void recognizesWindowsCtrlShiftMAsAlternative() {
        org.junit.jupiter.api.Assertions.assertTrue(
                GlobalHotkeyService.matchesHotkey(
                        NativeKeyEvent.VC_M,
                        NativeInputEvent.CTRL_MASK | NativeInputEvent.SHIFT_MASK,
                        false
                )
        );
    }

    @Test
    void rejectsWindowsLetterShortcutOnMac() {
        org.junit.jupiter.api.Assertions.assertFalse(
                GlobalHotkeyService.matchesHotkey(
                        NativeKeyEvent.VC_M,
                        NativeInputEvent.META_MASK | NativeInputEvent.ALT_MASK,
                        true
                )
        );
    }
}