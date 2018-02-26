package io.mapwize.app;

import android.app.Application;

import io.mapwize.mapwizeformapbox.AccountManager;


public class MapwizeApplication  extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AccountManager.start(this, "e2af1248a493cd196fe54b1dbdba8ba8");
    }

}
