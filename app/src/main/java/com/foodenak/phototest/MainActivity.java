package com.foodenak.phototest;

import com.foodenak.phototest.utilities.CameraUtils;
import com.foodenak.phototest.utilities.ExifUtils;
import com.foodenak.phototest.utilities.IOUtils;
import com.foodenak.phototest.utilities.ScalingUtils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements Observer<String> {

    private static final String TAG = "MainActivity";

    @Bind(R.id.progress_bar)
    View mProgressBar;

    int mMaxSize = 300000;

    int mWidth = 640;

    int mDecrement = 1;

    private static String createImageFile(String folder, String name) {
//        String imageFileName = "foodenak" + "-" + name;
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File mFolder = new File(storageDir + "/FoodEnak/" + folder);
        if (!mFolder.exists()) {
            mFolder.mkdirs();
        }
        File image = new File(mFolder, name + ".jpg");
        return image.getAbsolutePath();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnTextChanged(R.id.width_edit_text)
    void onWidthEditTextChanged(CharSequence s, int start, int before, int count) {
        if (TextUtils.isEmpty(s)) {
            mWidth = 0;
        } else {
            try {
                mWidth = Integer.valueOf(s.toString());
            } catch (Exception e) {
                mWidth = 0;
            }
        }
    }

    @OnTextChanged(R.id.width_edit_text)
    void onMaxSizeEditTextChanged(CharSequence s, int start, int before, int count) {
        if (TextUtils.isEmpty(s)) {
            mMaxSize = 0;
        } else {
            try {
                mMaxSize = Integer.valueOf(s.toString());
            } catch (Exception e) {
                mMaxSize = 0;
            }
        }
    }

    @OnTextChanged(R.id.width_edit_text)
    void onDecrementEditTextChanged(CharSequence s, int start, int before, int count) {
        if (TextUtils.isEmpty(s)) {
            mDecrement = 1;
        } else {
            try {
                mDecrement = Integer.valueOf(s.toString());
                if (mDecrement == 0) {
                    mDecrement = 1;
                }
            } catch (Exception e) {
                mDecrement = 1;
            }
        }
    }

    @OnClick(R.id.process_button)
    void onProcessButtonClick(View view) {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            return;

        }
        mProgressBar.setVisibility(View.VISIBLE);
        process2();
    }

    private void process2() {
        String[] imageFileNames = new String[10];
        for (int i = 0; i < 10; i++) {
            imageFileNames[i] = "image_" + i + ".jpg";
        }
        Observable.from(imageFileNames)
                .subscribeOn(Schedulers.io())
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        String fileName = copy(s);
                        CameraUtils.broadcastScanFile(Uri.fromFile(new File(fileName)), getApplicationContext());
                        return fileName;
                    }
                })
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        String fileName = scale1(s);
                        CameraUtils.broadcastScanFile(Uri.fromFile(new File(fileName)), getApplicationContext());
                        return fileName;
                    }
                })
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        String fileName = scale2(s);
                        CameraUtils.broadcastScanFile(Uri.fromFile(new File(fileName)), getApplicationContext());
                        return fileName;
                    }
                })
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        String fileName = crop(s);
                        CameraUtils.broadcastScanFile(Uri.fromFile(new File(fileName)), getApplicationContext());
                        return fileName;
                    }
                })
                .doOnNext(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        String name = new File(s).getName().replace(".jpg", "");
                        String fileName = createImageFile("Compress JPEG", name);
                        decreaseSize(s, fileName, Bitmap.CompressFormat.JPEG);
                        CameraUtils.broadcastScanFile(Uri.fromFile(new File(fileName)), getApplicationContext());
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this);
    }

    @Override
    public void onCompleted() {
        Log.i(TAG, "process complete");
        mProgressBar.setVisibility(View.GONE);
        Toast.makeText(getApplication(), R.string.completed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(Throwable e) {
        Log.e(TAG, "error happened", e);
        mProgressBar.setVisibility(View.GONE);
        Toast.makeText(getApplication(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNext(String s) {

    }

    void decreaseSize(String srcFile, String dstFile, Bitmap.CompressFormat format) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(dstFile);
            IOUtils.copy(in, out);
        } catch (IOException e) {
            Log.e(TAG, "Fail copy file " + srcFile, e);
        } finally {
            IOUtils.closeSilently(in);
            IOUtils.closeSilently(out);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferQualityOverSpeed = true;
        options.inScaled = false;
        options.inDither = false;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(dstFile, options);
            long size = new File(dstFile).length();
            int quality = 100;
            while (size > mMaxSize && quality > 0) {
                OutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(dstFile);
                    bitmap.compress(format, quality, outputStream);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Fail compress " + srcFile, e);
                } finally {
                    IOUtils.closeSilently(outputStream);
                }
                size = new File(dstFile).length();
                Log.i(TAG, "compress with quality = " + quality + ", size = " + size + ", file " + dstFile);
                quality -= mDecrement;
            }
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    Rect calculateCropSrcRect(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width < height) {
            return new Rect(0, 0, width, width);
        } else {
            return new Rect(0, 0, height, height);
        }
    }

    Rect calculateCropDstRect(Bitmap bitmap, int dstWidth) {
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

    Rect calculateSrcRec(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        return new Rect(0, 0, width, height);
    }

    Rect calculateDstRec(Bitmap bitmap, int dstWidth) {
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

    String copy(String s) {
        AssetManager manager = getAssets();
        InputStream in = null;
        OutputStream out = null;
        String name = s.replace(".jpg", "");
        String fileName = createImageFile("Original", name);
        try {
            in = manager.open(s);
            out = new FileOutputStream(fileName);
            IOUtils.copy(in, out);
        } catch (IOException e) {
            Log.e(TAG, "Fail copy " + s, e);
        } finally {
            IOUtils.closeSilently(in);
            IOUtils.closeSilently(out);
        }
        return fileName;
    }

    String scale1(String s) {
        Bitmap bitmap = null;
        OutputStream out = null;
        String fileName = null;
        Bitmap orientedBitmap = null;
        try {
            bitmap = ScalingUtils.decodeHQ(s, mWidth, mWidth);
            orientedBitmap = ExifUtils.fixOrientation(bitmap, s);
            if (bitmap != orientedBitmap) {
                bitmap.recycle();
            }
            String name = new File(s).getName().replace(".jpg", "");
            fileName = createImageFile("Scale 1", name + "-" + mWidth);
            out = new FileOutputStream(fileName);
            orientedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            Log.e(TAG, "Fail scale 1 " + s, e);
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
            if (orientedBitmap != null) {
                orientedBitmap.recycle();
            }
            IOUtils.closeSilently(out);
        }
        return fileName;
    }

    String scale2(String s) {
        Bitmap bitmap = null;
        OutputStream out = null;
        String fileName = null;
        Bitmap scaledBitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferQualityOverSpeed = true;
            bitmap = BitmapFactory.decodeFile(s, options);
            Rect dstRect = calculateDstRec(bitmap, mWidth);
            Rect srcRect = calculateSrcRec(bitmap);
            scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(scaledBitmap);
            canvas.drawBitmap(bitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));
            if (bitmap != scaledBitmap) {
                bitmap.recycle();
            }
            String name = new File(s).getName().replace(".jpg", "");
            fileName = createImageFile("Scale 2", name);
            out = new FileOutputStream(fileName);
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            Log.e(TAG, "Fail scale 2 " + s, e);
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
            if (scaledBitmap != null) {
                scaledBitmap.recycle();
            }
            IOUtils.closeSilently(out);
        }
        return fileName;
    }

    String crop(String s) {
        Bitmap bitmap = null;
        Bitmap cropped = null;
        OutputStream stream = null;
        String name = new File(s).getName().replace(".jpg", "");
        String fileName = createImageFile("Crop", name);
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferQualityOverSpeed = true;
            bitmap = BitmapFactory.decodeFile(s, options);
            Rect dstRect = calculateCropDstRect(bitmap, mWidth);
            Rect srcRect = calculateCropSrcRect(bitmap);
            cropped = Bitmap.createBitmap(dstRect.right, dstRect.bottom, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(cropped);
            canvas.drawBitmap(bitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));
            if (bitmap != cropped) {
                bitmap.recycle();
            }
            stream = new FileOutputStream(fileName);
            cropped.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Fail crop " + s, e);
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
            if (cropped != null) {
                cropped.recycle();
            }
            IOUtils.closeSilently(stream);
        }
        return fileName;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
