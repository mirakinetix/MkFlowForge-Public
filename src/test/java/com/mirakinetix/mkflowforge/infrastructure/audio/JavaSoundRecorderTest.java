package com.mirakinetix.mkflowforge.infrastructure.audio;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JavaSoundRecorderTest {

    @Test
    void recordingFormatIsMonoSixteenKhzPcm() {
        AudioFormat format = JavaSoundRecorder.recordingFormat();

        assertAll(
                () -> assertEquals(16000.0f, format.getSampleRate()),
                () -> assertEquals(16, format.getSampleSizeInBits()),
                () -> assertEquals(1, format.getChannels()),
                () -> assertEquals(2, format.getFrameSize()),
                () -> assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.getEncoding()),
                () -> assertFalse(format.isBigEndian())
        );
    }

    @Test
    void recorderStartsInInactiveState() {
        JavaSoundRecorder recorder = new JavaSoundRecorder();

        assertFalse(recorder.isRecording());
    }
}