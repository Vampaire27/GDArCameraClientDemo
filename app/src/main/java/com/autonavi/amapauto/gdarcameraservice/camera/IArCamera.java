package com.autonavi.amapauto.gdarcameraservice.camera;


import android.view.Surface;

import com.autonavi.amapauto.gdarcameraservice.constant.RecommendSize;
import com.autonavi.amapauto.gdarcameraservice.constant.gd.ArCameraParam;

import java.util.ArrayList;

/**
 * Android camera接口类.
 */
public interface IArCamera {
    /**
     * Init camera boolean.
     *
     * @param param the param
     * @return the boolean
     */
    boolean initCamera(ArCameraParam param);

    /**
     * Open camera.
     *
     * @param id 摄像头类别
     * @return the boolean
     */
    boolean openCamera(int id);

    /**
     * Request camera data ar camera data.
     *
     * @return the ar camera data
     */
    byte[] requestCameraData();

    /**
     * Close camera.
     *
     * @return the boolean
     */
    boolean closeCamera();

    /**
     * Is camera opened boolean.
     *
     * @return the boolean
     */
    boolean isCameraOpened();

    /**
     * Un init camera boolean.
     *
     * @return the boolean
     */
    boolean unInitCamera();

    /**
     * Sets Surface. 测试预览功能接口
     *
     * @param surface the surface
     */
    void setSurface(Surface surface);

    /**
     * get the support formats;
     *
     * @param formats the support formats list;
     */
    int GetCameraSupportFormat(ArrayList<Integer> formats);

    /**
     * set the ar navi status;
     *
     * @param arNaviStatus the status of arnavi
     */
    void setArNaviStatus(int arNaviStatus);

    /**
     * 获取自定义的Camera id，即指定固定只打开某个id的camera
     * @return
     */
    String getCustomCameraId();

    /**
     *
     * @return
     */
    RecommendSize getRealImageSize();

    /**
     * 释放image数据
     * @return
     */
    void releaseImageInfo();
}
