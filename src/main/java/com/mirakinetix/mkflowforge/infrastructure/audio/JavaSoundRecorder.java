package com.mirakinetix.mkflowforge.infrastructure.audio;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JavaSoundRecorder {

    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000.0f,
            16,
            1,
            2,
            16000.0f,
            false
    );

    private final Object lock = new Object();

    private TargetDataLine line;
    private Thread writerThread;
    private Path recordingPath;
    private volatile IOException writerFailure;
    private boolean recording;

    public static AudioFormat recordingFormat() {
        return FORMAT;
    }

    public boolean isRecording() {
        synchronized (lock) {
            return recording;
        }
    }

    public Path startRecording() throws IOException, LineUnavailableException {
        synchronized (lock) {
            if (recording) {
                throw new IllegalStateException("Recording is already active.");
            }

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            TargetDataLine candidate;

            try {
                candidate = (TargetDataLine) AudioSystem.getLine(info);
            } catch (IllegalArgumentException exception) {
                throw new IOException(
                        "No microphone supports 16 kHz mono PCM recording. "
                                + "Check the microphone and Windows permissions.",
                        exception
                );
            }

            candidate.open(FORMAT);
            Path output;

            try {
                output = Files.createTempFile(
                        "mkflowforge-recording-", ".wav"
                );
            } catch (IOException exception) {
                candidate.close();
                throw exception;
            }

            AudioInputStream input = new AudioInputStream(candidate);

            line = candidate;
            recordingPath = output;
            writerFailure = null;
            recording = true;

            writerThread = new Thread(() -> {
                try (AudioInputStream audio = input) {
                    AudioSystem.write(
                            audio,
                            AudioFileFormat.Type.WAVE,
                            output.toFile()
                    );
                } catch (IOException exception) {
                    writerFailure = exception;
                }
            }, "mkflowforge-audio-writer");

            writerThread.setDaemon(true);

            try {
                candidate.start();
                writerThread.start();
            } catch (RuntimeException exception) {
                recording = false;
                line = null;
                writerThread = null;
                recordingPath = null;
                candidate.close();
                Files.deleteIfExists(output);
                throw exception;
            }

            return output;
        }
    }

    public Path stopRecording() throws IOException, InterruptedException {
        TargetDataLine activeLine;
        Thread activeWriter;
        Path output;

        synchronized (lock) {
            if (!recording || line == null) {
                throw new IllegalStateException("No recording is active.");
            }

            recording = false;
            activeLine = line;
            activeWriter = writerThread;
            output = recordingPath;

            line = null;
            writerThread = null;
            recordingPath = null;

            activeLine.stop();
            activeLine.close();
        }

        activeWriter.join(5000);

        if (activeWriter.isAlive()) {
            throw new IOException(
                    "Audio recording did not finish writing within five seconds."
            );
        }

        IOException failure = writerFailure;

        if (failure != null) {
            Files.deleteIfExists(output);
            throw new IOException("Could not write the WAV recording.", failure);
        }

        if (!Files.exists(output) || Files.size(output) <= 44) {
            Files.deleteIfExists(output);
            throw new IOException(
                    "The recording is empty. Speak for longer and try again."
            );
        }

        return output;
    }

    public void cancelRecording() {
        TargetDataLine activeLine;
        Thread activeWriter;
        Path output;

        synchronized (lock) {
            if (!recording) {
                return;
            }

            recording = false;
            activeLine = line;
            activeWriter = writerThread;
            output = recordingPath;

            line = null;
            writerThread = null;
            recordingPath = null;

            activeLine.stop();
            activeLine.close();
        }

        try {
            activeWriter.join(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        try {
            Files.deleteIfExists(output);
        } catch (IOException ignored) {
            // Cancellation should not crash the desktop UI.
        }
    }
}