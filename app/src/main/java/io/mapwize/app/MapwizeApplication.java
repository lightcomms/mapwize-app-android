package io.mapwize.app;

import android.app.Application;

import io.mapwize.mapwizeformapbox.AccountManager;

import static io.mapwize.app.BuildConfig.API_KEY;


public class MapwizeApplication  extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AccountManager.start(this, API_KEY);
    }

}
