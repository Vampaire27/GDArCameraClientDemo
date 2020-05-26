// IGDCameraService.aidl
package com.autonavi.amapauto.gdarcameraservice;

import com.autonavi.amapauto.gdarcameraservice.IGDCameraStateCallBack;
import android.view.Surface;
import com.autonavi.amapauto.gdarcameraservice.IGDSize;
import com.autonavi.amapauto.gdarcameraservice.model.GDArCameraParam;

/**
 * 高德AR导航AIDL标准协议接口
 *
 * 摄像头服务接口
 */
interface IGDCameraService {
    /**
     * 注册摄像头状态监听
     * @param clientId 客户端ID
     * @param gdCameraStateCallBack 状态回调接口对象
     *
     * @return true：成功, false：失败
     */
    boolean registerCameraStateCallback(String clientId,IGDCameraStateCallBack gdCameraStateCallBack);

    /**
     * 注销摄像头状态监听
     * @param clientId 客户端ID
     * @param gdCameraStateCallBack 状态回调接口对象
     *
     * @return true：成功, false：失败
     */
    boolean unregisterCameraStateCallback(String clientId,IGDCameraStateCallBack gdCameraStateCallBack);

    /**
     * 设备是否支持AR导航
     * @param clientId 客户端ID
     *
     * @return true：支持, false：不支持
     */
    boolean isSupportArNavi(String clientId);

    /**
     * 获取支持的建议的图像规格大小
     * @param clientId 客户端ID
     *
     * @return IGDSize：建议的图像规格大小对象
     */
    IGDSize getRecommendSize(String clientId);

    /**
     * 摄像头是否已连接
     * @param clientId 客户端ID
     *
     * @return true：已连接, false：未连接
     */
    boolean isCameraConnected(String clientId);

    /**
     * 摄像头是否已打开
     * @param clientId 客户端ID
     *
     * @return true：已打开, false：未打开
     */
    boolean isCameraOpened(String clientId);

    /**
     * 初始化摄像头参数
     * @param clientId 客户端ID
     * @param imageFormat 图像格式，见{@link ImageFormat}
     * @param dataType 数据类型，见{@link DataType}
     * @param cameraUseType 摄像头使用类型，见{@link CameraUseType}
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @param surface Surfaced对象，可以实现渲染/预览
     *
     * @return true：成功, false：失败
     */
    //boolean initCamera(String clientId,int imageFormat, int dataType, int cameraUseType,int imageWidth, int imageHeight, in Surface surface);
    boolean initCamera(String clientId,in GDArCameraParam gdArCameraParam, in Surface surface);

    /**
     * 打开摄像头
     * @param clientId 客户端ID
     *
     * @return true：成功, false：失败
     */
    boolean openCamera(String clientId);

    /**
     * 关闭摄像头
     * @param clientId 客户端ID
     *
     * @return true：成功, false：失败
     */
    boolean closeCamera(String clientId);

    /**
     * 释放占用的摄像头资源,主要为释放客户端设置给服务端的一些资源，服务端自己创建并维护的资源需要自己在适当的时机进行释放
     * @param clientId 客户端ID
     *
     * @return true：成功, false：失败
     */
    boolean unInitCamera(String clientId);
}
