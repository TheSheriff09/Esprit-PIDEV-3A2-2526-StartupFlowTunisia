package tn.esprit.utils;

import javax.sound.sampled.*;

public class AudioCapture {

    public interface AudioListener {
        void onAudioData(byte[] data, int numBytes);
    }

    private TargetDataLine microphone;
    private Thread captureThread;
    private boolean isRecording = false;

    // Vosk standard model requirement: 16000 Hz, 16 bit, Mono, Signed,
    // Little-Endian
    private final AudioFormat audioFormat = new AudioFormat(16000.0f, 16, 1, true, false);

    public void start(AudioListener listener) throws Exception {
        if (isRecording)
            return;

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            throw new Exception("Microphone not supported for 16kHz 16-bit Mono");
        }

        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(audioFormat);
        microphone.start();
        isRecording = true;

        captureThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (isRecording) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0 && listener != null) {
                    listener.onAudioData(buffer, bytesRead);
                }
            }
        });
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void stop() {
        isRecording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }
    }
}
