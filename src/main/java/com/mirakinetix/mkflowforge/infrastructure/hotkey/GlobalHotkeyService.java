package com.mirakinetix.mkflowforge.infrastructure.hotkey;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GlobalHotkeyService implements AutoCloseable {

    private final Runnable callback;
    private final boolean macOS;
    private volatile boolean shortcutDown;
    private boolean started;
    private boolean registeredByUs;

    private final NativeKeyListener listener = new NativeKeyListener() {
        @Override
        public void nativeKeyPressed(NativeKeyEvent event) {
            if (!isShortcutKey(event.getKeyCode())) {
                return;
            }

            if (!shortcutDown
                    && matchesHotkey(event.getKeyCode(),
                                     event.getModifiers(), macOS)) {
                shortcutDown = true;
                callback.run();
                return;
            }

            // Prevent key-repeat from toggling dictation more than once.
            shortcutDown = true;
        }

        @Override
        public void nativeKeyReleased(NativeKeyEvent event) {
            if (isShortcutKey(event.getKeyCode())) {
                shortcutDown = false;
            }
        }

        @Override
        public void nativeKeyTyped(NativeKeyEvent event) {
            // No text is collected by the hotkey listener.
        }
    };

    public GlobalHotkeyService(Runnable callback) {
        this.callback = Objects.requireNonNull(callback, "callback");
        this.macOS = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("mac");
    }

    public synchronized void start() throws NativeHookException {
        if (started) {
            return;
        }

        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);

        if (!GlobalScreen.isNativeHookRegistered()) {
            GlobalScreen.registerNativeHook();
            registeredByUs = true;
        }

        GlobalScreen.addNativeKeyListener(listener);
        started = true;
    }

    private boolean isShortcutKey(int keyCode) {
        return keyCode == NativeKeyEvent.VC_F9
                || (!macOS && keyCode == NativeKeyEvent.VC_M);
    }

    public static boolean matchesHotkey(
            int keyCode,
            int modifiers,
            boolean macOS
    ) {
        if (macOS) {
            return keyCode == NativeKeyEvent.VC_F9
                    && (modifiers & NativeInputEvent.META_MASK) != 0
                    && (modifiers & NativeInputEvent.ALT_MASK) != 0;
        }

        boolean f9Shortcut = keyCode == NativeKeyEvent.VC_F9
                && (modifiers & NativeInputEvent.CTRL_MASK) != 0
                && (modifiers & NativeInputEvent.ALT_MASK) != 0;

        boolean letterShortcut = keyCode == NativeKeyEvent.VC_M
                && (modifiers & NativeInputEvent.CTRL_MASK) != 0
                && (modifiers & NativeInputEvent.SHIFT_MASK) != 0;

        return f9Shortcut || letterShortcut;
    }

    public static String shortcutDescription() {
        boolean macOS = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("mac");

        return macOS
                ? "Command + Option + F9"
                : "Ctrl + Shift + M (or Ctrl + Alt + F9)";
    }

    @Override
    public synchronized void close() {
        if (!started) {
            return;
        }

        GlobalScreen.removeNativeKeyListener(listener);
        started = false;
        shortcutDown = false;

        if (registeredByUs && GlobalScreen.isNativeHookRegistered()) {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException exception) {
                Logger.getLogger(GlobalHotkeyService.class.getName())
                        .log(Level.WARNING, "Could not unregister native hook", exception);
            }

            registeredByUs = false;
        }
    }
}