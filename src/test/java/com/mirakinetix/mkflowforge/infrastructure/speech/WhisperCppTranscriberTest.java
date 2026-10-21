package com.mirakinetix.mkflowforge.infrastructure.speech;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhisperCppTranscriberTest {

    @TempDir
    Path tempDir;

    @Test
    void reportsMissingAudioFile() {
        Path executable = tempDir.resolve("whisper-cli.exe");
        Path model = tempDir.resolve("model.bin");
        Path missingAudio = tempDir.resolve("missing.wav");

        WhisperCppTranscriber transcriber = new WhisperCppTranscriber(
                executable, model, Duration.ofSeconds(5)
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> transcriber.transcribe(missingAudio)
        );

        assertTrue(exception.getMessage().contains("Audio recording not found"));
    }

    @Test
    void reportsMissingExecutable() throws IOException {
        Path audio = Files.write(
                tempDir.resolve("test.wav"), new byte[]{1, 2, 3}
        );
        Path model = Files.write(
                tempDir.resolve("model.bin"), new byte[]{1, 2, 3}
        );
        Path executable = tempDir.resolve("missing-whisper.exe");

        WhisperCppTranscriber transcriber = new WhisperCppTranscriber(
                executable, model, Duration.ofSeconds(5)
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> transcriber.transcribe(audio)
        );

        assertTrue(exception.getMessage().contains("Whisper executable not found"));
    }

    @Test
    void reportsMissingModel() throws IOException {
        Path audio = Files.write(
                tempDir.resolve("test.wav"), new byte[]{1, 2, 3}
        );
        Path executable = Files.write(
                tempDir.resolve("whisper-cli.exe"), new byte[]{1, 2, 3}
        );
        Path model = tempDir.resolve("missing-model.bin");

        WhisperCppTranscriber transcriber = new WhisperCppTranscriber(
                executable, model, Duration.ofSeconds(5)
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> transcriber.transcribe(audio)
        );

        assertTrue(exception.getMessage().contains("Whisper model not found"));
    }

    @Test
    void rejectsZeroTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WhisperCppTranscriber(
                        tempDir.resolve("whisper.exe"),
                        tempDir.resolve("model.bin"),
                        Duration.ZERO
                )
        );
    }
}