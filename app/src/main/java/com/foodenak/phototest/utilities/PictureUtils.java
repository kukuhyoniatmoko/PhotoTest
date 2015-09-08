package com.foodenak.phototest.utilities;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PictureUtils {

    private static final String TAG = "PictureUtils";

    public static Bitmap getScaledBitmap(Activity a, Uri path) {
        BitmapFactory.Options options = createOptions(a, path);
        InputStream in = null;
        Bitmap bitmap = null;
        try {
            in = a.getContentResolver().openInputStream(path);
            bitmap = BitmapFactory.decodeStream(in, null, options);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "fail open stream", e);
        } finally {
            IOUtils.closeSilently(in);
        }
        return bitmap;
    }

    public static Bitmap getScaledBitmap(String path, int destWidth, int desHeight) {
        BitmapFactory.Options options = ScalingUtils.createOptions(path, destWidth, desHeight, ScalingUtils.CROP);
        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap getScaledBitmap(Context context, Uri uri, int destWidth, int desHeight) {
        BitmapFactory.Options options = ScalingUtils.createOptions(context, uri, destWidth, desHeight, ScalingUtils.CROP);
        InputStream in = null;
        Bitmap bitmap = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(in, null, options);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "fail open stream", e);
        } finally {
            IOUtils.closeSilently(in);
        }
        return bitmap;
    }

    public static Bitmap getCorrectedOrientationBitmap(String path, int destWidth, int desHeight) {
        Bitmap bitmap = getScaledBitmap(path, destWidth, desHeight);
        return fixOrientationBitmap(bitmap, path);
    }

    public static Bitmap getCorrectedOrientationBitmap(Context context, Uri uri, int destWidth, int desHeight) {
        Bitmap bitmap = getScaledBitmap(context, uri, destWidth, desHeight);
        return fixOrientationBitmap(bitmap, uri, context);
    }

    public static Bitmap getCorrectedOrientationBitmap(Activity a, Uri uri) {
        Bitmap bitmap = getScaledBitmap(a, uri);
        return fixOrientationBitmap(bitmap, uri, a);
    }

    private static Bitmap fixOrientationBitmap(Bitmap bitmap, Uri uri, Context context) {
        Bitmap oriented;
        oriented = ExifUtils.fixOrientation(bitmap, uri, context);
        if (bitmap != oriented) {
            bitmap.recycle();
        }
        return oriented;
    }

    private static Bitmap fixOrientationBitmap(Bitmap bitmap, String path) {
        Bitmap oriented;
        oriented = ExifUtils.fixOrientation(bitmap, path);
        if (bitmap != oriented) {
            bitmap.recycle();
        }
        return oriented;
    }

    private static BitmapFactory.Options createOptions(Activity a, Uri path) {
        Display display = a.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int destWidth = point.x;
        int destHeight = point.y;

        return ScalingUtils.createOptions(a, path, destWidth, destHeight, ScalingUtils.CROP);
    }

    public static String createImageFile(String name) {
        if (name == null || name.equals("")) {
            name = "foodenak";
        }
        String imageFileName = name + "-" + System.currentTimeMillis();
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File mFolder = new File(storageDir + "/FoodEnak/Temp");
        if (!mFolder.exists()) {
            mFolder.mkdirs();
        }
        File image = new File(mFolder, imageFileName + ".jpg");
        return image.getAbsolutePath();
    }

    public static String createCameraFile(String name) {
        if (name == null || name.equals("")) {
            name = "foodenak";
        }
        String imageFileName = name + "-" + System.currentTimeMillis();
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File mFolder = new File(storageDir + "/FoodEnak/Camera");
        if (!mFolder.exists()) {
            mFolder.mkdirs();
        }
        File image = new File(mFolder, imageFileName + ".jpg");
        return image.getAbsolutePath();
    }

    public static void cleanImageView(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        imageView.setImageDrawable(null);
    }

    public static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public static String decreaseSize(final Context context, final String path) throws IOException {
        final File file;
        Uri selectedImageUri = Uri.parse(path);
        String fileName = PictureUtils.createImageFile("decrease");
        IOUtils.copyUriToFile(selectedImageUri, fileName, context);
        file = new File(fileName);
        BufferedInputStream bitmapStream = new BufferedInputStream(new FileInputStream(file.getAbsoluteFile()));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferQualityOverSpeed = true;
        options.inScaled = false;
        options.inDither = false;
        Bitmap bitmap = BitmapFactory.decodeStream(bitmapStream, null, options);
        bitmapStream.close();
        Bitmap fixed = ExifUtils.fixOrientation(bitmap, selectedImageUri, context);
        if (bitmap != fixed) {
            bitmap.recycle();
        }
        Log.i(TAG, "bitmap dimension, width = " + fixed.getWidth() + ", height = " + fixed.getHeight());
        long size = file.length();
        Log.i(TAG, "actual size = " + size);
        int quality = 100;
        try {
            while (size > 300000 && quality > 0) {
                OutputStream stream = null;
                try {
                    stream = new BufferedOutputStream(new FileOutputStream(file));
                    fixed.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                    stream.flush();
                } finally {
                    IOUtils.closeSilently(stream);
                }
                size = new File(file.getAbsolutePath()).length();
                Log.i(TAG, "size after compression, quality = " + quality + ", size = " + size);
                quality -= 5;
            }
        } finally {
            fixed.recycle();
        }
        return file.getAbsolutePath();
    }
}

