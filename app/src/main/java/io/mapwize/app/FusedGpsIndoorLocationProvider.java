package io.mapwize.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import io.indoorlocation.core.IndoorLocation;
import io.indoorlocation.core.IndoorLocationProvider;

public class FusedGpsIndoorLocationProvider extends IndoorLocationProvider {

    private FusedLocationProviderClient mFusedLocationClient;
    private Activity mActivity;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private boolean started = false;

    public FusedGpsIndoorLocationProvider(Activity activity) {
        super();
        mActivity = activity;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                IndoorLocation indoorLocation = new IndoorLocation(locationResult.getLastLocation(), null);
                dispatchIndoorLocationChange(indoorLocation);
            };
        };
    }

    @Override
    public boolean supportsFloor() {
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void start() {

        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location != null) {
                    IndoorLocation indoorLocation = new IndoorLocation(location, null);
                    dispatchIndoorLocationChange(indoorLocation);
                }
            }
        });

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        started = true;
    }

    @Override
    public void stop() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public boolean isStarted() {
        return started;
    }
}
