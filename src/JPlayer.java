import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class JPlayer extends JPanel implements MediaPlayer.PlaybackStateChangeListener {

    private final JButton button_play;
    private final JLabel video;
    private final Slider slider;

    private ArrayList<File> videoFrames;
    private final AudioPlayer audioPlayer;
    private final VideoPlayer videoPlayer;

    public JPlayer() {
        setLayout(new BorderLayout());

        video = new JLabel("\\(^o^)/", JLabel.CENTER);
        video.setAlignmentX(CENTER_ALIGNMENT);
        add(video, BorderLayout.CENTER);

        JLabel status = new JLabel("Playlist is empty", JLabel.CENTER);
        slider = new Slider(status, "Now playing the %dth frame");

        audioPlayer = new AudioPlayer(slider);
        audioPlayer.setPlaybackStateChange(this);
        videoPlayer = new VideoPlayer(slider, video);
        videoPlayer.setPlaybackStateChange(this);

        JButton button_load = new JButton("load");
        button_load.addActionListener(new FileSelector("Select video directory...", JPlayer.this) {
            @Override
            public void onFileSelected(String path) {
                MediaReader reader = MediaReader.getInstance();
                videoFrames = reader.readVideoDir(path);
                if (!videoFrames.isEmpty()) {
                    audioPlayer.open(path);
                    audioPlayer.setVideoFrameLength(videoFrames.size());
                    video.setText(null);
                    video.setIcon(new ImageIcon(reader.readRgbFile(videoFrames.get(0))));
                    videoPlayer.open(videoFrames);
                    slider.reset(videoFrames);
                }
            }
        });

        button_play = new JButton("play");
        button_play.addActionListener(e -> {
            // for now, do nothing in Stopped state
            switch (videoPlayer.currentState) {
                case Paused:
                    // tell playback thread to resume
                    videoPlayer.play();
                    audioPlayer.peek(slider.getValue());
                    audioPlayer.play();
                    break;
            case Playing:
                    // tell playback thread to pause
                    videoPlayer.pause();
                    audioPlayer.pause();
                    break;
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
    public void onPlaybackStateChange(MediaPlayer.State state) {
        switch (state) {
            case Paused:
                button_play.setText("play");
                break;
            // System.out.println("Video Playback has paused");
            case Playing:
                button_play.setText("pause");
                break;
            // System.out.println("Video Playback has resumed");
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
