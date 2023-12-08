package com.scyber.audioplayer;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import java.util.Comparator;
import scyber.util.AlphabeticalComparator;

public class SFile implements Comparable {

  @Override
  public int compareTo(Object p1) {
    return new AlphabeticalComparator<SFile>().compare((SFile)this, (SFile)p1);
  }


  private final String TAG = "SFile";

  public static final long serialVersionUID = 23444569L;
  private String time;
  public long id;
  public String artist;
  public String title;
  public  String album;
  public long duration;

  public SFile(long id, String artist, String title, String album, long duration) {
    this.id = id;
    this.artist = artist;
    this.title = title;
    this.album = album;
    this.duration = duration;
    getTime();
  }

  public String getTime() {
    if (time != null) {
      return time;
    }

    long durationInSeconds = duration / 1000;
    long min = (durationInSeconds / 60) % 60;
    long sec = durationInSeconds % 60;
    long hour = durationInSeconds / 3600;
    String minS = String.format("%02d", min);
    String secS = String.format("%02d", sec);
    String hourS = String.format("%02d", hour);
    String i = ((hour <= 0 ? "" : hourS + ":") + minS + ":" + secS);
    return (time = i);
  }

  @Override
  public String toString() {
    return title;
  }

  @Override
  public boolean equals(Object obj) {
    return toString().equalsIgnoreCase(obj.toString());
  }

  public Bitmap getBitmap(Context context) {
    Bitmap imgAlbum = null;
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    try {
      retriever.setDataSource(context, getURI());
      byte[] art = retriever.getEmbeddedPicture();
      if (art != null) {
        imgAlbum = BitmapFactory.decodeByteArray(art, 0, art.length);
      } else {
        imgAlbum = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_browser_audio_normal);
      }
    }
    catch (Exception e) {
      Log.e(TAG, e.getMessage());
    }
    finally {
      retriever.release();
      retriever = null;
    }
    return imgAlbum;
  }

  public Uri getURI() {
    return ContentUris.withAppendedId(
      android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
  }

  public long getId() {
    return id;
  }

  public String getArtist() {
    return artist;
  }

  public String getTitle() {
    return title;
  }

  public String getAlbum() {
    return album;
  }

  public long getDuration() {
    return duration;
  }
}
