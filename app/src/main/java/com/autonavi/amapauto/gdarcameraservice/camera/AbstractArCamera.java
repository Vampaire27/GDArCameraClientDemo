package com.autonavi.amapauto.gdarcameraservice.camera;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import com.autonavi.amapauto.gdarcameraservice.constant.RecommendSize;
import com.autonavi.amapauto.gdarcameraservice.constant.gd.ArCameraParam;

import java.util.ArrayList;

public abstract class AbstractArCamera implements IArCamera{

    @Override
    public boolean initCamera(ArCameraParam param) {
        return false;
    }

    @Override
    public boolean openCamera(int id) {
        return false;
    }

    @Override
    public byte[] requestCameraData() {
        return null;
    }

    @Override
    public boolean closeCamera() {
        return false;
    }

    @Override
    public boolean isCameraOpened() {
        return false;
    }

    @Override
    public boolean unInitCamera() {
        return false;
    }

    @Override
    public void setSurface(Surface surface) {

    }

    @Override
    public int GetCameraSupportFormat(ArrayList<Integer> formats) {
        return 0;
    }

    @Override
    public void setArNaviStatus(int arNaviStatus) {

    }

    @Override
    public String getCustomCameraId() {
        return null;
    }

    @Override
    public RecommendSize getRealImageSize() {
        return new RecommendSize(1280,720);
    }

    public void releaseImageInfo(){

    }
}
