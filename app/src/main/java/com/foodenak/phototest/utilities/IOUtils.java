package com.foodenak.phototest.utilities;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ITP on 9/4/2015.
 */
public class IOUtils {

    private static final String TAG = "IOUtils";

    public static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                Log.e(TAG, "fail close closeable", e);
            }
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();
    }

    public static void copyUriToFile(Uri in, String out, Context context) {
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(in);
            outputStream = new FileOutputStream(new File(out));
            copy(inputStream, outputStream);
        } catch (IOException e) {
            Log.e(TAG, "Fail copy uri to file", e);
        } finally {
            closeSilently(outputStream);
            closeSilently(inputStream);
        }
    }
}
