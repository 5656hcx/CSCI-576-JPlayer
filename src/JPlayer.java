import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class JPlayer extends JPanel implements MediaPlayer.StateChangeListener {

    private final JButton button_play;
    private final JLabel video;
    private final Slider slider;

    private final AudioPlayer audioPlayer;
    private final VideoPlayer videoPlayer;

    private boolean isPlayerPaused = true;

    public JPlayer() {
        setLayout(new BorderLayout());

        video = new JLabel("\\(^o^)/", JLabel.CENTER);
        video.setAlignmentX(CENTER_ALIGNMENT);
        add(video, BorderLayout.CENTER);

        JLabel status = new JLabel("Playlist is empty", JLabel.CENTER);
        slider = new Slider(status, "Now playing the %dth frame");

        audioPlayer = new AudioPlayer(slider);
        audioPlayer.setStateChangeListener(this);
        videoPlayer = new VideoPlayer(slider, video);
        videoPlayer.setStateChangeListener(this);

        JButton button_load = new JButton("load");
        button_load.addActionListener(new FileSelector("Select video directory...", JPlayer.this) {
            @Override
            public void onFileSelected(File file) {
                MediaReader reader = MediaReader.getInstance();
                ArrayList<File> videoFrames = reader.readVideoDir(file.getPath());
                if (!videoFrames.isEmpty()) {
                    audioPlayer.open(reader.readWavFile(file.getPath()));
                    audioPlayer.setVideoFrameLength(videoFrames.size());
                    video.setText(null);
                    videoPlayer.open(videoFrames);
                    slider.reset(videoFrames);
                }
            }
        });

        button_play = new JButton("play");
        button_play.addActionListener(e -> {
            // for now, do nothing in Stopped state
            if (isPlayerPaused) {
                videoPlayer.play();
            }
            else {
                videoPlayer.pause();
            }
        });

        JPanel buttons = new JPanel(new GridLayout(1, 2));
        buttons.add(button_load);
        buttons.add(button_play);

        JPanel widget = new JPanel(new GridLayout(3, 1));
        widget.add(status);
        widget.add(slider);
        widget.add(buttons);

        add(widget, BorderLayout.SOUTH);
    }

    @Override
    public void onPlayerStateChange(MediaPlayer.State state) {
        switch (state) {
            case Paused:
                button_play.setText("play");
                isPlayerPaused = true;
                audioPlayer.pause();
                break;
            case Playing:
                button_play.setText("pause");
                isPlayerPaused = false;
                audioPlayer.peek(slider.getValue());
                audioPlayer.play();
                break;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Media Player");
        frame.add(new JPlayer());
        frame.setSize(360, 440);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
