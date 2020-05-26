// IGDSize.aidl
package com.autonavi.amapauto.gdarcameraservice;

/**
 * 高德AR导航AIDL标准协议接口
 *
 * 图像规格大小接口
 */
interface IGDSize {
    /**
     * 获取图像规格的宽度
     * @return 图像规格的宽度大小
     */
    int getWidth();

    /**
     * 获取图像规格的高度
     * @return 图像规格的高度大小
     */
    int getHeight();
}
