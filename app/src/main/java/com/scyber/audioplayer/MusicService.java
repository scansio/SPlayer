package com.scyber.audioplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.scyber.audioplayer.view.VController;
import java.io.IOException;
import java.util.List;

public class MusicService extends Service implements MusicFocusable,
PrepareMusicRetrieverTask.MusicRetrieverPreparedListener {

  final static String TAG = "SPlayer";

  public static final String ACTION_TOGGLE_PLAYBACK =
  "com.scyber.audioplayer.action.TOGGLE_PLAYBACK";
  public static final String ACTION_PLAY = "com.scyber.audioplayer.action.PLAY";
  public static final String ACTION_PAUSE = "com.scyber.audioplayer.action.PAUSE";
  public static final String ACTION_STOP = "com.scyber.audioplayer.action.STOP";
  public static final String ACTION_NEXT = "com.scyber.audioplayer.action.SKIP";
  public static final String ACTION_PREVIOUS = "com.scyber.audioplayer.action.REWIND";
  public static final String ACTION_URL = "com.scyber.audioplayer.action.URL";

  // The volume we set the media player to when we lose audio focus, but are allowed to reduce
  // the volume instead of stopping playback.
  public static final float DUCK_VOLUME = 0.1f;

  // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
  // If not available, this will be null. Always check for null before using!
  AudioFocusHelper mAudioFocusHelper = null;

  // indicates the state our service:
  enum State {
    Retrieving, // the MediaRetriever is retrieving music
    Stopped,    // media player is stopped and not prepared to play
    Preparing,  // media player is preparing...
    Playing,    // playback active (media player ready!). (but the media player may actually be
    // paused in this state if we don't have audio focus. But we stay in this state
    // so that we know we have to resume playback once we get focus back)
    Paused      // playback paused (media player ready!)
    };

  State mState = State.Retrieving;

  // if in Retrieving mode, this flag indicates whether we should start playing immediately
  // when we are ready or not.
  boolean mStartPlayingAfterRetrieve = false;

  enum PauseReason {
    UserRequest,  // paused by user request
    FocusLoss,    // paused because of audio focus loss
  };

  // why did we pause? (only relevant if mState == State.Paused)
  PauseReason mPauseReason = PauseReason.UserRequest;

  // do we have audio focus?
  enum AudioFocus {
    NoFocusNoDuck,    // we don't have audio focus, and can't duck
    NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
    Focused           // we have full audio focus
    }
  AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

  MusicRetriever mRetriever;

  // our RemoteControlClient object, which will use remote control APIs available in
  // SDK level >= 14, if they're available.
  RemoteControlClientCompat mRemoteControlClientCompat;

  // Dummy album art we will pass to the remote control (if the APIs are available).
  Bitmap mDummyAlbumArt;

  // The component name of MusicIntentReceiver, for use with media button and remote control
  // APIs
  ComponentName mMediaButtonReceiverComponent;

  AudioManager mAudioManager;
  NotificationManager mNotificationManager;

  @Override
  public void onLowMemory() {
    //stopSelf();
    System.gc();
    super.onLowMemory();
  }

  @Override
  public void onCreate() {
    Log.i(TAG, "debug: Creating service");
    controller = new Controller(this, this);
    vc = new VController(controller, this);

    mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

    // Create the retriever and start an asynchronous task that will prepare it.
    mRetriever = new MusicRetriever(getContentResolver());
    (new PrepareMusicRetrieverTask(mRetriever, this)).execute();

    // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
    if (android.os.Build.VERSION.SDK_INT >= 8)
      mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
    else
      mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

    mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.ic_browser_audio_normal);

    mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);
  }

  /**
   * Called when we receive an Intent. When we receive an intent sent to us via startService(),
   * this is the method that gets called. So here we react appropriately depending on the
   * Intent's action, which specifies what is being requested of us.
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    try {
      int pos = intent.getIntExtra("position", -1);
      if (pos != -1) {
        controller.control(pos);
      } else {
        String action = intent.getAction();
        if (action == null) {
          return START_NOT_STICKY;
        }
        controller.control(action);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    } 
    return START_NOT_STICKY;
  }

  void giveUpAudioFocus() {
    if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
        && mAudioFocusHelper.abandonFocus())
      mAudioFocus = AudioFocus.NoFocusNoDuck;
  }

  void tryToGetAudioFocus() {
    if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
        && mAudioFocusHelper.requestFocus())
      mAudioFocus = AudioFocus.Focused;
  }

  public void onGainedAudioFocus() {
//    Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
    mAudioFocus = AudioFocus.Focused;

    // restart media player with new focus settings
    if (controller.mPlayer != null && !controller.mPlayer.isPlaying())
      controller.configAndStartMediaPlayer();
  }

  public void onLostAudioFocus(boolean canDuck) {
//    Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" :
//                   "no duck"), Toast.LENGTH_SHORT).show();
    mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

    // start/restart/pause media player with new focus settings
    if (controller.mPlayer != null && controller.mPlayer.isPlaying())
      controller.configAndStartMediaPlayer();
  }

  public void onMusicRetrieverPrepared() {
    playlist = mRetriever.sFiles;
    controller.initPlayer(null, null);
    vc.setView(view);
  }


  @Override
  public void onDestroy() {
    controller.relaxResources(true);
    giveUpAudioFocus();
    controller.save();
  }

  private final Binding binding = new Binding();

  public class Binding extends Binder {
    public MusicService getBinder() {
      return MusicService.this;
    }
  }

  @Override
  public IBinder onBind(Intent p1) {
    return binding;
  }

  public static final String ACTION_REPEAT = "com.scyber.audioplayer.action.REPEAT";
  public static final String ACTION_SHUFFLE = "com.scyber.audioplayer.action.SHUFFLE";

  public Controller controller;

  public List<SFile> playlist;

  public VController vc;

  private View view;

  public void setView(View view) {
    this.view = view;

    if (playlist != null && playlist.size() > 1) {
      vc.setView(view);
    }
  }

  public View getView() {
    return view;
  }
}
