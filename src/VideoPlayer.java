import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

class VideoPlayer extends MediaPlayer<ArrayList<File>> implements Runnable, ChangeListener {
    private final Slider slider;
    private final JLabel canvas;
    private ArrayList<File> videoFrames;

    long lastSyncTime = -1;         // time of current frame rendered on screen
    long elapsedTimeActual = 0;     // actual elapsed time of the movie, depends on runtime environment
    int elapsedTimeStandard = 0;    // standard elapsed time of movie, calculated by pre-defined FPS
    int elapsedTimeReference = 0;   // target display time for every frame, regardless of I/O time or something
    int frameDurationOffset = 0;    // average offset between ideal and actual time to display a frame in target FPS

    private Thread playbackThread;

    /* DEBUG ONLY PARAMETERS */
    int fakeFrameIndex = 0;

    public VideoPlayer(Slider slider, JLabel canvas) {
        currentState = State.Stopped;
        this.canvas = canvas;
        this.slider = slider;
        slider.addChangeListener(this);
    }

    private void setAndNotifyStateChanged(State newState) {
        if (currentState != newState) {
            currentState = newState;
            notifyStateChanged();
        }
    }

    @Override
    void open(ArrayList<File> mediaSource) {
        synchronized (this) {
            videoFrames = mediaSource;
            slider.reset(videoFrames);
        }
        reset();
    }

    @Override
    void close() {

    }

    @Override
    public void reset() {
        synchronized (this) {
            setAndNotifyStateChanged(State.Paused);
            lastSyncTime = -1;
            peek(0);
            slider.setValue(0);
        }
        if (playbackThread == null) {
            playbackThread = new Thread(this);
            playbackThread.start();
        }
    }

    @Override
    protected void peek(long frameIndex) {
        if (canvas != null) {
            BufferedImage newImage = MediaReader.getInstance().readRgbFile(videoFrames.get((int) frameIndex));
            canvas.setIcon(new ImageIcon(newImage));
        }
        if (slider != null && slider.getValue() != frameIndex) {
            slider.setValue((int) frameIndex);
        }
    }

    @Override
    public void play() {
        if (currentState != State.Playing) {
            setAndNotifyStateChanged(State.Playing);
            synchronized (this) {
                lastSyncTime = -1;
                this.notifyAll();
            }
        }
    }

    @Override
    public void pause() {
        if (currentState != State.Paused) {
            synchronized (this) {
                setAndNotifyStateChanged(State.Paused);
            }
        }
    }

    @Override
    public void stop() {

    }

    @Override
    public void run() {
        // for now the thread never quit, since it
        // won't reach Stopped state after execution
        // to DEBUG, call stop() to exit the thread
        while (currentState != State.Stopped) {
            synchronized (VideoPlayer.this) {
                int standard = getFrameDurationBiased(slider.getValue());
                int cali = 0;
                if (lastSyncTime > 0) {
                    BufferedImage newImage = MediaReader.getInstance().readRgbFile(videoFrames.get(slider.getValue()));
                    canvas.setIcon(new ImageIcon(newImage));
                    if (slider.getValue() < slider.getMaximum()) {
                        long now = System.currentTimeMillis();
                        long actual = now - lastSyncTime;
                        lastSyncTime = now;

                        elapsedTimeActual += actual;
                        elapsedTimeReference += standard;
                        elapsedTimeStandard += getFrameDurationStandard(slider.getValue());
                        fakeFrameIndex++;
                        slider.forward();
                        frameDurationOffset = (int) (elapsedTimeActual - elapsedTimeReference) / fakeFrameIndex;
                        cali = (int) (elapsedTimeActual - elapsedTimeStandard);

                        if (standard - cali < 0) {
                            cali = standard;
                            System.out.printf("DELAYED! SKIP CURRENT FRAME: %d\n", slider.getValue());
                        } else
                            System.out.printf("Average FPS: %f, Accumulated Delay: %dms\n",
                                    1000 / ((float) elapsedTimeActual / fakeFrameIndex), cali);
                    }
                    else setAndNotifyStateChanged(State.Paused);
                }
                else lastSyncTime = System.currentTimeMillis();
                try {
                    if (currentState == State.Paused) {
                        wait();
                    }
                    else if (currentState == State.Playing) {
                        if (standard - cali > 0) {
                            wait(standard - cali);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Video Playback Thread : exits");
    }

    private int getFrameDurationBiased(int frameIndex) {
        return ((frameIndex % 3 == 1) ? 34 : 33) - frameDurationOffset;
    }

    private int getFrameDurationStandard(int frameIndex) {
        return (frameIndex % 3 == 1) ? 34 : 33;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (canvas != null && currentState == State.Paused) {
            BufferedImage newImage = MediaReader.getInstance().readRgbFile(videoFrames.get(slider.getValue()));
            canvas.setIcon(new ImageIcon(newImage));
        }
    }
}