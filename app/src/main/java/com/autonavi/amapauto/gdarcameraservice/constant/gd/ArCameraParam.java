package com.autonavi.amapauto.gdarcameraservice.constant.gd;

public class ArCameraParam {

    public static final int IMAGE_TYPE_UNKNOWN = 0;

    public static final int IMAGE_TYPE_RGB_565 = 4;

    public static final int IMAGE_TYPE_YV_12 = 842094169;

    public static final int IMAGE_TYPE_NV16 = 16;

    public static final int IMAGE_TYPE_NV21 = 17;

    public static final int IMAGE_TYPE_YUY2 = 20;

    public static final int IMAGE_TYPE_JPEG = 256;

    public static final int IMAGE_TYPE_YUV_420_888 = 35;

    public static final int IMAGE_TYPE_YUV_422_888 = 39;

    public static final int IMAGE_TYPE_YUV_444_888 = 40;

    public static final int IMAGE_TYPE_RGB_888 = 41;

    public static final int IMAGE_TYPE_BGRBGR_888  = 0x2000030;

    public static final int CAMERA_FACING_FRONT = 0;

    public static final int CAMERA_FACING_BACK = 1;

    public static final int CAMERA_FACING_EXTERNAL = 2;

    public int width;

    public int height;

    public int format;

    public int cameraFacing;

    public ArCameraParam(int width, int height, int format, int cameraFacing) {
        this.width = width;
        this.height = height;
        this.format = format;
        this.cameraFacing = cameraFacing;
    }

    @Override
    public String toString() {
        return "ArCameraParam{" +
            "width=" + width +
            ", height=" + height +
            ", format=" + format +
            ", cameraFacing=" + cameraFacing +
            '}';
    }
}
