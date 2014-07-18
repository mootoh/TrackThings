package net.mootoh.trackthings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TrackAdapter extends WearableListView.Adapter {
    private static final String TAG = "TrackAdapter";
    private final LayoutInflater layoutInflater_;
    String[] baseItems = {"Say...", "Stop"};
    List<String> items = new ArrayList<String>();

    public TrackAdapter(Context context) {
        super();
        layoutInflater_ = LayoutInflater.from(context);

        SharedPreferences pref = context.getSharedPreferences(context.getString(R.string.SHARED_PREF_KEY), Context.MODE_PRIVATE);
        int count = pref.getInt("contextCount", 0);
        for (int i=0; i<count; i++) {
            String item = pref.getString("context_" + i, "");
            items.add(item);
        }
    }

    public List<String> getItems() {
        return items;
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new WearableListView.ViewHolder(layoutInflater_.inflate(R.layout.track_item_layout, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int i) {
        TextView view = (TextView)holder.itemView.findViewById(R.id.name);

        String item;
        if (i < 2)
            item = baseItems[i];
        else if (i >= 2 + items.size()) {
            item = "Summary...";
        }
        else
            item = items.get(i-2);
        view.setText(item);
        holder.itemView.setTag(i);
    }

    @Override
    public int getItemCount() {
        return items.size() + 3;
    }

    public void choose(int position) {
        if (position < 2 || position >= 2 + items.size())
            return;
        position -= 2;

        // move the chosen item to the first place
        String chosen = items.remove(position);
        items.add(0, chosen);
    }

    public void add(String newContext) {
        if (items.contains(newContext)) {
            choose(items.indexOf(newContext) + 2);
            return;
        }

        items.remove(items.size()-1);
        items.add(0, newContext);
    }
}

public class TrackActivity extends Activity {
    private static final int SPEECH_REQUEST_CODE = 2;
    private static final String TAG = "TrackActivity";
    TrackAdapter adapter_;
    String currentContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPrefixedItemsUnlessExist();
        setContentView(R.layout.activity_track);

        SharedPreferences pref = getSharedPreferences(getString(R.string.SHARED_PREF_KEY), Context.MODE_PRIVATE);
        currentContext = pref.getString("currentContext", "Idle");
        TextView cts = (TextView)findViewById(R.id.current_context);
        cts.setText(currentContext);

        final TrackActivity self = this;

        WearableListView wlv = (WearableListView)findViewById(R.id.listView);
        adapter_ = new TrackAdapter(this);
        wlv.setAdapter(adapter_);

        wlv.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(WearableListView.ViewHolder holder) {
                Integer position = (Integer)holder.itemView.getTag();
                if (position == -1) { // header
                    return;
                }
                if (position == 0) { // say
                    displaySpeechRecognizer();
                    return;
                } else if (position == 1) { // stop
                    stopCurrentContext();
                    updateCurrentContext("Idle");
                    return;
                } else if (position == 2 + adapter_.getItems().size()) {
                    kickHandheldToOpenSummary();
                    return;
                }
                TextView tv = (TextView)holder.itemView.findViewById(R.id.name);
                String text = tv.getText().toString();
                adapter_.choose(position);
                updateCurrentContext(text);
                saveItems();
                sendTrackingContext(text);
            }

            @Override
            public void onTopEmptyRegionClick() {
            }
        });
    }

    private void kickHandheldToOpenSummary() {
        Intent intent = new Intent(this, TrackService.class);
        intent.setAction(TrackService.ACTION_SHOW_SUMMARY);
        startService(intent);
        finish();
    }

    private void stopCurrentContext() {
        Intent intent = new Intent(this, TrackService.class);
        intent.setAction(TrackService.ACTION_STOP_CONTEXT);
        startService(intent);
        finish();
    }

    private void saveItems() {
        SharedPreferences pref = getSharedPreferences(getString(R.string.SHARED_PREF_KEY), Context.MODE_PRIVATE);
        saveItemsInternal(pref.edit(), adapter_.getItems());
    }

    private void saveItemsInternal(SharedPreferences.Editor editor, List<String> items) {
        for (int i=0; i<items.size(); i++) {
            editor.putString("context_" + i, items.get(i));
        }
        editor.putInt("contextCount", items.size());
        editor.commit();
    }

    private void loadPrefixedItemsUnlessExist() {
        SharedPreferences pref = getSharedPreferences(getString(R.string.SHARED_PREF_KEY), Context.MODE_PRIVATE);
        int count = pref.getInt("contextCount", 0);
        if (count > 0)
            return;

        final String[] prefixedItems = {"Work", "Book", "Family"};
        saveItemsInternal(pref.edit(), Arrays.asList(prefixedItems));
    }

    private void kickService(String message) {
        Intent intent = new Intent(this, TrackService.class);
        intent.putExtra("context", message);
        intent.setAction(TrackService.ACTION_SEND_CONTEXT);
        startService(intent);
    }

    private void sendTrackingContext(String text) {
        Log.d(TAG, "send tracking context:" + text);
        kickService(text);
        finish();
    }

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            adapter_.add(spokenText);
            updateCurrentContext(spokenText);
            saveItems();
            sendTrackingContext(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void updateCurrentContext(String current) {
        SharedPreferences pref = getSharedPreferences(getString(R.string.SHARED_PREF_KEY), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("currentContext", current);
        editor.commit();
    }
}