package com.scyber.audioplayer.view;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import com.scyber.audioplayer.SFile;

public class UpdateTimeView implements Runnable {

  private final String TAG = "UpdateTimeView";

  public boolean isRunning() {
    return running;
  }

  @Override
  public void run() {

    while (player != null && player.isPlaying()) {

      handler.post(new Runnable(){

          public void run() {
            int tempSec = UpdateTimeView.this.player.getCurrentPosition() / 1000;
            int hour = tempSec / 3600;
            int min = (tempSec / 60) % 60;
            int sec = tempSec % 60;
            String minS = String.format("%02d", min);
            String secS = String.format("%02d", sec);
            String hourS = String.format("%02d", hour);
            String i = ((hour <= 0 ? "" : hourS + ":") + minS + ":" + secS);

            UpdateTimeView.this.lengthView.setText(i);
          }
        });

      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {
        Log.e(TAG, e.getMessage());
      }
    }
    running = false;
    return;
  }

  private volatile boolean running;
  private volatile boolean called;
  private Handler handler;
  TextView songTitle;
  TextView timeView;
  TextView lengthView;
  private MediaPlayer player;

  public void setViews(TextView songTitle, TextView timeView, TextView lengthView) {
    this.songTitle = songTitle;
    this.timeView = timeView;
    this.lengthView = lengthView;
  }

  public UpdateTimeView(Handler handler, TextView songTitle, TextView timeView, TextView lengthView, MediaPlayer player) {
    this.handler = handler;
    this.songTitle = songTitle;
    this.timeView = timeView;
    this.lengthView = lengthView;
    this.player = player;
  }

  public void start(SFile f) {
    if (f == null) return;
    timeView.setText(f.getTime());
    songTitle.setText(f.title);
    if (running && called) return;
    called = true;
    if (player == null || !player.isPlaying()) return;
    new Thread(this).start();
    running = true;
  }
}
