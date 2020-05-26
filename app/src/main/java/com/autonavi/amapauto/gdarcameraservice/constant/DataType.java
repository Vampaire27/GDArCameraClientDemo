package com.autonavi.amapauto.gdarcameraservice.constant;

/**
 * 摄像头的数据类型
 */
public enum DataType {
    /**
     * YUV数据格式类型
     */
    YUV(0),
    /**
     * H264视频流格式类型
     */
    H264(1);

    /**
     * 摄像头的数据类型
     */
    private int type;

    private DataType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }
}
