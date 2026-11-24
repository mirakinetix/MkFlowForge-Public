package com.mirakinetix.mkflowforge.infrastructure.notes;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Creates a note in the installed Microsoft Sticky Notes Windows app.
 *
 * Microsoft does not expose a documented local API for the Windows app, so
 * this adapter activates the app and uses its documented Ctrl+N and Ctrl+V
 * keyboard actions.
 */
public final class WindowsStickyNotesService {

    private static final String APP_USER_MODEL_ID =
            "Microsoft.MicrosoftStickyNotes_8wekyb3d8bbwe!App";
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(70);

    private static final String INSTALL_CHECK_SCRIPT =
            "$ErrorActionPreference = 'SilentlyContinue'; "
                    + "if (Get-AppxPackage -Name 'Microsoft.MicrosoftStickyNotes') "
                    + "{ exit 0 } else { exit 10 }";

    private static final String ACTIVATE_AND_PASTE_SCRIPT = String.join("; ",
            "$ErrorActionPreference = 'Stop'",
            "$shell = New-Object -ComObject WScript.Shell",
            "$deadline = [DateTime]::UtcNow.AddSeconds(60)",
            "$activated = $false",
            "while (-not $activated -and [DateTime]::UtcNow -lt $deadline) "
                    + "{ $activated = $shell.AppActivate('Sticky Notes'); "
                    + "if (-not $activated) { Start-Sleep -Milliseconds 250 } }",
            "if (-not $activated) { exit 11 }",
            "Start-Sleep -Milliseconds 250",
            "$shell.SendKeys('^n')",
            "Start-Sleep -Milliseconds 500",
            "$shell.SendKeys('^v')",
            "Start-Sleep -Milliseconds 250",
            "exit 0"
    );

    private final boolean windows;

    public WindowsStickyNotesService() {
        this(System.getProperty("os.name", ""));
    }

    WindowsStickyNotesService(String operatingSystem) {
        windows = operatingSystem.toLowerCase(Locale.ROOT).contains("win");
    }

    public Result createNote(String text) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Note text must not be blank.");
        }

        if (!windows) {
            return Result.UNSUPPORTED_PLATFORM;
        }

        if (!isInstalled()) {
            return Result.NOT_INSTALLED;
        }

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable previousClipboard = clipboard.getContents(null);
        clipboard.setContents(new StringSelection(text), null);

        try {
            new ProcessBuilder(
                    "explorer.exe",
                    "shell:AppsFolder\\" + APP_USER_MODEL_ID
            ).start();

            runPowerShell(ACTIVATE_AND_PASTE_SCRIPT);
        } finally {
            restoreClipboard(clipboard, previousClipboard);
        }

        return Result.CREATED;
    }

    private boolean isInstalled() throws IOException, InterruptedException {
        Process process = startPowerShell(INSTALL_CHECK_SCRIPT);
        boolean finished = process.waitFor(
                PROCESS_TIMEOUT.toMillis(),
                TimeUnit.MILLISECONDS
        );

        if (!finished) {
            process.destroyForcibly();
            process.waitFor();
            throw new IOException("Timed out while checking Microsoft Sticky Notes.");
        }

        int exitCode = process.exitValue();

        if (exitCode == 0) {
            return true;
        }
        if (exitCode == 10) {
            return false;
        }

        throw new IOException(
                "Could not check whether Microsoft Sticky Notes is installed."
        );
    }

    private static void runPowerShell(String script)
            throws IOException, InterruptedException {
        Process process = startPowerShell(script);
        boolean finished = process.waitFor(
                PROCESS_TIMEOUT.toMillis(),
                TimeUnit.MILLISECONDS
        );

        if (!finished) {
            process.destroyForcibly();
            process.waitFor();
            throw new IOException(
                    "Timed out while opening Microsoft Sticky Notes."
            );
        }

        if (process.exitValue() != 0) {
            throw new IOException(
                    "Microsoft Sticky Notes opened, but MkFlowForge could not "
                            + "activate it and enter the note."
            );
        }
    }

    private static Process startPowerShell(String script) throws IOException {
        return new ProcessBuilder(
                "powershell.exe",
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-WindowStyle",
                "Hidden",
                "-Command",
                script
        )
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
    }

    private static void restoreClipboard(
            Clipboard clipboard,
            Transferable previousClipboard
    ) {
        if (previousClipboard == null) {
            return;
        }

        try {
            clipboard.setContents(previousClipboard, null);
        } catch (IllegalStateException ignored) {
            // Clipboard ownership may briefly be held by another application.
        }
    }

    public enum Result {
        CREATED,
        NOT_INSTALLED,
        UNSUPPORTED_PLATFORM
    }
}
