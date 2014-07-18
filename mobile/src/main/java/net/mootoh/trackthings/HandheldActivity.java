package net.mootoh.trackthings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

class HandheldAdapter extends BaseAdapter {
    private final List<Map<String, Object>> history;
    private final Activity activity;

    HandheldAdapter(List<Map<String, Object>> history, final Activity activity) {
        this.history = history;
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return history.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Map<String, Object> item = history.get(position);

        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            v = inflater.inflate(R.layout.history_item_layout, null);
        }

        TextView nm = (TextView)v.findViewById(R.id.name);
        nm.setText((String)item.get("thing"));

        String duration = calcDuration((Long)item.get("duration"));

        TextView du = (TextView)v.findViewById(R.id.duration);
        du.setText(duration);

        return v;
    }

    static public String calcDuration(long diff) {
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours   = diff / (60 * 60 * 1000) % 60;

        String ret = "" + diffHours + ":" + diffMinutes + ":" + diffSeconds;
        return ret;
    }
}

public class HandheldActivity extends Activity {
    private static final String TAG = "HandheldActivity";
    private static final String SHOW_DAILY_SUMMARY_PATH = "/show_daily_summary";
    List<Map<String, Object>> history = new ArrayList<Map<String, Object>>();
    Map<String, Object> current;
    private GoogleApiClient googleApiClient_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handheld);

        ListView lv = (ListView)findViewById(R.id.listView);
        lv.setAdapter(new HandheldAdapter(history, this));

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        downloadFromParse();
        googleApiClient_.connect();
    }

    private void downloadFromParse() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Context");
        query.orderByAscending("createdAt");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "failed in retrieving data from Parse.com");
                    return;
                }
                updateHistory(parseObjects);
                updateTitle();
            }
        });
    }

    private void updateTitle() {
        if (current == null) {
            this.setTitle("Idle");
            return;
        }

        this.setTitle(current.get("thing") + ": " + HandheldAdapter.calcDuration((Long)current.get("duration")));
    }

    private void updateHistory(List<ParseObject> parseObjects) {
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

        ListView lv = (ListView)findViewById(R.id.listView);
        HandheldAdapter adapter = (HandheldAdapter)lv.getAdapter();
        adapter.notifyDataSetChanged();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.handheld, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_daily_summary) {
            showDailySummary();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    private void showDailySummary() {
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
    }
}