package com.scyber.audioplayer.view;

import java.util.List;

public final class SaveModel {
    private int current;
    private int position;
    private int repeat;
    private boolean shuffle;

    public SaveModel setCurrent(int current) {
        this.current = current;
        return this;
    }

    public int getCurrent() {
        return current;
    }

    public SaveModel setPosition(int position) {
        this.position = position;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public SaveModel setRepeat(int repeat) {
        this.repeat = repeat;
        return this;
    }

    public int getRepeat() {
        return repeat;
    }

    public SaveModel setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
        return this;
    }

    public boolean getShuffle() {
        return shuffle;
    }
}
