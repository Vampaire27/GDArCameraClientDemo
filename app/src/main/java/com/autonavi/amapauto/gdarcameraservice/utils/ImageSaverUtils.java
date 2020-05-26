package com.autonavi.amapauto.gdarcameraservice.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;
import com.autonavi.amapauto.utils.Logger;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class ImageSaverUtils {
    private final static String TAG = "GDCamera";
    /**
     * 调试用，图片保存路径
     */
    private static String imageSavePath = "/sdcard/amapauto9/GDArCameraClientDemo/";

    private static String restrict = "restrict/";

    /**
     * 保存图片NV21数据为jpg, 调试过程中查看图片信息是否正确
     */
    public static void saveYuv2PictureWithApi(byte[] data, int width, int height) {
        Logger.d(TAG, "saveYuv2PictureWithApi width = "+width+" height = "+height);
        FileOutputStream outStream;
        File file = new File(imageSavePath);
        if(!file.exists()){
            file.mkdirs();
        }
        try {
            //java.lang.IllegalArgumentException: only support ImageFormat.NV21 and ImageFormat.YUY2 for now
            YuvImage yuvimage = new YuvImage(data, android.graphics.ImageFormat.NV21, width, height, null);
            //YuvImage yuvimage = new YuvImage(data, convertStandardImageFormatToAuto(ImageFormat.I420_822), width, height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
            Bitmap bmp = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().length);
            outStream = new FileOutputStream(String.format("%s_%d_%d_%d%s", imageSavePath+"/yuvimage",
                    System.currentTimeMillis(), width, height, ".jpg"));
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.write(baos.toByteArray());
            outStream.close();
            baos.close();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private static int saveCount = 0;
    public static void saveYuv2PictureWithRawData(byte[] data,int width, int height,boolean isConverted,boolean isNeedRestrict) {
        Logger.d(TAG, "saveYuv2PictureWithRawData width = "+width+" height = "+height);

        if(isNeedRestrict) {
            //调用10次保存一次，避免保存太频繁，SD卡操作频繁卡死
            saveCount++;
            if (saveCount / 10 != 0) {
                return;
            }
        }

        try {
            File file = new File(imageSavePath+(isNeedRestrict?restrict:""));
            if (!file.exists()) {
                file.mkdirs();
            }
            String fileName = "";
            if(isConverted){
                fileName = "yuv_al_converted_image_";
            }else{
                fileName = "yuv_al_unConverted_image_";
            }
            FileOutputStream fos = new FileOutputStream(new File(imageSavePath+"/"+fileName + System.currentTimeMillis() + ".yuv"));
            fos.write(data);
            fos.close();
            fos.flush();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}
