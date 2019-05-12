package com.example.battleships;

import android.content.Context;
import android.media.MediaPlayer;

import java.util.ArrayList;
import java.util.Random;

public class SoundManager {

    private ArrayList<MediaPlayer> media;
    private Context context;
    private Random rnd;

    public SoundManager(Context context) {
        this.media = new ArrayList<>();
        this.context = context;
        this.rnd = new Random();
    }

    public void loadCannonSounds() {
        media.add(MediaPlayer.create(context, R.raw.cannon1));
        media.add(MediaPlayer.create(context, R.raw.cannon2));
        media.add(MediaPlayer.create(context, R.raw.cannon3));
        media.add(MediaPlayer.create(context, R.raw.cannon4));
        media.add(MediaPlayer.create(context, R.raw.cannon5));
    }

    public void playRandomSound() {;
        media.get(rnd.nextInt(media.size())).start();
    }
}
