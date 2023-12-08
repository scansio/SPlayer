package com.scyber.audioplayer;

import android.util.Log;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;
import java.io.File;
import java.io.IOException;
import com.scyber.audioplayer.view.SaveModel;

public class StateSaver {
    final String TAG = "StateSaver";

    private String directory;

    public StateSaver(String dir) {
        this.directory = dir;
    }



    public void save(final SaveModel saveModel) {
        try {
            new Thread(new Runnable(){
                    @Override
                    public void run() {
                        FileOutputStream fout = null;
                        ObjectOutputStream out = null;

                        try {
                            File file = new File(directory + "/.state");
                            file.createNewFile();
                            fout = new FileOutputStream(file);
                            out = new ObjectOutputStream(fout);
                            out.writeInt(saveModel.getCurrent() | 0);
                            out.writeInt(saveModel.getPosition());
                            out.writeInt(saveModel.getRepeat() | 0);
                            out.writeBoolean(saveModel.getShuffle() | false);

                        }
                        catch (Exception e) {
                            Log.e("Controller: ", e.getMessage());
                        }
                        finally {
                            try {
                                if (out != null) out.close();
                                if (fout != null) fout.close();
                            }
                            catch (IOException e) {}
                        }
                    }
                }).start();
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public SaveModel initState() {
        SaveModel saveModel = new SaveModel();
        try {
            FileInputStream fin = null;
            ObjectInputStream in = null;
            try {
                if (!new File(directory + "/.state").exists()) {
                    saveModel.setCurrent(0)
                        .setPosition(0)
                        .setRepeat(0)
                        .setShuffle(false);
                    return saveModel;
                };
                fin = new FileInputStream(directory + "/.state");
                in = new ObjectInputStream(fin);
                saveModel.setCurrent(in.readInt())
                    .setPosition(in.readInt())
                    .setRepeat(in.readInt())
                    .setShuffle(in.readBoolean());
            }
            catch (Exception e) {
                new File(directory + "/.state").delete();
                Log.e(TAG, e.getMessage());

                saveModel.setCurrent(0)
                    .setPosition(0)
                    .setRepeat(0)
                    .setShuffle(false);
            }
            finally {
                try {
                    if (in != null) in.close();
                    if (fin != null) fin.close();
                }
                catch (IOException e) {}
            }
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return saveModel;
    }
}
