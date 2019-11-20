package com.jy.zxingdemo;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.Result;
import com.jy.qrcode.CaptureCallback;
import com.jy.qrcode.camera.CameraManager;
import com.jy.qrcode.decode.DecodeThread;
import com.jy.qrcode.utils.CaptureActivityHandler;
import com.jy.qrcode.utils.InactivityTimer;
import com.jy.zxingdemo.utils.BeepManager;
import com.jy.zxingdemo.utils.ImageUtil;
import com.jy.zxingdemo.utils.Utils;


public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback, CaptureCallback {


    private Context mContext;
    private Activity mActivity;
    private TextView tvLight;
    private SurfaceView scanPreview;//SurfaceView控件
    private RelativeLayout scanContainer;//布局容器
    private RelativeLayout scanCropView;//布局中的扫描框

    private boolean isPause;//是否暂停
    private CaptureActivityHandler handler;
    private Rect mCropRect;//矩形
    private CameraManager mCameraManager;//相机管理类
    private InactivityTimer mInactivityTimer;//计时器
    private BeepManager mBeepManager;//蜂鸣器
    private ObjectAnimator mObjectAnimator;//扫描动画
    private boolean isHasSurface;//SurfaceView是否存在

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //保持屏幕常亮
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture);
        mContext = this;
        mActivity = this;
        init();
        initScan();
    }

    private void initScan() {
        scanPreview = findViewById(R.id.capture_preview);
        scanContainer = findViewById(R.id.capture_container);
        scanCropView = findViewById(R.id.capture_crop_view);
        ImageView scanLine = findViewById(R.id.capture_scan_line);

        //扫描动画,上下扫描
        float curTranslationY = scanLine.getTranslationY();
        mObjectAnimator = ObjectAnimator.ofFloat(scanLine, "translationY",
                curTranslationY, Utils.dp2px(this, 170));
        mObjectAnimator.setDuration(4000);
        mObjectAnimator.setInterpolator(new LinearInterpolator());
        mObjectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mObjectAnimator.setRepeatMode(ValueAnimator.RESTART);

    }

    @Override
    protected void onPause() {
        pauseScan();
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
        startScan();
    }

    /**
     * 开始扫描
     */
    private void startScan() {
        mInactivityTimer = new InactivityTimer(this);
        mBeepManager = new BeepManager(this);
        if (isPause) {
            // 如果暂停,扫描动画暂停
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mObjectAnimator.resume();
                isPause = false;
            }
        } else {
            //扫描动画开始
            mObjectAnimator.start();
        }
        //初始化相机管理
        mCameraManager = new CameraManager(this);
        handler = null;//重置handler
        if (isHasSurface) {
            initCamera(scanPreview.getHolder());
        } else {
            // 等待surfaceCreated来初始化相机
            scanPreview.getHolder().addCallback(this);
        }
        //开启计时器
        mInactivityTimer.onResume();
    }

    /**
     * 初始化相机
     *
     * @param holder
     */
    private void initCamera(SurfaceHolder holder) {
        if (holder == null) {
            throw new IllegalStateException("SurfaceHolder is null");
        }
        if (mCameraManager.isOpen()) {
            Log.e("zc===", "Camera is open");
            return;
        }
        try {
            mCameraManager.openDriver(holder);
            if (handler == null) {
                handler = new CaptureActivityHandler(this, mCameraManager, DecodeThread.ALL_MODE);
            }
            initCrop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化矩形裁剪
     */
    private void initCrop() {
        int cameraWidth = mCameraManager.getCameraResolution().y;
        int cameraHeight = mCameraManager.getCameraResolution().x;

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - Utils.getStatusBarHeight(this);

        /**获取截取的宽高*/
        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    /**
     * 暂停扫描
     */
    private void pauseScan() {
        if (handler != null) {
            // handler退出同步并置空
            handler.quitSynchronously();
            handler = null;
        }
        // 计时器暂停
        mInactivityTimer.onPause();
        //关闭蜂鸣器
        mBeepManager.close();
        //关闭相机管理器的驱动
        mCameraManager.closeDriver();
        if (!isHasSurface) {
            // remove 等待
            scanPreview.getHolder().removeCallback(this);
        }
        //动画暂停
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mObjectAnimator.pause();
        }
        isPause = true;
    }


    private void init() {
        tvLight = findViewById(R.id.tv_light);
        ToggleButton tbLight = findViewById(R.id.tb_light);
        tbLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tvLight.setText("关闭闪光灯");
                    Utils.openFlashLight(mCameraManager);
                } else {
                    tvLight.setText("打开闪光灯");
                    Utils.closeFlashLight();
                }
            }
        });

        findViewById(R.id.albumLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //打开相册,做权限判断
                Utils.openAlbum(mActivity);
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e("zc====", "SurfaceHolder is null");
        }
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    @Override
    public Rect getCropRect() {
        return mCropRect;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Utils.SELECT_PIC && data != null){
            Uri uri = data.getData();
            String path = ImageUtil.getImageAbsolutePath(this, uri);
            Result result = Utils.scaningImage(path);
            if (result != null){
                String text = result.getText();
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "未识别到二维码", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void handleDecode(Result result, Bundle bundle) {
        //扫码成功之后的回调方法
        mInactivityTimer.onActivity();
        //播放蜂鸣声
        mBeepManager.playBeepSoundAndVibrate();
        //将扫码的结构返回
        if (result != null) {
            String text = result.getText();
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onDestroy() {
        //关闭计时器
        mInactivityTimer.shutdown();
        if (mObjectAnimator != null) {
            mObjectAnimator.end();
        }
        super.onDestroy();
        mActivity = null;
        mContext = null;
    }
}
