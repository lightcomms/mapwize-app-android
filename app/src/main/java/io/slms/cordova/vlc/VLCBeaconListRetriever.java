package io.slms.cordova.vlc;

/**
 * <p>                          mapwize-simple-app-android
 * <p><strong>Description :</strong>
 * <p><strong>File :</strong>       VLCBeaconListRetriever.java
 * <p><strong>Author :</strong>     Xavier Melendez
 * <p><strong>Date :</strong>       23/02/2018
 * <p><strong>Company :</strong>     slms
 */
import android.support.annotation.NonNull;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class VLCBeaconListRetriever {
    private static final String ENDPOINT = "https://api.mapwize.io/v1/beacons?api_key=e2af1248a493cd196fe54b1dbdba8ba8&venueId=5a8b1432c0b1600013546407";
    private static final Moshi MOSHI = new Moshi.Builder().build();
    private static final JsonAdapter<List<VLCBeacon>> CONTRIBUTORS_JSON_ADAPTER = MOSHI.adapter(
            Types.newParameterizedType(List.class, VLCBeacon.class));

    static class VLCBeacon {
        String alias;
        double floor;
        String type;
        Location location;
        Properties properties;
    }
    static class Location{
        double lat;
        double lon;
    }

    static class Properties{
        String lightId;
    }
    public static void go(@NonNull final VLCBeaconCallback vlcBeaconCallback){
        OkHttpClient client = new OkHttpClient();

        // Create request for remote resource.
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .build();

        // Execute the request and retrieve the response.

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                vlcBeaconCallback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                List<VLCBeacon> vlcBeacons = CONTRIBUTORS_JSON_ADAPTER.fromJson(response.body().source());

                Iterator<VLCBeacon> it = vlcBeacons.iterator();
                while (it.hasNext()) {
                    if (!"vlc".equals(it.next().type) ) {
                        it.remove();
                    }
                }
                /*/ Output list of vlcBeacons.
                for (VLCBeacon VLCBeacon : vlcBeacons) {
                    System.out.println(VLCBeacon.alias + ": " +VLCBeacon.type+  VLCBeacon.properties.lightId);
                }  // END    */
                vlcBeaconCallback.onSuccess( vlcBeacons);
            }});

    }
    interface VLCBeaconCallback{
        void onFailure(IOException e);
        void onSuccess(List<VLCBeacon> vlcbeacons);
    }
    private VLCBeaconListRetriever() {
        // No instances.
    }
}