import javax.sound.sampled.*;
import java.io.*;

public class AudioPlayer extends MediaPlayer<BufferedInputStream> {
    private BufferedInputStream inputStream;
    private Clip audioClip;

    private int frameOffsetMicros = 0;

    public AudioPlayer(Slider slider) {
        slider.setManualChangeListener(() -> peek(slider.getValue()));
    }

    public void setVideoFrameLength(int frameLength) {
        if (audioClip != null && frameLength > 0) {
            frameOffsetMicros = (int) (audioClip.getMicrosecondLength() / frameLength);
            while ((long) frameOffsetMicros * frameLength < audioClip.getMicrosecondLength()) {
                frameOffsetMicros += 1;
            }
        }
    }

    @Override
    public void open(BufferedInputStream mediaSource) {
        close();
        inputStream = mediaSource;
        if (inputStream != null) {
            reset();
        }
    }

    @Override
    public void close() {
        if (audioClip != null && audioClip.isOpen()) {
            audioClip.stop();
            audioClip.close();
        }
        currentState = State.Stopped;
    }

    @Override
    public void play() {
        if (currentState == State.Paused) {
            currentState = State.Playing;
            audioClip.start();
        }
    }

    @Override
    public void pause() {
        if (currentState == State.Playing) {
            currentState = State.Paused;
            audioClip.stop();
        }
    }

    @Override
    public void stop() {

    }

    @Override
    public void reset() {
        try {
            audioClip = AudioSystem.getClip();
            audioClip.open(AudioSystem.getAudioInputStream(inputStream));
            currentState = State.Paused;
        }
        catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            currentState = State.Stopped;
        }
    }

    @Override
    public void peek(long frameIndex) {
        if (audioClip != null) {
            audioClip.setMicrosecondPosition(frameIndex * frameOffsetMicros);
        }
    }
}
