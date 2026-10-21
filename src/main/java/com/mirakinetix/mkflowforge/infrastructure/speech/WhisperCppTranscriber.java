package com.mirakinetix.mkflowforge.infrastructure.speech;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class WhisperCppTranscriber {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(3);

    private final Path executable;
    private final Path model;
    private final Duration timeout;

    public WhisperCppTranscriber() {
        this(defaultExecutable(), defaultModel(), DEFAULT_TIMEOUT);
    }

    public WhisperCppTranscriber(
            Path executable,
            Path model,
            Duration timeout
    ) {
        this.executable = executable.toAbsolutePath().normalize();
        this.model = model.toAbsolutePath().normalize();
        this.timeout = timeout;

        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException(
                    "Transcription timeout must be positive."
            );
        }
    }

    public String transcribe(Path audioFile)
            throws IOException, InterruptedException {

        Path audio = audioFile.toAbsolutePath().normalize();

        if (!Files.isRegularFile(audio)) {
            throw new IOException("Audio recording not found: " + audio);
        }
        if (!Files.isRegularFile(executable)) {
            throw new IOException(
                    "Whisper executable not found: " + executable
                    + ". Install the local speech engine or configure "
                    + "MKFLOWFORGE_WHISPER_CLI."
            );
        }
        if (!Files.isRegularFile(model)) {
            throw new IOException(
                    "Whisper model not found: " + model
                    + ". Download the local model or configure "
                    + "MKFLOWFORGE_WHISPER_MODEL."
            );
        }

        Path workDirectory = Files.createTempDirectory(
                "mkflowforge-whisper-run-"
        );
        Path outputBase = workDirectory.resolve("transcript");
        Path transcriptFile = workDirectory.resolve("transcript.txt");
        Path logFile = workDirectory.resolve("whisper.log");

        Process process = null;

        try {
            List<String> command = List.of(
                    executable.toString(),
                    "-m", model.toString(),
                    "-f", audio.toString(),
                    "-l", "auto",
                    "-nt",
                    "-np",
                    "-otxt",
                    "-of", outputBase.toString(),
                    "-t", Integer.toString(threadCount())
            );

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(executable.getParent().toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(logFile.toFile());

            process = builder.start();

            boolean finished = process.waitFor(
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
                throw new IOException(
                        "Offline transcription timed out after "
                                + timeout.toSeconds() + " seconds."
                );
            }

            if (process.exitValue() != 0) {
                throw new IOException(
                        "Whisper exited with code " + process.exitValue()
                                + ". Diagnostics: " + readDiagnostics(logFile)
                );
            }

            if (!Files.isRegularFile(transcriptFile)) {
                throw new IOException(
                        "Whisper completed without producing a transcript. "
                                + "Diagnostics: " + readDiagnostics(logFile)
                );
            }

            return Files.readString(
                    transcriptFile,
                    StandardCharsets.UTF_8
            ).strip();

        } catch (InterruptedException exception) {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw exception;
        } finally {
            deleteQuietly(transcriptFile);
            deleteQuietly(logFile);
            deleteQuietly(workDirectory);
        }
    }

    private static int threadCount() {
        return Math.max(
                1,
                Math.min(4, Runtime.getRuntime().availableProcessors())
        );
    }

    private static String readDiagnostics(Path logFile) {
        try {
            if (!Files.isRegularFile(logFile)) {
                return "No diagnostic log was created.";
            }

            String text = Files.readString(logFile, StandardCharsets.UTF_8);
            int limit = 3000;

            if (text.length() > limit) {
                return text.substring(text.length() - limit);
            }

            return text;
        } catch (IOException exception) {
            return "Could not read Whisper diagnostics: "
                    + exception.getMessage();
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Temporary-file cleanup must not hide the original result.
        }
    }

    private static Path defaultExecutable() {
        String configured = System.getenv("MKFLOWFORGE_WHISPER_CLI");

        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }

        Path root = runtimeRoot();
        boolean windows = System.getProperty("os.name")
                .toLowerCase()
                .contains("win");

        return root.resolve("bin-b5454")
                .resolve(windows ? "whisper-cli.exe" : "whisper-cli");
    }

    private static Path defaultModel() {
        String configured = System.getenv("MKFLOWFORGE_WHISPER_MODEL");

        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }

        return runtimeRoot().resolve("models").resolve("ggml-base.bin");
    }

    private static Path runtimeRoot() {
        boolean windows = System.getProperty("os.name")
                .toLowerCase()
                .contains("win");

        String localAppData = System.getenv("LOCALAPPDATA");

        if (windows && localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "MkFlowForge", "whisper");
        }

        return Path.of(
                System.getProperty("user.home"),
                "Library",
                "Application Support",
                "MkFlowForge",
                "whisper"
        );
    }
}