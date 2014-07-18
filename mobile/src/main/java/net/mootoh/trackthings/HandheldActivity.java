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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handheld);

        TrackApplication app = (TrackApplication)getApplication();

        ListView lv = (ListView)findViewById(R.id.listView);
        lv.setAdapter(new HandheldAdapter(app.getHistory(), this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        downloadFromParse();
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

                TrackApplication app = (TrackApplication)getApplication();
                app.updateHistory(parseObjects);

                ListView lv = (ListView)findViewById(R.id.listView);
                HandheldAdapter adapter = (HandheldAdapter)lv.getAdapter();
                adapter.notifyDataSetChanged();

                updateTitle();
            }
        });
    }

    private void updateTitle() {
        TrackApplication app = (TrackApplication)getApplication();
        HashMap<String, Object> current = app.getCurrent();
        if (current == null) {
            this.setTitle("Idle");
            return;
        }

        this.setTitle(current.get("thing") + ": " + HandheldAdapter.calcDuration((Long)current.get("duration")));
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
            TrackApplication app = (TrackApplication)getApplication();
            app.showDailySummary();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}