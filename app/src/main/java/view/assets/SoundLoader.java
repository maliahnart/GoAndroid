package view.assets;

import android.content.Context;
import android.media.SoundPool;
import androidx.annotation.RawRes;

public class SoundLoader {
    private final Context context;
    private final SoundPool soundPool;
    private int soundId;

    public SoundLoader(Context context) {
        this.context = context;
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .build();
    }

    public void loadSound(@RawRes int resourceId) {
        soundId = soundPool.load(context, resourceId, 1);
    }

    public void playSound() {
        soundPool.play(soundId, 1f, 1f, 0, 0, 1f);
    }

    public void release() {
        soundPool.release();
    }
}