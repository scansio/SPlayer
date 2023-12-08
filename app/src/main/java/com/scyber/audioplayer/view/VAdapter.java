package com.scyber.audioplayer.view;

import android.annotation.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import com.scyber.audioplayer.*;
import java.util.*;
import android.widget.AdapterView.OnItemClickListener;

public class VAdapter extends BaseAdapter {

  private MusicService ctx;
  private LayoutInflater inflater;
  private List<SFile> playlist;
  private ArrayList<SFile> arrayList;

  VAdapter(MusicService ctx, List<SFile> playlist) {
    this.ctx = ctx;
    this.playlist = playlist;
    inflater = LayoutInflater.from(ctx);
    this.arrayList = new ArrayList<>();
    this.arrayList.addAll(playlist);
  }

  @Override
  public int getCount() {
    return playlist.size();
  }

  @Override
  public SFile getItem(int i) {
    return playlist.get(i);
  }

  @Override
  public long getItemId(int i) {
    return playlist.get(i).id;
  }

  @SuppressLint("InflateParams")
  @Override
  public View getView(final int position, View view, ViewGroup parent) {
    if (view == null) view = inflater.inflate(R.layout.list_item, null);
    final ViewHolder holder = getViewHolder(view);        
    view.setTag(String.valueOf(position));

    SFile item = playlist.get(position);

    holder.filelist_name.setText(item.title);
    holder.filelist_time.setText(item.getTime());
    holder.filelist_num.setText((position + 1) + ". ");
    SFile playing = ctx.controller.getPlayingItem();
    if (item.equals(playing)) {
      holder.currentSongBtnView.setVisibility(View.VISIBLE);
    } else {
      holder.currentSongBtnView.setVisibility(View.INVISIBLE);
    }
    view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          IntentStarter.start(ctx, position);
        }
      });

    return view;
  }

  //filter
  void filter(String charText) {
    charText = charText.toLowerCase(Locale.getDefault());
    playlist.clear();
    if (charText.length() == 0) {
      playlist.addAll(arrayList);
    } else {
      for (SFile SFile : arrayList) {
        if (SFile.title.toLowerCase(Locale.getDefault())
            .contains(charText)) {
          playlist.add(SFile);
        }
      }
    }
    notifyDataSetChanged();
  }

  public int getCurrentIndex() {
    return ctx.controller.getCurrentIndex();
  }

  public ViewHolder getViewHolder(View view) {
    ViewHolder holder = new ViewHolder();
    holder.filelist_name = view.findViewById(R.id.filelist_name);
    holder.filelist_time = view.findViewById(R.id.filelist_time);
    holder.filelist_num = view.findViewById(R.id.filelist_num);
    holder.currentSongBtnView = view.findViewById(R.id.playing);
    return holder;
  }

}
