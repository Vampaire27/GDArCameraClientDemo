package com.autonavi.amapauto.gdarcameraclientdemo;

import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.res.Configuration;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.autonavi.amapauto.gdarcameraservice.ArClientContext;
import com.autonavi.amapauto.utils.Logger;

import java.io.File;

public class ArClientApplication extends Application {

    public ArClientApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ArClientContext.getInstance().setApplication(this);
        initLogger();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public void registerComponentCallbacks(ComponentCallbacks callback) {
        super.registerComponentCallbacks(callback);
    }

    @Override
    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        super.unregisterComponentCallbacks(callback);
    }

    @Override
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        super.registerActivityLifecycleCallbacks(callback);
    }

    @Override
    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        super.unregisterActivityLifecycleCallbacks(callback);
    }

    @Override
    public void registerOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
        super.registerOnProvideAssistDataListener(callback);
    }

    @Override
    public void unregisterOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
        super.unregisterOnProvideAssistDataListener(callback);
    }

    private void initLogger() {
        File logcatFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/amapauto9/amap_start_logcat_open.txt");
        Logger.setLog(BuildConfig.DEBUG || logcatFile.exists());
        Logger.init(this);
        Logger.createLogDir();
    }
}
