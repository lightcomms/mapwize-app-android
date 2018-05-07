package io.mapwize.app;

import android.content.Context;
import io.indoorlocation.core.IndoorLocation;
import io.indoorlocation.gps.GPSIndoorLocationProvider;
import io.indoorlocation.manual.ManualIndoorLocationProvider;
import io.indoorlocation.providerselector.SelectorIndoorLocationProvider;

public class MapwizeLocationProvider extends SelectorIndoorLocationProvider {

    private GPSIndoorLocationProvider mGpsIndoorLocationProvider;
    private ManualIndoorLocationProvider mManualIndoorLocationProvider;

    MapwizeLocationProvider(Context context) {
        super(60000);
        mGpsIndoorLocationProvider = new GPSIndoorLocationProvider(context);
        mManualIndoorLocationProvider = new ManualIndoorLocationProvider();
        this.addIndoorLocationProvider(mGpsIndoorLocationProvider);
        this.addIndoorLocationProvider(mManualIndoorLocationProvider);
    }

    public void defineLocation(IndoorLocation indoorLocation) {
        mManualIndoorLocationProvider.setIndoorLocation(indoorLocation);
    }

}
