package net.mootoh.trackthings;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.parse.Parse;
import com.parse.ParseObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by takayama.motohiro on 7/17/14.
 */
public class TrackApplication extends Application {
    private static final String TAG = "TrackApplication";
    private static final String SHOW_DAILY_SUMMARY_PATH = "/show_daily_summary";
    private GoogleApiClient googleApiClient_;
    List<Map<String, Object>> history = new ArrayList<Map<String, Object>>();
    HashMap<String, Object> current;

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_key));

        googleApiClient_ = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "connected to google api");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "connection suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.e(TAG, "google api connection failed: " + connectionResult.toString());
                    }
                }).build();
        googleApiClient_.connect();
    }

    public List<Map<String, Object>> getHistory() {
        return history;
    }

    public void updateHistory(List<ParseObject> parseObjects) {
        history.clear();

        for (ParseObject po: parseObjects) {

            String thing = (String)po.get("thing");
            Date createdAt = po.getCreatedAt();
            Date finishedAt = (Date)po.get("finishedAt");
            if (finishedAt == null) { // current task
                Date now = new Date();
                long diff = now.getTime() - createdAt.getTime();
                current = new HashMap<String, Object>();
                current.put("thing", thing);
                current.put("duration", diff);
                continue;
            }

            long diff = finishedAt.getTime() - createdAt.getTime();
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("thing", thing);
            item.put("duration", diff);
            history.add(item);
        }

        Collections.reverse(history);
    }

    public HashMap<String, Object> getCurrent() {
        return current;
    }

    // http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
    class ValueComparator implements Comparator<String> {
        Map<String, Long> base;

        public ValueComparator(Map<String, Long> base) {
            this.base = base;
        }

        @Override
        public int compare(String lhs, String rhs) {
            if (base.get(lhs) >= base.get(rhs))
                return -1;
            return 1;
        }
    }

    private Map<String, Long> constructDailySummary() {
        Map<String, Long> items = new HashMap<String, Long>();
        for (Map<String, Object> item : history) {
            String title = (String)item.get("thing");

            Long duration = (Long)items.get(title);
            if (duration == null) {
                duration = new Long(0);
            }
            duration += (Long)item.get("duration");
            items.put(title, duration);
        }

        ValueComparator bvc = new ValueComparator(items);
        TreeMap<String, Long> sortedMap = new TreeMap<String, Long>(bvc);
        sortedMap.putAll(items);;
        return sortedMap;
    }

    public void showDailySummary() {
        Map<String, Long> sortedMap = constructDailySummary();
        Iterator it = sortedMap.entrySet().iterator();

        final JSONObject json = new JSONObject();
        while (it.hasNext()) {
            Map.Entry<String, Long> item = (Map.Entry<String, Long>)it.next();
            try {
                json.put(item.getKey(), item.getValue());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, json.toString());

        Wearable.NodeApi.getConnectedNodes(googleApiClient_).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                Log.d(TAG, "get connected nodes result");
                for (Node node : nodes.getNodes()) {
                    Log.d(TAG, "node " + node.getId());
                    Wearable.MessageApi.sendMessage(googleApiClient_, node.getId(), SHOW_DAILY_SUMMARY_PATH, json.toString().getBytes());
                    return;
                }
            }
        });
    }}
