package com.foodenak.phototest.utilities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.OutputStream;

/**
 * Created by ITP on 9/8/2015.
 */
public class CameraUtils {

    private static final String TAG = "CameraUtils";

    public static void broadcastScanFile(Uri uri, Context context) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(uri);
        context.sendBroadcast(intent);
    }

    public static void scaleImage(Uri src, Uri dst, Context context) {
        OutputStream out = null;
        Bitmap srcBitmap = null;
        Bitmap scaledBitmap = null;
        Bitmap orientedBitmap = null;
        try {
            ContentResolver cr = context.getContentResolver();
            srcBitmap = ScalingUtils.decodeHQ(context, src, 640, 640);
            Rect dstRect = calculateDstRec(srcBitmap, 640);
            Rect srcRect = calculateSrcRec(srcBitmap);
            scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(scaledBitmap);
            canvas.drawBitmap(srcBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));
            if (srcBitmap != scaledBitmap) {
                srcBitmap.recycle();
            }
            orientedBitmap = ExifUtils.fixOrientation(scaledBitmap, src, context);
            if (scaledBitmap != orientedBitmap) {
                scaledBitmap.recycle();
            }
            out = cr.openOutputStream(dst);
            orientedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Fail scale image", e);
        } finally {
            if (srcBitmap != null) {
                srcBitmap.recycle();
            }
            if (scaledBitmap != null) {
                scaledBitmap.recycle();
            }
            if (orientedBitmap != null) {
                orientedBitmap.recycle();
            }
            IOUtils.closeSilently(out);
        }
    }

    public static void scaleAndCropImage(Uri src, Uri dst, Context context) {
        OutputStream out = null;
        Bitmap srcBitmap = null;
        Bitmap scaledBitmap = null;
        Bitmap orientedBitmap = null;
        try {
            ContentResolver cr = context.getContentResolver();
            srcBitmap = ScalingUtils.decodeHQ(context, src, 640, 640);
            Rect dstRect = calculateCropDstRect(srcBitmap, 640);
            Rect srcRect = calculateCropSrcRect(srcBitmap);
            scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(scaledBitmap);
            canvas.drawBitmap(srcBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));
            if (srcBitmap != scaledBitmap) {
                srcBitmap.recycle();
            }
            orientedBitmap = ExifUtils.fixOrientation(scaledBitmap, src, context);
            if (scaledBitmap != orientedBitmap) {
                scaledBitmap.recycle();
            }
            out = cr.openOutputStream(dst);
            orientedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Fail scale image", e);
        } finally {
            if (srcBitmap != null) {
                srcBitmap.recycle();
            }
            if (scaledBitmap != null) {
                scaledBitmap.recycle();
            }
            if (orientedBitmap != null) {
                orientedBitmap.recycle();
            }
            IOUtils.closeSilently(out);
        }
    }

    static Rect calculateCropSrcRect(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width < height) {
            return new Rect(0, 0, width, width);
        } else {
            return new Rect(0, 0, height, height);
        }
    }

    static Rect calculateCropDstRect(Bitmap bitmap, int dstWidth) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (dstWidth > width || dstWidth > height) {
            if (width > height) {
                return new Rect(0, 0, height, height);
            } else {
                return new Rect(0, 0, width, width);
            }
        }
        return new Rect(0, 0, dstWidth, dstWidth);
    }

    static Rect calculateSrcRec(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        return new Rect(0, 0, width, height);
    }

    static Rect calculateDstRec(Bitmap bitmap, int dstWidth) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (dstWidth > width || dstWidth > height) {
            return new Rect(0, 0, width, height);
        }
        Rect rect = new Rect();
        rect.top = 0;
        rect.left = 0;
        float divider;
        if (width > height) {
            divider = (float) height / (float) dstWidth;
        } else {
            divider = (float) width / (float) dstWidth;
        }
        rect.right = Math.round((float) width / divider);
        rect.bottom = Math.round((float) height / divider);
        return rect;
    }
}
