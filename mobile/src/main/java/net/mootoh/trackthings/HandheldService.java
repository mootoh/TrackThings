package net.mootoh.trackthings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.WearableListenerService;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.Date;

public class HandheldService extends WearableListenerService {
    private static final String TAG = "HandheldService";
    private static final String FIELD_CONTEXT = "context";
    private static final String FIELD_COMMAND = "command";
    public static final String SHARED_PREF = "TrackThingsSharedPref";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String command = DataMap.fromByteArray(event.getDataItem().getData()).get(FIELD_COMMAND);
                if (command.equals("stop")) {
                    stopCurrent();
                } else if (command.equals("update")) {
                    String context = DataMap.fromByteArray(event.getDataItem().getData()).get(FIELD_CONTEXT);
                    Log.d(TAG, "update the context:" + context);

                    if (sameAsCurrent())
                        return;

                    stopCurrent();
                    saveCurrentContext(context);
                    uploadContext(context);
                }
            }
        }
    }

    private boolean sameAsCurrent() {
        return false;
    }

    private void stopCurrent() {
        Log.d(TAG, "stop the current context");

        // locally
        SharedPreferences pref = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        String current = pref.getString(getString(R.string.current_context), null);
        if (current == null) {
            Log.d(TAG, "already stopped, skipping");
            return;
        }

        SharedPreferences.Editor editor = pref.edit();
        editor.remove(getString(R.string.current_context));
        editor.commit();

        // remotely
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Context");
        query.orderByDescending("createdAt");
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "failed in retrieving data from Parse.com");
                    return;
                }
                parseObject.put("finishedAt", new Date());
                parseObject.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e2) {
                        if (e2 != null) {
                            Log.e(TAG, "failed in saving finishedAt: " + e2.getMessage());
                            return;
                        }
                        Log.d(TAG, "succeeded in saving finishedAt");
                    }
                });
            }
        });
    }

    private void kickMainActivity() {
        Intent intent = new Intent(this, HandheldActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // save the current context locally
    private void saveCurrentContext(String context) {
        SharedPreferences pref = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(getString(R.string.current_context), context);
        editor.putLong(getString(R.string.last_updated), (new Date().getTime()));
        editor.commit();
    }

    // save the current context to a server
    private void uploadContext(String context) {
        ParseObject po = new ParseObject("Context");
        po.put("thing", context);
        po.put("timestamp", new Date());
        po.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                kickMainActivity();
            }
        });
    }
}