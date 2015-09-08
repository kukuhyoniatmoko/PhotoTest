package com.foodenak.phototest.utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by ITP on 3/1/2015.
 */
public class ExifUtils {

    private static final String TAG = "ExifUtils";

    public static Bitmap fixOrientation(Bitmap bitmap, String pathForExif) {
        try {
            ExifInterface exifInterface = new ExifInterface(pathForExif);
            return getCorrectedBitmap(bitmap, exifInterface);
        } catch (IOException e) {
            Log.e(TAG, "fail fix orientation", e);
        }
        return bitmap;
    }

    public static Bitmap fixOrientation(Bitmap bitmap, Uri uri, Context context) {
        File tempFile = null;
        try {
            String fileName = PictureUtils.createImageFile("temporary");
            IOUtils.copyUriToFile(uri, fileName, context);
            tempFile = new File(fileName);
            return fixOrientation(bitmap, tempFile.getAbsolutePath());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    private static Bitmap getCorrectedBitmap(Bitmap bitmap, ExifInterface exifInterface) {
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Matrix matrix = ExifUtils.getMatrix(orientation);
        if (matrix == null) {
            return bitmap;
        }
        try {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            return bitmap;
        }
    }

    /**
     * Convert orientation to matrix for fixing image rotation
     *
     * @param orientation on ExifInterface
     * @return Matrix if orientation need to be fixed, null if not
     */
    public static Matrix getMatrix(int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                return matrix;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                return matrix;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postRotate(180);
                matrix.postScale(-1, 1);
                return matrix;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.postRotate(90);
                matrix.postScale(-1, 1);
                return matrix;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                return matrix;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.postRotate(-90);
                matrix.postScale(-1, 1);
                return matrix;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(-90);
                return matrix;
            default:
                return null;
        }
    }
}
