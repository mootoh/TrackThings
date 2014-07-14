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
    private static final String TAG = "TrackService";
    private static final String PATH_CONTEXTS = "/contexts";
    private static final String FIELD_CONTEXT = "context";
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
        String context = intent.getStringExtra("context");
        if (context != null) {
            Log.d(TAG, "   --- context to send:" + context);
        }

        googleApiClient_.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "TrackService.onHandleIntent");
        }
        if (googleApiClient_.isConnected()) {
            if (intent.getAction().equals(ACTION_SEND_CONTEXT)) {
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_CONTEXTS);
                putDataMapRequest.getDataMap().putString(FIELD_CONTEXT, context);
                Wearable.DataApi.putDataItem(googleApiClient_, putDataMapRequest.asPutDataRequest()).await();
            }
        }
    }
}
