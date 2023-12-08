package com.scyber.audioplayer.view;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.widget.SeekBar;

public class UpdateSeekBar implements Runnable {

  private final String TAG = "UpdateSeekBar";

  public void setSeekBar(SeekBar seekBar) {
    this.seekBar = seekBar;
    configSeekBar();
  }

  public SeekBar getSeekBar() {
    return seekBar;
  }

  public void start() {
    if (running && called) {
        setMax();
        return;
    };
    called = true;
    if (player == null || !player.isPlaying()) return;
    new Thread(this).start();
    running = true;
  }

  @Override
  public void run() {
    while (player != null && player.isPlaying()) {
      try {

        handler.post(new Runnable(){

            @Override
            public void run() {
              seekBar.setProgress(player.getCurrentPosition());
            }
          });
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Log.e(TAG, e.getMessage());
      }
    }
    running = false;
    return;
  }

  void configSeekBar() {
    seekBar.setFadingEdgeLength(1);
    seekBar.setOnSeekBarChangeListener(
      new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar p1, int p2, boolean p3) {}

        @Override
        public void onStartTrackingTouch(SeekBar p1) {}

        @Override
        public void onStopTrackingTouch(final SeekBar p1) {
          UpdateSeekBar.this.player.seekTo(p1.getProgress());
        }
      });
    setMax();
  }

  private void setMax() {
      seekBar.setMax(player != null ? player.getDuration() : 0);
  }

  private volatile boolean running;
  private volatile boolean called;
  SeekBar seekBar;
  private Handler handler;
  private MediaPlayer player;

  public UpdateSeekBar(SeekBar seekBar, Handler handler, MediaPlayer player) {
    this.seekBar = seekBar;
    this.handler = handler;
    this.player = player;
    configSeekBar();
  }


  public boolean isRunning() {
    return running;
  }
}
