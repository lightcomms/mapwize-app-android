package io.mapwize.app;

import android.app.Activity;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.indoorlocation.core.IndoorLocation;
import io.indoorlocation.core.IndoorLocationProviderListener;
import io.indoorlocation.manual.ManualIndoorLocationProvider;
import io.indoorlocation.polestarlocationprovider.PolestarIndoorLocationProvider;
import io.indoorlocation.providerselector.SelectorIndoorLocationProvider;
import io.indoorlocation.socketlocationprovider.SocketIndoorLocationProvider;
import io.mapwize.mapwizeformapbox.model.Venue;

public class LocationProvidersManager extends SelectorIndoorLocationProvider {

    private double MIN_DISTANCE_TO_ACTIVATE = 1000;
    private Activity activity;
    private List<Venue> venues;
    private Venue activeVenue;
    private FusedGpsIndoorLocationProvider gpsProvider;
    private ManualIndoorLocationProvider manualProvider;
    private PolestarIndoorLocationProvider polestarProvider;
    private SocketIndoorLocationProvider socketProvider;
    private boolean started = false;

    public LocationProvidersManager(Activity activity) {
        super(60000);
        this.activity = activity;
        this.gpsProvider = new FusedGpsIndoorLocationProvider(activity);
        this.addIndoorLocationProvider(this.gpsProvider);
        this.manualProvider = new ManualIndoorLocationProvider();
        this.addIndoorLocationProvider(this.manualProvider);

        this.gpsProvider.addListener(gpsIndoorLocationProvider);
    }

    public void start() {
        super.start();
        started = true;
    }

    public void stop() {
        this.gpsProvider.stop();
        this.manualProvider.stop();
        if (this.socketProvider != null) {
            this.socketProvider.stop();
        }
        started = false;
    }

    public boolean isStarted() {
        return started;
    }

    public void defineLocation(IndoorLocation indoorLocation) {
        this.manualProvider.setIndoorLocation(indoorLocation);
    }

    public void setVenues(List<Venue> venues) {
        this.venues = venues;
    }

    private void activateSocket(String socketUrl) {
        if (this.socketProvider != null) {
            this.removeIndoorLocationProvider(this.socketProvider);
        }
        this.socketProvider = new SocketIndoorLocationProvider(this.activity, socketUrl);
        this.addIndoorLocationProvider(this.socketProvider);
        this.socketProvider.start();
    }

    private void deactivateSocket() {
        if (this.socketProvider != null) {
            this.removeIndoorLocationProvider(this.socketProvider);
            this.socketProvider.stop();
            this.socketProvider = null;
        }
    }

    private void activatePolestar(String polestarKey, Map<Double, Double> floorByAltitude) {
        if (this.polestarProvider != null) {
            this.removeIndoorLocationProvider(this.polestarProvider);
        }
        this.polestarProvider = new PolestarIndoorLocationProvider(this.activity, polestarKey);
        this.addIndoorLocationProvider(this.polestarProvider);
        this.polestarProvider.setFloorByAlitudeMap(floorByAltitude);
        this.polestarProvider.start();
    }

    private void deactivatePolestar() {
        if (this.polestarProvider != null) {
            this.removeIndoorLocationProvider(this.polestarProvider);
            this.polestarProvider.stop();
            this.polestarProvider = null;
        }
    }

    private void deactivateAll() {
        deactivateSocket();
        deactivatePolestar();
    }

    private Venue getNearestVenue(IndoorLocation location) {
        if (this.venues == null || this.venues.size() == 0) {
            return null;
        }
        LatLng latLng = new LatLng(location);
        Venue nearestVenue = this.venues.get(0);
        double distanceMin = getDistance(latLng, nearestVenue);
        for (Venue venue : venues) {
            double distance = getDistance(latLng, venue);
            if (distance < distanceMin) {
                nearestVenue = venue;
                distanceMin = distance;
            }
        }

        if (distanceMin < MIN_DISTANCE_TO_ACTIVATE) {
            return nearestVenue;
        }
        return null;
    }

    private double getDistance(LatLng latLng, Venue venue) {
        LatLng venueLatLng = venue.getMarker();
        double R = 6381000;
        double lat1 = latLng.getLatitude() * Math.PI / 180d;
        double lat2 = venueLatLng.getLatitude() * Math.PI / 180d;
        double deltaLat = (venueLatLng.getLatitude() - latLng.getLatitude()) * Math.PI /180d;
        double deltaLng = (venueLatLng.getLongitude() - latLng.getLongitude()) * Math.PI /180d;
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLng/2) * Math.sin(deltaLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }

    private void activateVenue(Venue venue) {
        if (venue != activeVenue) {
            activeVenue = venue;
            JSONObject providers = venue.getIndoorLocationProviders();
            if (providers == null) {
                return;
            }
            JSONObject socketProviderObject = providers.optJSONObject("socket");
            if (socketProviderObject != null) {
                boolean enabled = socketProviderObject.optBoolean("enabled", false);
                if (enabled) {
                    String url = socketProviderObject.optString("socketUrl");
                    if (url != null) {
                        activateSocket(url);
                    }
                }
            }
            JSONObject polestarProviderObject = providers.optJSONObject("polestar");
            if (polestarProviderObject != null) {
                boolean enabled = polestarProviderObject.optBoolean("enabled", false);
                if (enabled) {
                    String apiKey = polestarProviderObject.optString("apiKey");
                    if (apiKey != null) {
                        JSONArray array = polestarProviderObject.optJSONArray("floors");
                        Map<Double,Double> floors = new HashMap<>();
                        if (array != null) {
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.optJSONObject(i);
                                if (object != null) {
                                    floors.put(object.optDouble("altitude"), object.optDouble("floor"));
                                }
                            }
                        }
                        activatePolestar(apiKey, floors);
                    }
                }
            }
        }
    }

    public void checkVenueForIndoorLocationActivation() {
        if (mIndoorLocation == null) {
            return;
        }
        Venue venue = getNearestVenue(mIndoorLocation);
        if (venue != null) {
            if (activeVenue != null && activeVenue != venue) {
                deactivateAll();
            }
            activateVenue(venue);
        } else {
            deactivateAll();
        }
    }

    private IndoorLocation mIndoorLocation;
    private IndoorLocationProviderListener gpsIndoorLocationProvider = new IndoorLocationProviderListener() {
        @Override
        public void onProviderStarted() {
            // Don't need
        }

        @Override
        public void onProviderStopped() {
            // Don't need
        }

        @Override
        public void onProviderError(Error error) {
            // Don't need
        }

        @Override
        public void onIndoorLocationChange(IndoorLocation indoorLocation) {
            mIndoorLocation = indoorLocation;
            checkVenueForIndoorLocationActivation();
        }
    };
}
