import javax.sound.sampled.*;
import java.io.*;

public class AudioPlayer extends MediaPlayer<String> {
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
    public void open(String mediaSource) {
        MediaReader reader = MediaReader.getInstance();
        inputStream = reader.readWavFile(mediaSource);
        reset();
    }

    @Override
    public void close() {
        if (audioClip != null && audioClip.isOpen()) {
            audioClip.stop();
            audioClip.close();
        }
    }

    @Override
    public void play() {
        audioClip.start();
    }

    @Override
    public void pause() {
        audioClip.stop();
    }

    @Override
    public void stop() {

    }

    @Override
    void reset() {
        try {
            close();
            audioClip = AudioSystem.getClip();
            audioClip.open(AudioSystem.getAudioInputStream(inputStream));
        }
        catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    void peek(long frameIndex) {
        if (audioClip != null) {
            audioClip.setMicrosecondPosition(frameIndex * frameOffsetMicros);
        }
    }
}
