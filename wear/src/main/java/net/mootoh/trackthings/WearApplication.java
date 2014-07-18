package net.mootoh.trackthings;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by takayama.motohiro on 7/18/14.
 */
public class WearApplication extends Application implements MessageApi.MessageListener {
    private static final String TAG = "WearApplication";
    private static final String SHOW_DAILY_SUMMARY_PATH = "/show_daily_summary";

    private GoogleApiClient googleApiClient_;

    @Override
    public void onCreate() {
        super.onCreate();
        final WearApplication self = this;

        googleApiClient_ = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "connected. addListener...");
                        Wearable.MessageApi.addListener(googleApiClient_, self);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "connection suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "connection failed");
                    }
                })
                .build();
        googleApiClient_.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived");
        if (messageEvent.getPath().equals(SHOW_DAILY_SUMMARY_PATH)) {
            Intent startIntent = new Intent(this, DailySummaryActivity.class);
            String jsonStr = new String(messageEvent.getData());
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("json", jsonStr);
            Log.d(TAG, "jsonStr = " + jsonStr);
            startActivity(startIntent);
        }
    }

    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient_;
    }
}
