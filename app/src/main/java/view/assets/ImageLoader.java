package view.assets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.DrawableRes;

public class ImageLoader {
    private final Context context;

    public ImageLoader(Context context) {
        this.context = context;
    }

    public Bitmap loadBitmap(@DrawableRes int resourceId) {
        return BitmapFactory.decodeResource(context.getResources(), resourceId);
    }
}