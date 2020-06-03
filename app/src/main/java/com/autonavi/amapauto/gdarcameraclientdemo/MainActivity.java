package com.autonavi.amapauto.gdarcameraclientdemo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.autonavi.amapauto.gdarcameraservice.camera.GDCamera;
import com.autonavi.amapauto.gdarcameraservice.camera.IArCamera;
import com.autonavi.amapauto.gdarcameraservice.constant.gd.ArCameraParam;
import com.autonavi.amapauto.gdarcameraservice.utils.ImageSaverUtils;
import com.autonavi.amapauto.utils.Logger;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    private ArCameraParam autoArCameraParam;

    private IArCamera arCamera;

    String serviceName = GDCamera.DEFAULT_SERVICE_PACKAGE_NAME;

    String serviceAction = GDCamera.DEFAULT_SERVICE_ACTION;

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 从共享内存读取的数据
     */
    private byte[] readData;

    /**
     * 线程池大小
     */
    private static final int THREAD_NUM = 1;

    /**
     * 线程池服务, 管理线程
     */
    private ExecutorService fixedThreadPool;

    /**
     * 预览图片
     */
    private SurfaceHolder surfaceHolder;

    /**
     * 用于在主线程中更新ui
     */
    private ImageView imageView;
    private TextView receivedDataTV;
    private ClientHandler clientHandler;

    private boolean shoot = false;

    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;
    private Bitmap bitmap;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public Bitmap nv21ToBitmap() {
        if(readData !=null) {
            int imageWidth = arCamera != null ? arCamera.getRealImageSize().getWidth() : 1280;
            int imageHeight = arCamera != null ? arCamera.getRealImageSize().getHeight() : 720;
            if (yuvType == null) {
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(readData.length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(imageWidth).setY(imageHeight);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }
            in.copyFrom(readData);
            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);
            bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
            out.copyTo(bitmap);
            return bitmap;
        }else{
            Logger.d(TAG, "nv21ToBitmap mReadData==null");
            return null;
        }
    }

    class ClientHandler extends Handler {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public void handleMessage(Message msg) {

            String str = DATE_FORMAT.format(new Date()) + ", " + (readData !=null?(readData.length + ", " + readData[0]):(0 + ", null"));

            if(receivedDataTV!=null) {
                receivedDataTV.setText("FPS:" + mFps + ";   " + str);
            }

            Bitmap bitmap = nv21ToBitmap();
            if(bitmap!=null) {
                if(imageView!=null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private void initGDCamera(){
        EditText serviceNameView = findViewById(R.id.define_service_name);
        EditText serviceActionView = findViewById(R.id.define_service_action);
        if(serviceNameView!=null){
            if(!TextUtils.isEmpty(serviceNameView.getText().toString().trim()) && !getResources().getString(R.string.app_service_name).equals(serviceNameView.getText().toString().trim())){
                serviceName = serviceNameView.getText().toString().trim();
            }
        }

        if(serviceActionView!=null){
            if(!TextUtils.isEmpty(serviceActionView.getText().toString().trim()) && !getResources().getString(R.string.app_service_action).equals(serviceActionView.getText().toString().trim())){
                serviceAction = serviceActionView.getText().toString().trim();
            }
        }
        Logger.d(TAG, "onCreate() serviceAction = {?} serviceName = {?}",serviceAction,serviceName);
        arCamera = new GDCamera(serviceAction,serviceName);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initGDCamera();

        SurfaceView mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(callback);

        imageView = findViewById(R.id.image_view);
        receivedDataTV = findViewById(R.id.received_data);

        findViewById(R.id.init).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                initGDCamera();
                initCamera();
            }
        });

        findViewById(R.id.open).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                isMyStart = false;
                closeCamera();
            }
        });
        findViewById(R.id.start_read_thread).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                isMyStart = true;
                if(arCamera!=null && arCamera.isCameraOpened()){
                    runReadThread();
                }else{
                    Toast.makeText(MainActivity.this,"还未收到摄像头打开完成的消息，暂时无法启动读取图像数据的线程！",Toast.LENGTH_LONG).show();
                }

            }
        });

        findViewById(R.id.tv_shoot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shoot = true;
            }
        });

        findViewById(R.id.cloea_app).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                isMyStart = true;
                finish();
            }
        });

        clientHandler = new ClientHandler();
        fixedThreadPool = Executors.newFixedThreadPool(THREAD_NUM);

        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        autoArCameraParam =  new ArCameraParam(1280,720,ArCameraParam.IMAGE_TYPE_YUV_420_888,0);

        //第一种，不设置外部预览的surface的情况
        arCamera.initCamera(autoArCameraParam);
        //初始化完成后才可以点打开
        Toast.makeText(MainActivity.this,"可以点开始了",Toast.LENGTH_LONG).show();
    }

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Logger.d(TAG, "in surfaceCreated");
            surfaceHolder = holder;

            //第二种，设置外部预览的surface的情况
            /*if(arCamera!=null){
                arCamera.setSurface(surfaceHolder.getSurface());
            }
            //如果需要预览那么预览需要在初始化调用之前设置
            initCamera();
            //初始化完成后才可以点打开
            Toast.makeText(MainActivity.this,"可以点开始了",Toast.LENGTH_LONG).show();*/

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Logger.d(TAG, "in surfaceChanged");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Logger.d(TAG, "in surfaceDestroyed");
        }
    };

    private void initCamera(){
        Logger.d(TAG, "initCamera()");
        if(arCamera!=null) {
            arCamera.initCamera(autoArCameraParam);
        }
    }

    private void openCamera() {
        Logger.d(TAG, "openCamera()");
        if(arCamera!=null) {
            boolean openResult = arCamera.openCamera(0);
            if(!openResult){
                Toast.makeText(MainActivity.this,"接口调用异常，可能还未绑定摄像头服务，请先启动摄像头服务程序，绑定摄像头服务后再重试！",Toast.LENGTH_LONG).show();
            }

        }
    }

    private void closeCamera() {
        Logger.d(TAG, "closeCamera()");
        if(arCamera!=null) {
            arCamera.closeCamera();
        }

    }

    private int mFrameCount = 0;
    private int mFps = 0;
    private long mDeltaTime = 0;
    private long mCounterTime = System.currentTimeMillis();
    private boolean isMyStart = false;

    private ReadThread readThread;
    class ReadThread extends Thread{

        String threadName;

        public ReadThread(String threadName) {
            this.threadName = threadName;
        }

        @Override
        public void run() {
            while (arCamera != null && arCamera.isCameraOpened() && isMyStart) {
                Logger.d(TAG, "ReadThread run threadName = "+threadName);
                readData = arCamera.requestCameraData();

                mFrameCount++;
                mDeltaTime = System.currentTimeMillis() - mCounterTime;
                if (mDeltaTime > 1000) {
                    mFps = (int) (((float) mFrameCount / (float) mDeltaTime) * 1000) + 1;
                    mCounterTime = System.currentTimeMillis();
                    mFrameCount = 0;
                    Logger.d(TAG, "FPS: " + mFps);
                }

                if(readData !=null) {
                    if (shoot) {
                        ImageSaverUtils.saveYuv2PictureWithRawData(readData, 1280, 720, false, false);
                        shoot = false;
                    }
                    /**
                     * 将图片数据的某一个字节在ui上进行显示
                     */
                    clientHandler.obtainMessage(1).sendToTarget();
                }else{
                    Logger.d(TAG, "ReadThread run mReadData!=null");
                }

                try {
                    //睡眠时间会直接影响帧率的输出，这里后续需要和服务端保存统一的帧率基调
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Logger.d(TAG, "ReadThread run end");
        }
    }


    private void runReadThread() {
        Logger.d(TAG, "runReadThread()");
        if(readThread!=null){
            readThread.interrupt();
        }
        readThread = new ReadThread("ReadThread_"+System.currentTimeMillis());
        fixedThreadPool.execute(readThread);
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG, "onDestroy()");
        if(arCamera!=null){
            arCamera.unInitCamera();
        }

        super.onDestroy();
    }
}
