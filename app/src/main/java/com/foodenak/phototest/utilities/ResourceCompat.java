package com.foodenak.phototest.utilities;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;

/**
 * Created by ITP on 8/18/2015.
 */
public class ResourceCompat {

    public static int getColor(Resources resources, @ColorRes int colorRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return resources.getColor(colorRes, null);
        }
        //noinspection deprecation
        return resources.getColor(colorRes);
    }

    public static Drawable getDrawable(Resources resources, int drawableRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return resources.getDrawable(drawableRes, null);
        } else {
            //noinspection deprecation
            return resources.getDrawable(drawableRes);
        }
    }
}
