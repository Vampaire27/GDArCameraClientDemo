package com.autonavi.amapauto.gdarcameraservice;

import android.app.Application;

public class ArClientContext {
    private Application app;

    private static class ArContextInstanceHolder {
        private static final ArClientContext INSTANCE = new ArClientContext();
    }

    public static ArClientContext getInstance() {
        return ArContextInstanceHolder.INSTANCE;
    }

    public void setApplication(Application app) {
        this.app = app;
    }

    public Application getApplication() {
        return app;
    }
}
