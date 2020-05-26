package com.autonavi.amapauto.gdarcameraservice.camera;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.Surface;

import com.autonavi.amapauto.gdarcameraservice.ArClientContext;
import com.autonavi.amapauto.gdarcameraservice.IGDCameraService;
import com.autonavi.amapauto.gdarcameraservice.IGDCameraStateCallBack;
import com.autonavi.amapauto.gdarcameraservice.constant.ImageFormat;
import com.autonavi.amapauto.gdarcameraservice.constant.RecommendSize;
import com.autonavi.amapauto.gdarcameraservice.constant.gd.ArCameraParam;
import com.autonavi.amapauto.gdarcameraservice.model.ArCameraOpenResultParam;
import com.autonavi.amapauto.gdarcameraservice.model.GDArCameraParam;
import com.autonavi.amapauto.gdarcameraservice.utils.ImageSaverUtils;
import com.autonavi.amapauto.gdarcameraservice.utils.MemoryFileUtil;
import com.autonavi.amapauto.gdarcameraservice.utils.SharedMemUtils;
import com.autonavi.amapauto.utils.Logger;

import java.util.ArrayList;

import static android.content.Context.BIND_AUTO_CREATE;

public class GDCamera extends AbstractArCamera {
    private final static String TAG = "GDCamera";

    private final String clientId = "com.autonavi.amapauto";
    /**
     * 摄像头服务类
     */
    private IGDCameraService gdCameraService;

    /**
     * 摄像头是否连接
     */
    private boolean isCameraConnected;

    /**
     * 摄像头是否打开
     */
    private boolean isCameraOpened;

    /**
     * 通过MemoryFile类提供的writeBytes()将数据写入共享内存, readBytes()从共享内存读取数据
     */
    private MemoryFile memoryFile = null;

    /**
     * 图片宽度
     */
    private int imageWidth = 1280;

    /**
     * 图片高度
     */
    private int imageHeight = 720;

    /**
     * 共享内存的HEADER
     */
    private byte[] header = new byte[SharedMemUtils.HEADER_SIZE];

    /**
     * 摄像头数据大小, 视实际分辨率确定
     */
    private int dataSize = (imageWidth == 0 && imageHeight == 0) ? 1382400 :
            imageWidth * imageHeight * 3 / 2;
    /**
     * 申请内存大小, 等于摄像头数据+标记位
     */
    private int memorySize = dataSize + SharedMemUtils.HEADER_SIZE;

    /**
     * 从共享内存读取的数据
     */
    private byte[] readData = new byte[dataSize];

    /**
     * 高德auto内部用的ar camera的相关参数类对象
     */
    private ArCameraParam autoArCameraParam;

    /**
     * 预览接口用的surface
     */
    private Surface externalSurface = null;

    /**
     * 调试用，保存图片，原始buffer数据
     */
    private boolean isNeedSaveCameraImage = false;

    /**
     * 默认服务的ACTION
     */
    public final static String DEFAULT_SERVICE_ACTION = "com.autonavi.amapauto.gdarcameraservice";

    /**
     * 默认服务所在程序的包名
     */
    public final static String DEFAULT_SERVICE_PACKAGE_NAME = "com.autonavi.amapauto.gdarcameraservicedemo";

    /**
     * 服务的ACTION
     */
    private String serviceAction = DEFAULT_SERVICE_ACTION;

    /**
     * 服务所在程序的包名
     */
    private String servicePackageName = DEFAULT_SERVICE_PACKAGE_NAME;

    public GDCamera(String serviceAction,String servicePackageName) {
        if(!TextUtils.isEmpty(serviceAction)) {
            this.serviceAction = serviceAction;
        }else{
            Logger.d(TAG, "GDCamera serviceAction == null");
        }
        if(!TextUtils.isEmpty(servicePackageName)) {
            this.servicePackageName = servicePackageName;
        }else{
            Logger.d(TAG, "GDCamera servicePackageName == null");
        }
        Logger.d(TAG, "GDCamera serviceAction = {?},servicePackageName = {?}",serviceAction,servicePackageName);
    }

    /**
     * 绑定服务
     */
    private void bindCameraService() {
        Logger.d(TAG, "bindCameraService");
        //Intent intent =  new Intent();
        //intent.setComponent(new ComponentName("com.autonavi.amapauto.gdarcameraservicedemo","com.autonavi.amapauto.gdarcameraservicedemo.GDArCameraService"));
        Intent intent =  new Intent();
        intent.setAction(serviceAction);
        intent.setPackage(servicePackageName);

        boolean result = ArClientContext.getInstance().getApplication().bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        Logger.d(TAG, "bindCameraService bind result = " + result);
    }

    /**
     * 绑定服务
     */
    private void unBindCameraService(){
        Logger.d(TAG, "bindCameraService");
        ArClientContext.getInstance().getApplication().unbindService(mServiceConnection);
    }

    private IGDCameraStateCallBack.Stub gdCameraStateCallBack = new IGDCameraStateCallBack.Stub() {

        @Override
        public void onConnected() throws RemoteException {
            Logger.d(TAG, "onConnected");
            isCameraConnected = true;
        }

        @Override
        public void onDisconnected() throws RemoteException {
            Logger.d(TAG, "onDisconnected");
            isCameraConnected = false;
        }


        @Override
        public void onOpened(ParcelFileDescriptor parcelFileDescriptor, ArCameraOpenResultParam arCameraOpenResultParam, String memoryfileName) throws RemoteException {
            Logger.d(TAG, "onOpened " + parcelFileDescriptor + ", " +(arCameraOpenResultParam!=null?arCameraOpenResultParam.toString():"null") + ", " + memoryfileName);
            //发现有些设备的图片宽高和image的buffer大小没有必然的联系，比如华为设备，返回的图片的大小可能是960、540，但是image的buffer大小确是固定为4147198，设备分辨率确是1920x1280
            isCameraOpened = true;
            if(arCameraOpenResultParam!=null) {
                reInitCameraparam(arCameraOpenResultParam.imageWidth, arCameraOpenResultParam.imageHeight, arCameraOpenResultParam.imageSize,arCameraOpenResultParam.imageFormat);
            }
            /**
             * 通过文件描述器创建MemoryFile对象
             */
            try {
                Logger.d(TAG, "parcelFileDescriptor: " + parcelFileDescriptor);
                if (parcelFileDescriptor != null) {
                    memoryFile = MemoryFileUtil.openMemoryFile(parcelFileDescriptor, memorySize,
                            MemoryFileUtil.OPEN_READWRITE);
                }
            } catch (Exception e) {
                Logger.d(TAG, e.toString());
            }

        }

        @Override
        public void onClosed(int code, String message) throws RemoteException {
            Logger.d(TAG, "onClosed()");
            isCameraOpened = false;
        }

        @Override
        public void onError(int code, String message) throws RemoteException {
            Logger.d(TAG, "CameraClientsManager onError() code = " + code + "   message = " + message);
        }
    };
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Logger.d(TAG, "onServiceConnected");
            gdCameraService = IGDCameraService.Stub.asInterface(iBinder);
            try {
                initArCameraParamWithMem();
                /**
                 * 获取连接状态
                 */
                isCameraConnected = gdCameraService.isCameraConnected(clientId);
                Logger.d(TAG, "onServiceConnected isCameraConnected = {?}",isCameraConnected);
                /**
                 * 注册摄像头状态监听
                 */
                gdCameraService.registerCameraStateCallback(clientId,gdCameraStateCallBack);

                openCamera(0);
            } catch (Exception e) {
                Logger.d(TAG, e.toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Logger.d(TAG, "onServiceDisconnected");
            try {
                gdCameraService.unregisterCameraStateCallback(clientId,gdCameraStateCallBack);
            } catch (RemoteException e) {
                Logger.d(TAG, "onServiceDisconnected RemoteException = {?}",e.toString());
            }
            gdCameraService = null;
        }
    };

    private void reInitCameraparam(int imageWidth,int imageHeight,int imageSize,int imageFormat){
        Logger.d(TAG, "reInitCameraparam before change imageWidth = {?},imageHeight = {?},imageSize = {?},imageFormat = {?}",imageWidth,imageHeight,imageSize,imageFormat);
        Logger.d(TAG, "reInitCameraparam before change IMAGE_WIDTH = {?},IMAGE_HEIGHT = {?},DATA_SIZE = {?},MEMORY_SIZE = {?}", this.imageWidth, this.imageHeight, dataSize, memorySize);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        if(imageSize>0){
            dataSize = imageSize;
        }else{
            dataSize = (this.imageWidth == 0 && this.imageHeight == 0) ? 1382400 :
                    this.imageWidth * this.imageHeight * 3 / 2;
        }
        memorySize = dataSize + SharedMemUtils.HEADER_SIZE;
        readData = new byte[dataSize];
        Logger.d(TAG, "reInitCameraparam after change IMAGE_WIDTH = {?},IMAGE_HEIGHT = {?},DATA_SIZE = {?},MEMORY_SIZE = {?}", this.imageWidth, this.imageHeight, dataSize, memorySize);
    }

    private int convertAutoImageFormatToStandard(int imageFormat){
        Logger.d(TAG, "convertAutoImageFormatToStandard imageFormat = {?}",imageFormat);
        int standardImageFormat = ImageFormat.I420_822;
        switch(imageFormat){
            case ArCameraParam.IMAGE_TYPE_RGB_565:
                standardImageFormat =  ImageFormat.RGB_888;
                break;
            case ArCameraParam.IMAGE_TYPE_YV_12:
                standardImageFormat =  ImageFormat.YV12;
                break;
            case ArCameraParam.IMAGE_TYPE_NV16:
                standardImageFormat = ImageFormat.NV21_822;
                break;
            case ArCameraParam.IMAGE_TYPE_NV21:
                standardImageFormat = ImageFormat.NV21_822;
                break;
            case ArCameraParam.IMAGE_TYPE_YUY2:
                standardImageFormat = ImageFormat.I420_822;
                break;
            case ArCameraParam.IMAGE_TYPE_JPEG:
                standardImageFormat = ImageFormat.I420_822;
                break;
            case ArCameraParam.IMAGE_TYPE_YUV_420_888:
                standardImageFormat = ImageFormat.I420_822;
                break;
            case ArCameraParam.IMAGE_TYPE_YUV_422_888:
                standardImageFormat = ImageFormat.I420_822;
                break;
            case ArCameraParam.IMAGE_TYPE_YUV_444_888:
                standardImageFormat = ImageFormat.I420_822;
                break;
            case ArCameraParam.IMAGE_TYPE_RGB_888:
                standardImageFormat = ImageFormat.RGB_888;
                break;
            case ArCameraParam.IMAGE_TYPE_BGRBGR_888:
                standardImageFormat = ImageFormat.RGBA_8888;
                break;
            default:
                standardImageFormat = ImageFormat.I420_822;
                break;
        }
        Logger.d(TAG, "convertAutoImageFormatToStandard standardImageFormat = {?}",standardImageFormat);
        return standardImageFormat;
    }

    private boolean initArCameraParamWithMem(){
        Logger.d(TAG, "initArCameraParamWithMem");
        reInitCameraparam(autoArCameraParam.width,autoArCameraParam.height,0,0);
        if(gdCameraService!=null){
            Logger.d(TAG, "initArCameraParamWithMem gdCameraService!=null");
            GDArCameraParam GDArCameraParam = new GDArCameraParam();
            GDArCameraParam.imageFormat = convertAutoImageFormatToStandard(autoArCameraParam.format);
            GDArCameraParam.imageWidth = autoArCameraParam.width;
            GDArCameraParam.imageHeight = autoArCameraParam.height;
            try {
                return gdCameraService.initCamera(clientId, GDArCameraParam,externalSurface);
            } catch (RemoteException e) {
                Logger.d(TAG, "initArCameraParamWithMem RemoteException = {?}",e.toString());
                return false;
            }
        }else{
            Logger.d(TAG, "initArCameraParamWithMem gdCameraService==null");
            return false;
        }
    }
    @Override
    public boolean initCamera(ArCameraParam param) {
        if(param!=null) {
            //测试，这里先指定为某一个格式
//            param.width = 800;
//            param.height = 480;
//            param.format = ArCameraParam.IMAGE_TYPE_NV21;
            param.format = ArCameraParam.IMAGE_TYPE_YUV_420_888;

            autoArCameraParam = param;
            bindCameraService();
            Logger.d(TAG, "initCamera param = {?}",param.toString());
            return initArCameraParamWithMem();
        }else{
            Logger.d(TAG, "initCamera param==null");
            return false;
        }
    }

    @Override
    public boolean openCamera(int id) {
        Logger.d(TAG, "openCamera id = {?}",id);
        if(gdCameraService!=null){
            try {
                Logger.d(TAG, "openCamera before isCameraOpened = {?},isCameraConnected = {?}",isCameraOpened,isCameraConnected);
                isCameraOpened = gdCameraService.isCameraOpened(clientId);
                isCameraConnected = gdCameraService.isCameraConnected(clientId);
                Logger.d(TAG, "openCamera after isCameraOpened = {?},isCameraConnected = {?}",isCameraOpened,isCameraConnected);
                if(isCameraConnected && isCameraOpened){
                    Logger.d(TAG, "openCamera has opened");
                    return true;
                }
                gdCameraService.openCamera(clientId);
            } catch (RemoteException e) {
                Logger.d(TAG, "openCamera RemoteException = {?}",e.toString());
                return false;
            }
        }else{
            Logger.d(TAG, "openCamera gdCameraService==null");
            return false;
        }
        return true;
    }
    private static int convertStandardImageFormatToAuto(int imageFormat){
        Logger.d(TAG, "convertStandardImageFormatToAuto imageFormat = {?}",imageFormat);
        int autoImageFormat = ArCameraParam.IMAGE_TYPE_YUV_420_888;
        switch(imageFormat){
            case ImageFormat.NV21_822:
                autoImageFormat = ArCameraParam.IMAGE_TYPE_NV21;
                break;
            case ImageFormat.I420_822:
                autoImageFormat = ArCameraParam.IMAGE_TYPE_YUV_420_888;
                break;
            case ImageFormat.RGB_888:
                autoImageFormat = ArCameraParam.IMAGE_TYPE_RGB_888;
                break;
            case ImageFormat.RGBA_8888:
                autoImageFormat = ArCameraParam.IMAGE_TYPE_BGRBGR_888;
                break;
            case ImageFormat.YV12:
                autoImageFormat = ArCameraParam.IMAGE_TYPE_YV_12;
                break;
            default:
                autoImageFormat = ArCameraParam.IMAGE_TYPE_YUV_420_888;
                break;
        }
        Logger.d(TAG, "convertStandardImageFormatToAuto autoImageFormat = {?}",autoImageFormat);
        return autoImageFormat;
    }


    private byte[] readCameraData(){
        Logger.d(TAG, "readCameraData");
        byte[] info = null;
        if (memoryFile != null) {
            Logger.d(TAG, "readCameraData memoryFile != null");
            try {
                /**
                 * step7-读取共享内存第1个字节数据, 即可读/可写标记位
                 */
                memoryFile.readBytes(header, 0, 0, SharedMemUtils.HEADER_SIZE);
                /**
                 * step8-判断共享内存是否可读
                 */
                boolean canRead = SharedMemUtils.canRead(header);
                Logger.d(TAG, "readCameraData canRead = {?}",canRead);
                if (canRead) {
                    Logger.d(TAG, "readCameraData canRead");
                    /**
                     * step9-如果可读, 读取共享内存后DATA_SIZE字节数据
                     */
                    memoryFile.readBytes(readData, SharedMemUtils.HEADER_SIZE, 0, dataSize);

                    /**
                     * step10-读取完成, 将可读可写标记位置为可写
                     */
                    SharedMemUtils.setWirtable(header);
                    memoryFile.writeBytes(header, 0, 0, SharedMemUtils.HEADER_SIZE);

                    Logger.d(TAG, "read data length = " + readData.length);

                    info = readData;

                    if (isNeedSaveCameraImage) {
                        Logger.d(TAG, "readCameraData canRead isNeedSaveCameraImage");
                        ImageSaverUtils.saveYuv2PictureWithRawData(readData, imageWidth, imageHeight,false,true);
                    }
                }
            } catch (Exception e) {
                Logger.d(TAG, e.toString());
            }
        }else{
            Logger.d(TAG, "readCameraData memoryFile == null");
        }
        return info;
    }



    @Override
    public byte[] requestCameraData() {
        Logger.d(TAG, "requestCameraData isCameraOpened = "+isCameraOpened);
        /*if (!isCameraOpened) {
            return null;
        }*/
        byte[] image = readCameraData();
        if (image == null) {
            Logger.d(TAG, "requestCameraData image == null");
            return null;
        }
        Logger.d(TAG, "requestCameraData image = {?}",image.toString());
        return image;
    }

    @Override
    public boolean closeCamera() {
        Logger.d(TAG, "closeCamera mIsCameraOpened = {?}", isCameraOpened);

        try {
            if(gdCameraService!=null) {

                boolean result = gdCameraService.closeCamera(clientId);

                /**
                 * step13-关闭共享内存
                 */
                if (memoryFile != null) {
                    memoryFile.close();
                    memoryFile = null;
                }else{
                    Logger.d(TAG, "closeCamera memoryFile==null");
                }

                Logger.d(TAG, "closeCamera result = {?}", result);
                return result;
            }else{
                Logger.d(TAG, "closeCamera gdCameraService==null");
            }
        } catch (Exception e) {
            Logger.d(TAG, e.toString());
            return false;
        } finally {

            isCameraOpened = false;
            isCameraConnected = true;
        }
        return false;
    }

    @Override
    public boolean isCameraOpened() {
        if(gdCameraService!=null) {
            try {
                return gdCameraService.isCameraOpened(clientId);
            } catch (RemoteException e) {
                Logger.d(TAG, "isCameraOpened RemoteException = {?}",e.toString());
            }
        }else{
            Logger.d(TAG, "isCameraOpened gdCameraService==null");
        }
        return false;
    }

    @Override
    public boolean unInitCamera() {
        if(gdCameraService!=null) {
            try {
                closeCamera();
                gdCameraService.unregisterCameraStateCallback(clientId,gdCameraStateCallBack);
                gdCameraService.unInitCamera(clientId);
                unBindCameraService();

                return true;
            } catch (RemoteException e) {
                Logger.d(TAG, "unInitCamera RemoteException = {?}",e.toString());
            }
        }else{
            Logger.d(TAG, "unInitCamera gdCameraService==null");
        }
        return false;
    }

    @Override
    public void setSurface(Surface surface) {
        super.setSurface(surface);
        this.externalSurface = surface;
    }

    @Override
    public int GetCameraSupportFormat(ArrayList<Integer> formats) {
        return super.GetCameraSupportFormat(formats);
    }

    @Override
    public void setArNaviStatus(int arNaviStatus) {
        super.setArNaviStatus(arNaviStatus);
    }

    @Override
    public String getCustomCameraId() {
        return super.getCustomCameraId();
    }

    @Override
    public RecommendSize getRealImageSize() {
        return new RecommendSize(this.imageWidth,this.imageHeight);
    }

    @Override
    public void releaseImageInfo() {
        super.releaseImageInfo();
    }


}
