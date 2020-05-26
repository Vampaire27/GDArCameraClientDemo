package com.autonavi.amapauto.gdarcameraservice.utils;

import android.os.Build;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;

public class MemoryFileUtil {
    private static final String TAG = MemoryFileUtil.class.getSimpleName();

    private static final String MEMORY_NAME = "CameraService";

    private static final int PROT_READ = 0x1;
    private static final int PROT_WRITE = 0x2;
    public static final int OPEN_READONLY = PROT_READ;
    public static final int OPEN_READWRITE = PROT_READ |PROT_WRITE;

    /**
     * 通过文件描述符创建MemoryFile, Android 28及更高版本处理方式不同
     *
     * @param parcelFileDescriptor 序列化后的文件描述符, 从CameraService获取
     * @param length 长度, 测试发现只要为正数即可
     * @param mode 读写模式, 一般为OPEN_READWRITE
     *
     * @return MemoryFile对象 or null
     */
    public static MemoryFile openMemoryFile(ParcelFileDescriptor parcelFileDescriptor, int length, int mode){
        Log.d(TAG, "openMemoryFile()");
        if(parcelFileDescriptor == null){
            Log.e(TAG, "parcelFileDescriptor is null");
            return null;
        }
        if (mode != OPEN_READONLY && mode != OPEN_READWRITE) {
            Log.e(TAG, "mode is error");
            return null;
        }
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            return openMemoryFileV27(fileDescriptor, length, mode);
        }
        return openMemoryFileV28(fileDescriptor, mode);
    }

    private static MemoryFile openMemoryFileV27(FileDescriptor fileDescriptor, int length, int mode){
        Log.d(TAG, "openMemoryFile27()");
        MemoryFile memoryFile = null;
        try {
            memoryFile = new MemoryFile(MEMORY_NAME, 1);
            memoryFile.close();
            Class<?> clz = MemoryFile.class;
            InvokeUtil.setValueOfField(memoryFile, "mFD", fileDescriptor);
            InvokeUtil.setValueOfField(memoryFile, "mLength", length);
            long address = (long) InvokeUtil.invokeStaticMethod(clz, "native_mmap",
                fileDescriptor, length, mode);
            InvokeUtil.setValueOfField(memoryFile,"mAddress", address);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return memoryFile;
    }

    private static MemoryFile openMemoryFileV28(FileDescriptor fileDescriptor, int mode) {
        Log.d(TAG, "openMemoryFile28()");
        MemoryFile memoryFile = null;
        try {
            memoryFile = new MemoryFile(MEMORY_NAME, 1);
            memoryFile.close();
            Class<?> clz = Class.forName("android.os.SharedMemory");
            Object sharedMemory = InvokeUtil.newInstanceOrThrow(clz, fileDescriptor);
            ByteBuffer mapping;
            if (mode == OPEN_READONLY) {
                mapping = (ByteBuffer) InvokeUtil.invokeMethod(sharedMemory, "mapReadOnly");
            } else {
                mapping = (ByteBuffer) InvokeUtil.invokeMethod(sharedMemory, "mapReadWrite");
            }
            InvokeUtil.setValueOfField(memoryFile, "mSharedMemory", sharedMemory);
            InvokeUtil.setValueOfField(memoryFile, "mMapping", mapping);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return memoryFile;
    }
}
