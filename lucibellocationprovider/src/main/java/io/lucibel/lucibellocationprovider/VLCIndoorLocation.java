package io.lucibel.lucibellocationprovider;

import android.annotation.SuppressLint;
import android.app.Application;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import io.indoorlocation.core.IndoorLocation;
import io.indoorlocation.core.IndoorLocationProvider;
import io.lucibel.api.IPCCallbacks;
import io.lucibel.api.Sequencer;

/**
 * <p>                          Example class to call vlc command and link callbacks
 * <p><strong>Description :</strong>
 * <p><strong>File :</strong>       VLCIndoorLocation.java
 * <p><strong>Author :</strong>     Xavier Melendez
 * <p><strong>Date :</strong>       10/05/2017
 * <p><strong>Company :</strong>     slms
 */
public final class VLCIndoorLocation extends IndoorLocationProvider
{
    // use to identify messages in tables of VLC ids
    private final static int RAW_MSG_LENGTH=BuildConfig.VLC_RAW_MSG_LENGTH;
    private static boolean started=false;
    private static IndoorLocation lastLocation;
    static {
        lastLocation = new IndoorLocation(new Location(VLCIndoorLocation.class.getName()),BuildConfig.FLOOR);
        lastLocation.setBearing(0f);
        lastLocation.setAccuracy(0f);
    }


    @SuppressLint({"StaticFieldLeak"})
    private static VLCIndoorLocation vlcIndoorLocation =null;
    private final Application application;
    private final Handler handler;
    private final String vlcApiKey;
    private boolean doLiveLocation=false;
    private List<VLCBeaconListRetriever.VLCBeacon> vlcKnownLocation;
    private String vlcId;
    private java.util.logging.Logger logger= Logger.getLogger(VLCIndoorLocation.class.getName());
    //private final static  java.util.logging.Logger logger = Logger.getLogger(VLCIndoorLocation.class.getName());

    public static VLCIndoorLocation init(@NonNull Application application, @NonNull String vlcAPIKey){
        if(vlcIndoorLocation != null) {
            //throw new IllegalStateException("VLCIndoorLocation already initialized");
            lastLocation = new IndoorLocation(new Location(VLCIndoorLocation.class.getName()),BuildConfig.FLOOR);
            lastLocation.setBearing(0f);
            lastLocation.setAccuracy(0f);
            return vlcIndoorLocation;
        } else {
            vlcIndoorLocation = new VLCIndoorLocation(application,vlcAPIKey);

            return vlcIndoorLocation;
        }
    }

    public static VLCIndoorLocation getVlcIndoorLocation(){
        return vlcIndoorLocation;
    }

    private VLCIndoorLocation(Application application, String vlcAPIKey){
        this.application=application;
        this.handler = new android.os.Handler(application.getMainLooper());
        this.vlcApiKey=vlcAPIKey;
        // TODO: 28/06/2018 : this may have been loaded on Mapwize implementation
        VLCBeaconListRetriever.go(new VLCBeaconListRetriever.VLCBeaconCallback() {
            @Override
            public void onFailure( IOException e) {
                logger.severe("onFailure:"+e.getMessage());
                doLiveLocation = false;
                vlcKnownLocation=null;
            }

            @Override
            public void onSuccess(List<VLCBeaconListRetriever.VLCBeacon> beacons)  {
                logger.severe("onResponse");
                vlcKnownLocation=beacons;
                doLiveLocation=true;

            }
        });

        this.ipcCallbacks= new IPCCallbacks() {
            @Override
            public void onError(JSONObject jsonObject) {
                logger.severe(jsonObject.toString());
                final JSONObject localObj = jsonObject;
                VLCIndoorLocation.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        VLCIndoorLocation.this.dispatchOnProviderError(new Error(localObj.toString()));
                    }
                });
                //VLCIndoorLocation.this.dispatchOnProviderError(new Error(jsonObject.toString()));
            }

            @Override
            public void onProcessStopped(JSONObject jsonObject) {
                started = false;
                VLCIndoorLocation.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        VLCIndoorLocation.this.dispatchOnProviderStopped();
                    }
                });
                //VLCIndoorLocation.this.dispatchOnProviderStopped();
            }

            @Override
            public void onProcessStarted(JSONObject jsonObject) {
                started = true;
                VLCIndoorLocation.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        VLCIndoorLocation.this.dispatchOnProviderStarted();
                    }
                });
                //VLCIndoorLocation.this.dispatchOnProviderStarted();

            }

            @Override
            public void onNewMessage(JSONObject jsonObject) {
                logger.severe("XME : NEW MESSAGE");
                try {
                    String newID=jsonObject.getString("data");
                    // Do the filtering
                    if (vlcId!=null&&(newID.startsWith(vlcId.substring(0, RAW_MSG_LENGTH))))
                        return;
                    //  END OF FILTERING */
                    if (newID.startsWith("0x") && newID.length()>=RAW_MSG_LENGTH)
                        vlcId=newID.substring(0, RAW_MSG_LENGTH);

                } catch (JSONException e) {
                    VLCIndoorLocation.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            VLCIndoorLocation.this.dispatchOnProviderError(new Error("Empty VLC data"));
                        }
                    });
                    //VLCIndoorLocation.this.dispatchOnProviderError(new Error(e));
                    return;
                }
                // get the location from the location service
                final IndoorLocation localLocation;
                if(doLiveLocation)
                    localLocation=getLocationFromMapwize(vlcId,VLCIndoorLocation.this.vlcApiKey);
                else
                    localLocation=  VLCIndoorLocation.this.getHardLocation(vlcId, VLCIndoorLocation.this.vlcApiKey);
                if (localLocation==null) return;
                if ( localLocation.getLatitude() != lastLocation.getLatitude()
                        || localLocation.getLongitude() != lastLocation.getLongitude()
                        || localLocation.getFloor() != lastLocation.getFloor())
                {
                    lastLocation.setLatitude(localLocation.getLatitude());
                    lastLocation.setLongitude(localLocation.getLongitude());
                    lastLocation.setFloor(localLocation.getFloor());
                    lastLocation.setTime(localLocation.getTime());

                    VLCIndoorLocation.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            VLCIndoorLocation.this.dispatchIndoorLocationChange(localLocation);
                        }
                    });
                    //VLCIndoorLocation.this.dispatchIndoorLocationChange(localLocation);
                }else{
                    lastLocation.setLatitude(localLocation.getLatitude());
                    lastLocation.setLongitude(localLocation.getLongitude());
                    lastLocation.setFloor(localLocation.getFloor());
                    lastLocation.setTime(localLocation.getTime());
                }
            }
        };

    }

    private IPCCallbacks ipcCallbacks ;

    public static String getStatus() {
        return Sequencer.getStatus();
    }
    public static String getVersion() {
        return Sequencer.getEngineVersion();
    }

    @Override
    public boolean supportsFloor() {
        return true;
    }

    @Override
    public void start() {
        //Sequencer.camBack(application.getApplicationContext());
        //logger.severe("XME : STARTED VLC!!");

        Sequencer.start(application.getApplicationContext(), ipcCallbacks,"camback", 100);
    }

    @Override
    public void stop() {
        //logger.severe("XME : STOPPED VLC!!!");
        Sequencer.stop(application.getApplicationContext());
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    private IndoorLocation getLocationFromMapwize(String idVLC,String key){
        IndoorLocation newLocation = new IndoorLocation(lastLocation,lastLocation.getFloor());
        boolean found = false;
        VLCBeaconListRetriever.VLCBeacon current;
        Iterator<VLCBeaconListRetriever.VLCBeacon> it = vlcKnownLocation.iterator();
        while (it.hasNext()) {

            current = it.next();
            //logger.severe(current.alias);
            if (current.properties.lightId.length()>=RAW_MSG_LENGTH &&
                    idVLC.equals(current.properties.lightId.substring(0,RAW_MSG_LENGTH))) {
                newLocation.setFloor(current.floor);
                newLocation.setLatitude(current.location.lat);
                newLocation.setLongitude(current.location.lon);
                newLocation.setTime(System.currentTimeMillis());
                newLocation.setAccuracy(0f);
                newLocation.setBearing(0f);
                found=true;
                break;
            }
        }
        if (found) return newLocation;
        else return null;
    }
    private IndoorLocation getHardLocation(String idVLC, String key){
        // TODO 19/02/2018 : replace these lines with an http request
        //logger.severe("XME: New vlc message :: "+idVLC);

        IndoorLocation newLocation = new IndoorLocation(lastLocation,lastLocation.getFloor());
        if (idVLC.startsWith("0x71")){
            newLocation.setLatitude(48.887988338992166);
            newLocation.setLongitude(2.168635725975037);
            newLocation.setTime(System.currentTimeMillis());
            newLocation.setAccuracy(0f);
            newLocation.setBearing(0f);
            newLocation.setFloor(7.0);
            //logger.severe("XME : Xavier Desk");
        }else if (idVLC.startsWith("0x68")) {
            newLocation.setBearing(50f);
            newLocation.setLatitude(48.8880923937327);
            newLocation.setLongitude(2.1684533357620244);
            newLocation.setAccuracy(50f);
            newLocation.setTime(System.currentTimeMillis());
            newLocation.setFloor(7.0);
            //logger.severe("XME : Meeting room");
        }
        return newLocation;
    }
}
