package com.scyber.audioplayer;

import android.content.*;

public class IntentStarter {

  public static void start(Context ctx, int position) {
    Intent intent = new Intent(ctx, MusicService.class);
    intent.putExtra("position", position);
    ctx.startService(intent);
  }

  public static void start(Context context, String action) {
    Intent intent = new Intent(context, MusicService.class);
    intent.setAction(action);
    context.startService(intent);
  }
}
