package com.jy.zxingdemo.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.jy.qrcode.camera.CameraManager;

import java.lang.reflect.Field;
import java.util.Hashtable;

public class Utils {

    private static Camera sCamera;
    public static final int SELECT_PIC_KITKAT = 100;
    public static final int SELECT_PIC = 101;


    /**
     * 打开闪光灯
     *
     * @param cameraManager
     */
    public static void openFlashLight(CameraManager cameraManager) {
        sCamera = cameraManager.getCamera();
        Camera.Parameters parameters = sCamera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        sCamera.setParameters(parameters);
        sCamera.startPreview();
    }

    /**
     * 关闭闪光灯
     */
    public static void closeFlashLight() {
        if (sCamera != null) {
            Camera.Parameters parameters = sCamera.getParameters();
            String flashMode = parameters.getFlashMode();
            if (flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                sCamera.setParameters(parameters);
                sCamera.startPreview();
            }

        }
    }

    /**
     * 打开相册
     *
     * @param activity
     */
    public static void openAlbum(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.startActivityForResult(intent, SELECT_PIC);
        } else {
            activity.startActivityForResult(intent, SELECT_PIC);
        }

    }

    /**
     * 尺寸计算
     *
     * @param context
     * @param dp
     * @return
     */
    public static int dp2px(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());

    }

    /**
     * 获取状态栏高度
     *
     * @param context
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 二维码图片识别
     *
     * @param path
     * @return
     */
    public static Result scaningImage(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        Result result = null;
        //DecodeHintType 和 EncodeHintType
        Hashtable<DecodeHintType, String> hints = new Hashtable<>();
        //设置二维码内容的编码方式
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        BitmapFactory.Options options = new BitmapFactory.Options();
        //获取新的大小
        options.inJustDecodeBounds = false;
        int sampleSize = (int) (options.outHeight / (float) 200);
        if (sampleSize <= 0) {
            sampleSize = 1;
        }
        options.inSampleSize = sampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
        Reader reader = new MultiFormatReader();
        try {
             result = reader.decode(bitmap1, hints);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


}
