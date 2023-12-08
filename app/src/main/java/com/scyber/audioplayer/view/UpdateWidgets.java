package com.scyber.audioplayer.view;

import android.os.AsyncTask;
import com.scyber.audioplayer.MusicService;
import com.scyber.audioplayer.SFile;

public class UpdateWidgets implements Runnable 
{

  @Override
  public void run() {
    ms.vc.updateWidget();
  }
  
  private MusicService ms;

  public UpdateWidgets(MusicService ms)
  {
	this.ms = ms;
  }

}
