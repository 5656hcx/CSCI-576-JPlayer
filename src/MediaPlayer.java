public abstract class MediaPlayer<T>  {

    private StateChangeListener listener;
    protected volatile State currentState;

    enum State { Playing, Paused, Stopped }

    abstract void open(T mediaSource);      // Open a media file
    abstract void close();  // Closed any resources
    abstract void play();   // Stopped -> Playing
    abstract void pause();  // Playing -> Paused
    abstract void stop();   //       * -> Stopped
    abstract void reset();  // use this function to restore runtime variables
    abstract void peek(long frameIndex);   // relocate current frame to frameIndex

    final void setStateChangeListener(StateChangeListener listener) {
        this.listener = listener;
    }

    final void notifyStateChanged() {
        if (listener != null) {
            listener.onPlayerStateChange(currentState);
        }
    }

    public interface StateChangeListener {
        void onPlayerStateChange(MediaPlayer.State state);
    }
}
