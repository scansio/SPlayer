package com.scyber.audioplayer;

import java.io.*;
import java.util.*;

public class Save
{
  private static String parent;
  private static String store = ".media";

  private List<SFile> playlist;

  public Save(List<SFile> playlist, String parentPath)
  {
    this.playlist = playlist;
    parent = parentPath;
  }

  public List<SFile> getPlaylist()
  {
	return playlist;
  }

  public void setPlaylist(List<SFile> sFiles)
  {
	this.playlist = sFiles;
  }

  public void setParent(String parent)
  {
    this.parent = parent;
  }

  public String getParent()
  {
    return parent;
  }

  public synchronized void save()
  {
    try
	{
      FileOutputStream fout = new FileOutputStream(getFile());
      ObjectOutputStream out = new ObjectOutputStream(fout);
      out.writeObject(playlist);
      out.close();
      fout.close();
    }
	catch (IOException e1)
	{
      System.out.println(e1.getMessage());
    }
  }

  public String getFile()
  {
    return parent.concat("/").concat(store);
  }

  public synchronized static Save getSaved(String path) throws IOException, ClassNotFoundException
  {
    try
	{
	  parent = path;
	  FileInputStream fin = new FileInputStream(parent.concat("/").concat(store));
	  ObjectInputStream in = new ObjectInputStream(fin);
	  Save save = null;
	  Object o = in.readObject();
	  if (o instanceof ArrayList)
	  {
		save = new Save((ArrayList) o, parent);
	  }
	  else
	  {
		in.close();
		throw new IOException("Not an instance of Arraylist");
	  }
	  in.close();
	  fin.close();
	  fin = null;
	  in = null;
	  return save;
	}
	catch (IOException e)
	{}
	catch (ClassNotFoundException e)
	{}
	return null;
  }
}
