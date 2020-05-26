// IGDCameraStateCallBack.aidl
package com.autonavi.amapauto.gdarcameraservice;

import android.os.ParcelFileDescriptor;
import com.autonavi.amapauto.gdarcameraservice.model.ArCameraOpenResultParam;
/**
 * 高德AR导航AIDL标准协议接口
 *
 * 摄像头状态回调接口
 */
interface IGDCameraStateCallBack{
    /**
     * 摄像头连接回调
     */
    void onConnected();

    /**
     * 摄像头断开回调
     */
    void onDisconnected();

    /**
     * 摄像头已开启的回调，建议在摄像头成功打开并且有返回第一张图片、共享内存打开成功时才回调该接口
     * @param parcelFileDescriptor 文件描述器（共享内存）
     * @param arCameraOpenResultParam            打开摄像头后的返回的结果参数
     * @param memoryfileName       共享文件名
     */
    void onOpened(in ParcelFileDescriptor parcelFileDescriptor, in ArCameraOpenResultParam arCameraOpenResultParam, String memoryfileName);

    /**
     * 摄像头关闭回调
     * @param code     关闭原因编码
     * @param message  对应提示信息
     */
    void onClosed(int code, String message);

    /**
     * 摄像头相关异常回调
     * @param code     异常原因编码
     * @param message  对应提示信息
     */
    void onError(int code, String message);

}