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

    private Thread playbackThread;

    /* SYNCHRONIZATION PARAMETERS */
    long lastSyncTime = -1;         // time of current frame rendered on screen
    long elapsedTimeActual = 0;     // actual elapsed time of the movie, depends on runtime environment
    int elapsedTimeStandard = 0;    // standard elapsed time of movie, calculated by pre-defined FPS
    int elapsedTimeReference = 0;   // target display time for every frame, regardless of I/O time or something
    int frameDurationOffset = 0;    // average offset between ideal and actual time to display a frame in target FPS

    /* DEBUG ONLY PARAMETERS */
    int renderedFrameCount = 0;

    public VideoPlayer(Slider slider, JLabel canvas) {
        this.canvas = canvas;
        this.slider = slider;
        slider.addChangeListener(this);
        setAndNotifyStateChanged(State.Stopped);
    }

    private void setAndNotifyStateChanged(State newState) {
        currentState = newState;
        notifyStateChanged();
    }

    private void updateCanvas(long frameIndex) {
        BufferedImage newImage = MediaReader.getInstance().readRgbFile(videoFrames.get((int) frameIndex));
        canvas.setIcon(new ImageIcon(newImage));
    }

    @Override
    public void open(ArrayList<File> mediaSource) {
        synchronized (slider) {
            videoFrames = mediaSource;
            slider.reset(videoFrames);
            reset();
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void reset() {
        synchronized (slider) {
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
            updateCanvas(frameIndex);
        }
        if (slider != null) {
            synchronized (slider) {
                if (slider.getValue() != frameIndex) {
                    slider.setValue((int) frameIndex);
                }
            }
        }
    }

    @Override
    public void play() {
        synchronized (this) {
            if (currentState != State.Playing) {
                lastSyncTime = -1;
                this.notifyAll();
            }
        }
    }

    @Override
    public void pause() {
        currentState = State.Paused;
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
            int standard = getFrameDurationBiased(slider.getValue());
            synchronized (slider) {
                if (lastSyncTime > 0) {
                    updateCanvas(slider.getValue());
                    if (slider.getValue() < slider.getMaximum()) {
                        /* update synchronization parameters */
                        long now = System.currentTimeMillis();
                        long actual = now - lastSyncTime;
                        lastSyncTime = now;
                        elapsedTimeActual += actual;
                        elapsedTimeReference += standard;
                        elapsedTimeStandard += getFrameDurationStandard(slider.getValue());
                        renderedFrameCount++;
                        frameDurationOffset = (int) (elapsedTimeActual - elapsedTimeReference) / renderedFrameCount;
                        slider.forward();

                        /* calculate duration compensation for next frame */
                        int compensation = (int) (elapsedTimeActual - elapsedTimeStandard);
                        standard -= compensation;
                        if (standard <= 0) {
                            // delay is too much, discard next frame
                            System.out.printf("DELAYED! SKIP CURRENT FRAME: %d\n", slider.getValue());
                        } else {
                            float fps = 1000 / ((float) elapsedTimeActual / renderedFrameCount);
                            System.out.printf("Average FPS: %f, Accumulated Delay: %dms\n", fps, compensation);
                        }
                    } else setAndNotifyStateChanged(State.Paused);
                } else lastSyncTime = System.currentTimeMillis();
            }

            synchronized (this) {
                try {
                    if (currentState == State.Paused) {
                        setAndNotifyStateChanged(State.Paused);
                        wait();
                        setAndNotifyStateChanged(State.Playing);
                    }
                    else if (currentState == State.Playing) {
                        if (standard > 0) {
                            wait(standard);
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
        // 30 Frame Per Second
        return ((frameIndex % 3 == 1) ? 34 : 33) - frameDurationOffset;
    }

    private int getFrameDurationStandard(int frameIndex) {
        // 30 Frame Per Second
        return (frameIndex % 3 == 1) ? 34 : 33;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (currentState == State.Paused && canvas != null) {
            updateCanvas(slider.getValue());
        }
    }
}