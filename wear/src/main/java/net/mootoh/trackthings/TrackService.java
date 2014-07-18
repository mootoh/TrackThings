package net.mootoh.trackthings;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

public class TrackService extends IntentService {
    public static final String ACTION_SEND_CONTEXT = "ACTION_SEND_CONTEXT";
    public static final String ACTION_STOP_CONTEXT = "ACTION_STOP_CONTEXT";
    private static final String TAG = "TrackService";
    private static final String PATH_CONTEXTS = "/contexts";
    private static final String FIELD_CONTEXT = "context";
    private static final String FIELD_COMMAND = "command";

    public TrackService() {
        super(TrackService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");

        WearApplication app = (WearApplication)getApplication();
        GoogleApiClient client = app.getGoogleApiClient();

        if (! client.isConnected()) {
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

        Wearable.DataApi.putDataItem(client, putDataMapRequest.asPutDataRequest()).await();
    }

}
