package com.scyber.audioplayer.view;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import com.scyber.audioplayer.Controller;
import com.scyber.audioplayer.Entry;
import com.scyber.audioplayer.IntentStarter;
import com.scyber.audioplayer.MusicService;
import com.scyber.audioplayer.R;
import com.scyber.audioplayer.SFile;

public class VController implements OnClickListener, TextWatcher {
  
  ImageButton play;
  private SeekBar seekBar;
  private TextView time;
  private TextView length;
  private TextView songTitle;
  private ImageButton shuffle;
  private ImageButton repeat;
  private Controller controller;
  private ImageButton previous;
  private ImageView stop;
  private final int REPEAT_ONE = 1, REPEAT_ALL = 2, REPEAT_NONE = 0;
  private ImageButton next;
  private NotificationManager notificationManager;
  final String TAG = "VController";
  private ListView lsv;
  private MusicService ctx;

  private View v;

  private Notification.Builder builder;

  private UpdateSeekBar seekBarUpdater;

  private UpdateTimeView timeViewUpdater;

  private Handler handler;

  private VAdapter lsa;

  private ImageView search;

  private EditText searchView;

  private ImageView filter_btn;

  public VController(Controller controller, MusicService ctx) {
    this.controller = controller;
    this.ctx = ctx;
    this.handler = new Handler(ctx.getMainLooper());
  }
  
  public void onTextChanged(CharSequence s, int start, int before, int count) {}

  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

  public void afterTextChanged(Editable e) {
    String s = e.toString();
    if (TextUtils.isEmpty(s)) {
      lsa.filter("");
      lsv.clearTextFilter();
    } else {
      lsa.filter(s);
      lsv.setVisibility(View.VISIBLE);
    }
  }

  public void setView(View view) {
    this.v = view;

    try {
      seekBar = v.findViewById(R.id.timeline);
      time = v.findViewById(R.id.timeView);
      length = v.findViewById(R.id.lengthView);
      play = v.findViewById(R.id.play);
      shuffle = v.findViewById(R.id.shuffle);
      repeat = v.findViewById(R.id.repeat);
      songTitle = v.findViewById(R.id.song_title);

      previous = v.findViewById(R.id.previous);
      previous.setOnClickListener(this);

      play.setOnClickListener(this);

      next = v.findViewById(R.id.next);
      next.setOnClickListener(this);

      stop = v.findViewById(R.id.stop);
      stop.setOnClickListener(this);
      
      search = v.findViewById(R.id.search);
      search.setOnClickListener(this);
      
      shuffle.setOnClickListener(this);

      repeat.setOnClickListener(this);
      searchView = v.findViewById(R.id.search_view);
      searchView.addTextChangedListener(this);
      filter_btn = v.findViewById(R.id.filter_btn);
      filter_btn.setOnClickListener(this);
      lsv = v.findViewById(R.id.lsv);
      lsa = new VAdapter(ctx, controller.ms.playlist);
      lsv.setAdapter(lsa);
      updateWidget();
    }
    catch (Exception e) {
      Log.e(TAG, e.getMessage());
    }
  }

  public void setEnable(boolean enable) {
    play.setEnabled(enable);
    shuffle.setEnabled(enable);
    repeat.setEnabled(enable);
    previous.setEnabled(enable);
    stop.setEnabled(enable);
    next.setEnabled(enable);
  }

  public boolean toggleShuffle() {
    return (controller.shuffle = !controller.shuffle);
  }
  
  public void toggleSearchView(){
    View parent = (View)searchView.getParent();
    if (parent.getVisibility() == View.VISIBLE){
      parent.setVisibility(View.GONE);
      searchView.setText("");
      search.setImageResource(R.drawable.ic_menu_search);
    } else {
      parent.setVisibility(View.VISIBLE);
      search.setImageResource(R.drawable.ic_search);
    }
  }

  public int toggleRepeat() {
    return controller.repeat =
      (controller.repeat == REPEAT_NONE ? REPEAT_ONE : controller.repeat == REPEAT_ONE ? REPEAT_ALL : REPEAT_NONE);
  }

  public void update(RemoteViews notificationView) {
    SFile f = controller.getCurrentSFile();
    if (controller.shuffle) {
      shuffle.setImageResource(R.drawable.ic_shuffle_pressed);
      notificationView.setImageViewResource(R.id.shuffleN, (R.drawable.ic_shuffle_pressed));
    } else {
      shuffle.setImageResource(R.drawable.ic_shuffle_normal);
      notificationView.setImageViewResource(R.id.shuffleN, (R.drawable.ic_shuffle_normal));
    }

    if (controller.mPlayer != null && controller.mPlayer.isPlaying()) {
      play.setImageResource(R.drawable.ic_pause_normal);
      notificationView.setImageViewResource(R.id.playN, (R.drawable.ic_pause_normal));
    } else {
      play.setImageResource(R.drawable.ic_play_normal);
      notificationView.setImageViewResource(R.id.playN, (R.drawable.ic_play_normal));
    }

    if (notificationView != null && f != null) {
      notificationView.setImageViewBitmap(R.id.cover, f.getBitmap(ctx));
      notificationView.setTextViewText(R.id.fileName, f.title);
    }

    try {
      switch (controller.repeat) {
        case REPEAT_ALL:
          repeat.setImageResource(R.drawable.ic_repeat_pressed);
          notificationView.setImageViewResource(R.id.repeatN, (R.drawable.ic_repeat_pressed));
          break;

        case REPEAT_ONE:
          repeat.setImageResource(R.drawable.ic_repeat_one_o);
          notificationView.setImageViewResource(R.id.repeatN, (R.drawable.ic_repeat_one_o));
          break;

        case REPEAT_NONE:
          repeat.setImageResource(R.drawable.ic_repeat_normal);
          notificationView.setImageViewResource(R.id.repeatN, (R.drawable.ic_repeat_normal));
          break;
      }
    }
    catch (Exception e) {
      Log.e(TAG, e.getMessage());
    }
  }

  public Bitmap getBitmap(int res) {
    try {
      Bitmap b = BitmapFactory.decodeResource(ctx.getResources(), res);
      return b;
    }
    catch (Exception e) {
      Log.e(TAG, e.getMessage());
    }
    return null;
  }

  public void initNotification() {
    try {
      RemoteViews notificationView;
      notificationView = new RemoteViews(ctx.getPackageName(), R.layout.notification_expanded);
      Intent ic = new Intent(ctx, MusicService.class);

      notificationView.setOnClickPendingIntent(
        R.id.shuffleN,
        PendingIntent.getService(
          ctx, 0, ic.setAction(MusicService.ACTION_SHUFFLE), PendingIntent.FLAG_UPDATE_CURRENT));

      notificationView.setOnClickPendingIntent(
        R.id.stopN,
        PendingIntent.getService(
          ctx, 0, ic.setAction(MusicService.ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT));

      notificationView.setOnClickPendingIntent(
        R.id.playN,
        PendingIntent.getService(
          ctx, 0, ic.setAction(MusicService.ACTION_TOGGLE_PLAYBACK), PendingIntent.FLAG_UPDATE_CURRENT));

      notificationView.setOnClickPendingIntent(
        R.id.nextN,
        PendingIntent.getService(
          ctx, 0, ic.setAction(MusicService.ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT));

      notificationView.setOnClickPendingIntent(
        R.id.previousN,
        PendingIntent.getService(
          ctx, 0, ic.setAction(MusicService.ACTION_PREVIOUS), PendingIntent.FLAG_UPDATE_CURRENT));

      notificationView.setOnClickPendingIntent(
        R.id.repeatN,
        PendingIntent.getService(
          ctx, 0, ic.setAction(MusicService.ACTION_REPEAT), PendingIntent.FLAG_UPDATE_CURRENT));

      update(notificationView);
      if (notificationManager == null) notificationManager = (NotificationManager) ctx.getSystemService(MusicService.NOTIFICATION_SERVICE);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel notificationChannel =
          new NotificationChannel("scyber", "APlayer", NotificationManager.IMPORTANCE_NONE);
        notificationChannel.setBypassDnd(false);
        //notificationChannel.setAllowBubbles(true);
        notificationChannel.setLockscreenVisibility(View.VISIBLE);
        notificationChannel.setShowBadge(true);
        notificationChannel.setSound(null, null);
        notificationChannel.setVibrationPattern(null);
        notificationManager.createNotificationChannel(notificationChannel);
      }
      if (builder == null) builder = new Notification.Builder(ctx);
      builder.setTicker("APlayer")
        .setSmallIcon(R.drawable.ic_play_pressed)
        .setOnlyAlertOnce(true)
        .setCustomBigContentView(notificationView)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(
        PendingIntent.getActivity(
          ctx,
          12,
          new Intent(ctx, Entry.class),
          PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        builder.setAutoCancel(false)
          .setChannelId("scyber")
          .setColorized(true);
      }      
      ctx.startForeground(45563335, builder.build());
    }
    catch (Exception e) {
      Log.e("Error: ", e.getMessage());
    }
  }

  public View getView() {
    return v;
  }

  @Override
  public void onClick(View target) {
    try {
      if (target == play) 
        IntentStarter.start(ctx, MusicService.ACTION_TOGGLE_PLAYBACK);
      else if (target == next)
        IntentStarter.start(ctx, MusicService.ACTION_NEXT);
      else if (target == previous)
        IntentStarter.start(ctx, MusicService.ACTION_PREVIOUS);	
      else if (target == stop)
        IntentStarter.start(ctx, MusicService.ACTION_STOP);
      else if (target == shuffle)
        IntentStarter.start(ctx, MusicService.ACTION_SHUFFLE);
      else if (target == repeat)
        IntentStarter.start(ctx, MusicService.ACTION_REPEAT);
      else if (target == search)
        toggleSearchView();
      else if (target == filter_btn)
        selectFilter();
      
    }
    catch (Exception e) {
      Log.e("Entry", e.getMessage());
    }
  }
  
  public void selectFilter(){
    PopupMenu popup = new PopupMenu(ctx, filter_btn);
    popup.getMenuInflater()
      .inflate(R.menu.ex_popup_menu, popup.getMenu());

    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {

          if (item.getItemId() == R.id.add) {

          } else if (item.getItemId() == R.id.rem) {

          }

          return true;
        }
      });
    popup.show();
  }

  public void initUTSVS() {
    if (seekBarUpdater == null) seekBarUpdater = new UpdateSeekBar(seekBar, handler, controller.mPlayer);
    if (seekBarUpdater.seekBar != seekBar) {
      seekBarUpdater.setSeekBar(seekBar);
    }
    if (timeViewUpdater == null) timeViewUpdater = new UpdateTimeView(handler, songTitle, time, length, controller.mPlayer);
    if (timeViewUpdater.songTitle != songTitle || timeViewUpdater.timeView != time || timeViewUpdater.lengthView != length) {
      timeViewUpdater.setViews(songTitle, time, length);
    }
  }

  public void utsvs(SFile f) {
    initUTSVS();
    seekBarUpdater.start();
    timeViewUpdater.start(f);
  }

  public void utsvs() {
    utsvs(controller.ms.playlist.get(controller.current));
  }

  public void updateWidget() {
    SFile f = controller.getCurrentSFile();
    initNotification();
    utsvs(f);
    scrollListView();
  }

  public void scrollListView() {
    int pos = controller.getCurrentIndex();
    lsa.notifyDataSetChanged();
    if (!(pos > lsv.getFirstVisiblePosition() && pos < lsv.getLastVisiblePosition())) {
      lsv.smoothScrollToPosition(pos);
    }
  }
}
