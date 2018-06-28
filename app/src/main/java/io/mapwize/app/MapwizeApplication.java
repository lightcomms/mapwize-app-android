package io.mapwize.app;

import android.app.Application;

import io.mapwize.mapwizeformapbox.AccountManager;


public class MapwizeApplication  extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AccountManager.start(this, "49036d2ce04575909ccc816bcec837ca");
    }

}
