package net.mootoh.trackthings;

import android.app.Activity;
import android.content.Context;
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

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    String calcDuration(long diff) {
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours   = diff / (60 * 60 * 1000) % 60;

        String ret = "" + diffHours + ":" + diffMinutes + ":" + diffSeconds;
        return ret;
    }
}

public class HandheldActivity extends Activity {
    private static final String TAG = "HandheldActivity";
    List<Map<String, Object>> history = new ArrayList<Map<String, Object>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handheld);

        Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_key));

        ListView lv = (ListView)findViewById(R.id.listView);
        lv.setAdapter(new HandheldAdapter(history, this));
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
                updateHistory(parseObjects);
            }
        });
    }

    private void updateHistory(List<ParseObject> parseObjects) {
        history.clear();

        ParseObject current = parseObjects.get(0);
        int i = 0;
        for (ParseObject po: parseObjects) {
            if (i++ == 0) {
                continue;
            }

            String thing = (String) current.get("thing");
            if (thing.equals("stop") || thing.equals("ストップ")) {
                // ignore idle time
                current = po;
                continue;
            }

            Date first = (Date)current.get("timestamp");
            Date second = (Date)po.get("timestamp");
            long diff = second.getTime() - first.getTime();

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("thing", current.get("thing"));
            item.put("duration", diff);
            history.add(item);

            current = po;
        }

        String thing = (String) current.get("thing");
        if (!thing.equals("stop") && !!thing.equals("ストップ")) {
            Date currentTimestamp = (Date)current.get("timestamp");
            Date now = new Date();
            long diff = now.getTime() - currentTimestamp.getTime();

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("thing", current.get("thing"));
            item.put("duration", diff);
            history.add(item);
        }

        Collections.reverse(history);

        ListView lv = (ListView)findViewById(R.id.listView);
        HandheldAdapter adapter = (HandheldAdapter)lv.getAdapter();
        adapter.notifyDataSetChanged();
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
        }
        return super.onOptionsItemSelected(item);
    }

}