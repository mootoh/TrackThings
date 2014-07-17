package net.mootoh.trackthings;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

public class TrackService extends IntentService {
    public static final String ACTION_SEND_CONTEXT = "ACTION_SEND_CONTEXT";
    public static final String ACTION_STOP_CONTEXT = "ACTION_STOP_CONTEXT";
    private static final String TAG = "TrackService";
    private static final String PATH_CONTEXTS = "/contexts";
    private static final String FIELD_CONTEXT = "context";
    private static final String FIELD_COMMAND = "command";

    private GoogleApiClient googleApiClient_;
    private static final long CONNECTION_TIME_OUT_MS = 100;

    public TrackService() {
        super(TrackService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        googleApiClient_ = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                    }
                })
                .build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");

        googleApiClient_.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "TrackService.onHandleIntent");
        }
        if (! googleApiClient_.isConnected()) {
            Log.e(TAG, "Google API client not connected, nothing can do at this moment from wearable");
            return;
        }

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_CONTEXTS);

        String action = intent.getAction();
        if (action.equals(ACTION_SEND_CONTEXT)) {
            String context = intent.getStringExtra("context");
            Log.d(TAG, "   --- context to send:" + context);

            putDataMapRequest.getDataMap().putString(FIELD_COMMAND, "update");
            putDataMapRequest.getDataMap().putString(FIELD_CONTEXT, context);
        } else if (action.equals(ACTION_STOP_CONTEXT)) {
            putDataMapRequest.getDataMap().putString(FIELD_COMMAND, "stop");
        }

        Wearable.DataApi.putDataItem(googleApiClient_, putDataMapRequest.asPutDataRequest()).await();
    }
}
