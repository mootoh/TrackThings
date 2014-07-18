package net.mootoh.trackthings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DailySummaryActivity extends Activity {

    private static final String TAG = "DailySummary";
    Map<String, Integer> history;
    ArrayList<Map.Entry<String, Integer>> items = new ArrayList<Map.Entry<String, Integer>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final DailySummaryActivity self = this;
        setContentView(R.layout.activity_daily_summary);
        ListView wlv = (ListView)findViewById(R.id.activity_daily_view);
        wlv.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size();
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
                Map.Entry<String, Integer> item = (Map.Entry<String, Integer>) items.get(position);

                View v = convertView;

                if (v == null) {
                    LayoutInflater inflater = self.getLayoutInflater();
                    v = inflater.inflate(R.layout.history_item_layout, null);
                }

                TextView nm = (TextView)v.findViewById(R.id.name);
                nm.setText(item.getKey());

                Integer duration = item.getValue();
                TextView du = (TextView)v.findViewById(R.id.duration);
                du.setText(duration.toString());

                return v;
            }
        });


        setTitle("Daily Summary");

        Intent intent = getIntent();
        String jsonStr = intent.getStringExtra("json");
        try {
            JSONObject obj = new JSONObject(jsonStr);
            Log.d(TAG, "json = " + obj);
            Map<String, Integer> map = new HashMap<String, Integer>();
            Iterator<?> keys = obj.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                map.put(key, (Integer)obj.get(key));
            }
            history = map;
            Log.d(TAG, "history = " + history);

            Iterator it = history.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) it.next();
                items.add(entry);
            }

            BaseAdapter ba = (BaseAdapter)wlv.getAdapter();
            ba.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

