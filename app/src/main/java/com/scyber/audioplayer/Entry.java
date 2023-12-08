package com.scyber.audioplayer;

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

public class Entry extends Activity {
  private ServiceConnection connection =
  new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName p1, IBinder p2) {
      MusicService ms = ((MusicService.Binding) p2).getBinder();
      View v = findViewById(R.id.entryView);
      ms.setView(v);
    }

    @Override
    public void onServiceDisconnected(ComponentName p1) {
Entry.this.finish();
    }
  };

  private Intent i;



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(1);
    setContentView(R.layout.entry_layout);
    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED
        & checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(
        new String[] {
          Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        },
        234907439);
    } else {
        start();
    }
  }

  private void start() {
      this.i = new Intent(this, MusicService.class);
      startService(i);
      bindService(i, connection, MusicService.BIND_AUTO_CREATE);
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  public void onLowMemory() {
    //unbindService(connection);
    //stopService(this.i);
    super.onLowMemory();
    System.gc();
    
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unbindService(connection);
  }

  @Override
  public void onRequestPermissionsResult(
    int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (grantResults[0] != RESULT_OK || grantResults[1] != RESULT_OK) finish();
    start();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
      case KeyEvent.KEYCODE_HEADSETHOOK:
        IntentStarter.start(this, MusicService.ACTION_TOGGLE_PLAYBACK);
        return true;
    }
    return super.onKeyDown(keyCode, event);
  }
}
