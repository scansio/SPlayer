package com.scyber.audioplayer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;
import java.io.File;
import java.util.Random;
import java.util.Vector;
import com.scyber.audioplayer.view.SaveModel;

public class Controller implements MediaPlayer.OnCompletionListener,
MediaPlayer.OnErrorListener,
MediaPlayer.OnPreparedListener {

  private StateSaver ss;

  private final String TAG = "APController";

  public MediaPlayer mPlayer = null;

  private SFile playingItem;

  public void save() {
    ss.save(new SaveModel().
            setCurrent(current)
            .setPosition(mPlayer == null ? 0 : mPlayer.getCurrentPosition())
            .setRepeat(repeat)
            .setShuffle(shuffle));
  }

  void createMediaPlayerIfNeeded() {
    if (mPlayer == null) {
      mPlayer = new MediaPlayer();

      // Make sure the media player will acquire a wake-lock while playing. If we don't do
      // that, the CPU might go to sleep while the song is playing, causing playback to stop.
      //
      // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
      // permission in AndroidManifest.xml.
      mPlayer.setWakeMode(ms.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
      //mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      // we want the media player to notify us when it's ready preparing, and when it's done
      // playing:
      /*mPlayer.setAudioAttributes(
       new AudioAttributes.Builder()
       .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
       .setUsage(AudioAttributes.USAGE_MEDIA)
       .build());*/
      mPlayer.setOnPreparedListener(this);
      mPlayer.setOnCompletionListener(this);
      mPlayer.setOnErrorListener(this);
    } else
      mPlayer.reset();
    position = 0;
  }

  void configAndStartMediaPlayer() {
    if (ms.mAudioFocus == MusicService.AudioFocus.NoFocusNoDuck) {
      // If we don't have audio focus and can't duck, we have to pause, even if mState
      // is State.Playing. But we stay in the Playing state so that we know we have to resume
      // playback once we get the focus back.
      if (mPlayer.isPlaying()) mPlayer.pause();
      return;
    } else if (ms.mAudioFocus == MusicService.AudioFocus.NoFocusCanDuck)
      mPlayer.setVolume(ms.DUCK_VOLUME, ms.DUCK_VOLUME);  // we'll be relatively quiet
    else
      mPlayer.setVolume(1.0f, 1.0f); // we can be loud

    if (!mPlayer.isPlaying()) mPlayer.start();
  }

  public void control(int pos) {
    play(pos, PLAY);
  }

  public boolean onError(MediaPlayer playerl, int a, int b) {
    next(false);
    return false;
  }

  @Override
  public void onPrepared(MediaPlayer p1) {
    if (position > 0) mPlayer.seekTo(position);
    play();
  }

  public MusicService ms;
  public int repeat;
  public int position;
  public int current;
  public Vector<Integer> previousHistory = new Vector<Integer>();
  public Vector<Integer> nextHistory = new Vector<Integer>();
  public static final short PLAY = 0, NEXT = 1, PREVIOUS = 3;
  public static final int REPEAT_ONE = 1, REPEAT_ALL = 2, REPEAT_NONE = 0;
  public boolean shuffle;

  private Context ctx;

  public Controller(MusicService ms, Context ctx) {
    this.ms = ms;
    this.ctx = ctx;
    File[] mediaDir = ctx.getExternalMediaDirs();
    String directory = mediaDir[0].getAbsolutePath();
    this.ss = new StateSaver(directory);
    SaveModel sm = ss.initState();
    current = sm.getCurrent();
    position = sm.getPosition();
    repeat = sm.getRepeat();
    shuffle = sm.getShuffle();
  }

  public void control(String action) {
    try {
      if (action != null) {

        if (action.equals(MusicService.ACTION_TOGGLE_PLAYBACK)) {
          play();
        } else if (action.equals(MusicService.ACTION_NEXT)) {
          next(false);
        } else if (action.equals(MusicService.ACTION_STOP)) {
          try {
            relaxResources(true);
            ms.giveUpAudioFocus();

            // Tell any remote controls that our playback state is 'paused'.
            if (ms.mRemoteControlClientCompat != null) {
              ms.mRemoteControlClientCompat
                .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            // service is no longer necessary. Will be started again if needed.
            System.gc();
            ms.stopSelf();
          }
          catch (Exception e) {

          }

        } else if (action.equals(MusicService.ACTION_PREVIOUS)) {
          previous(false);
        } else if (action.equals(MusicService.ACTION_SHUFFLE)) {
          ms.vc.toggleShuffle();
          ms.vc.initNotification();
        } else if (action.equals(MusicService.ACTION_REPEAT)) {
          ms.vc.toggleRepeat();
          ms.vc.initNotification();
        }}
    }
    catch (Exception e) {
      Log.e(ms.TAG, e.getMessage());
    }
  }

  public SFile previous(boolean returnSFile) {
    if (!previousHistory.isEmpty()) {
      int p = previousHistory.remove(previousHistory.size() - 1);
      if (returnSFile) return ms.playlist.get(p); else play(p, PREVIOUS);
    } else {
      int p = current - 1 < 0 ? ms.playlist.size() - 1 : current - 1;
      if (returnSFile) return ms.playlist.get(p); else play(p, PREVIOUS);
    }
    return null;
  }

  public SFile next(boolean returnSFile) {
    if (!nextHistory.isEmpty()) {
      int n = nextHistory.remove(nextHistory.size() - 1);
      if (returnSFile) return ms.playlist.get(n); else play(n, NEXT);
    } else {
      int n = current + 1 > ms.playlist.size() - 1 ? 0 : current + 1;
      if (returnSFile) return ms.playlist.get(n); else play(shuffle ? (Math.abs(new Random().nextInt() % ms.playlist.size())) : n, NEXT);
    }
    return null;
  }

  public void initPlayer(SFile sFile, Uri source) {
    try {
      relaxResources(false);
      ms.tryToGetAudioFocus();
      createMediaPlayerIfNeeded();
      System.gc();
      SFile playingItem = sFile != null ? sFile : null;
      if (source != null) {
        mPlayer.setDataSource(ctx, source);
      } else {
        if (ms.playlist != null) {
          if (ms.playlist.size() > current) {
            playingItem = getCurrentSFile();
          } else {
            playingItem = ms.playlist.get(current);
          }
          if (playingItem != null) {
            mPlayer.setDataSource(ctx, playingItem.getURI());
          }
        }
      }

      if (playingItem != null && source == null) {
        MediaButtonHelper.registerMediaButtonEventReceiverCompat(
          ms.mAudioManager, ms.mMediaButtonReceiverComponent);

        // Use the remote control APIs (if available) to set the playback state

        if (ms.mRemoteControlClientCompat == null) {
          Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
          intent.setComponent(ms.mMediaButtonReceiverComponent);
          ms.mRemoteControlClientCompat = new RemoteControlClientCompat(
            PendingIntent.getBroadcast(ms /*context*/,
                                       0 /*requestCode, ignored*/, intent /*intent*/, 0));
          RemoteControlHelper.registerRemoteControlClient(ms.mAudioManager,                                              ms.mRemoteControlClientCompat);
        }
        ms.mRemoteControlClientCompat.setPlaybackState(
          RemoteControlClient.PLAYSTATE_PLAYING);

        ms.mRemoteControlClientCompat.setTransportControlFlags(
          RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
          RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
          RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
          RemoteControlClient.FLAG_KEY_MEDIA_STOP);

        // Update the remote controls
        ms.mRemoteControlClientCompat.editMetadata(true)
          .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, playingItem.getArtist())
          .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, playingItem.getAlbum())
          .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, playingItem.getTitle())
          .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                   playingItem.getDuration())
          // TODO: fetch real item artwork
          .putBitmap(
          RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
          ms.mDummyAlbumArt)
          .apply();
          this.playingItem = playingItem;
      }
      mPlayer.prepareAsync();
      Log.i(TAG, Long.toString(Runtime.getRuntime().freeMemory()));
      System.out.println("Free memory: " + Long.toString(Runtime.getRuntime().freeMemory()));
    }
    catch (Exception e) {
      Log.e("Controller", e.getMessage());
    }
  }

  public void play() {
    try {
      if (mPlayer == null) return;
      if (!mPlayer.isPlaying()) {
        mPlayer.start();
        if (ms.mRemoteControlClientCompat != null) {
          ms.mRemoteControlClientCompat
            .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
      } else {
        mPlayer.pause();
        relaxResources(false);
        if (ms.mRemoteControlClientCompat != null) {
          ms.mRemoteControlClientCompat
            .setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
      }
      ms.vc.updateWidget();
    }
    catch (Exception e) {

    }
  }

  void relaxResources(boolean releaseMediaPlayer) {
    if (releaseMediaPlayer && mPlayer != null) {
      ms.stopForeground(true);
      mPlayer.reset();
      mPlayer.release();
      mPlayer = null;
    }
  }
  
  public SFile getPlayingItem(){
    return playingItem;
  }

  public void play(int index, short flag) {
    try {
      save();
      switch (flag) {
        case PLAY:
        case NEXT:
          {
            if (previousHistory.contains(current)) previousHistory.removeElement(current);
            previousHistory.add(current);
            break;
          }

        case PREVIOUS:
          {
            if (nextHistory.contains(current)) nextHistory.removeElement(current);
            nextHistory.add(current);
            break;
          }
      }

      current = index;

      initPlayer(ms.playlist.get(index), null);

    }
    catch (Exception e) {
      Log.e("Sayer", e.getMessage());
    }
  }

  public void onCompletion(MediaPlayer p) {
    try {
      switch (repeat) {
        case REPEAT_ONE:
          play(current, NEXT);
          break;
        case REPEAT_ALL:
          next(false);
          break;
        case REPEAT_NONE:
          if (current != ms.playlist.size() - 1) next(false);
          break;
      }
    }
    catch (Exception e) {
      Log.e(TAG, e.getMessage());
      mPlayer.release();
    }
  }

  public SFile getCurrentSFile() {
    if (ms.playlist == null) return null;
    if (ms.playlist.size() != 0 & !(ms.playlist.size() < current)) {
      return ms.playlist.get(current);
    }
    current = 0;
    return ms.playlist.size() > 1 ? ms.playlist.get(0) : null;
  }

  public int getCurrentIndex() {
    if (ms.playlist.size() != 0 & !(ms.playlist.size() < current)) {
      return current;
    }
    current = 0;
    return current;
  }
}
