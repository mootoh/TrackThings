package net.mootoh.trackthings;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.NotificationCompat;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

class TrackAdapter extends WearableListView.Adapter {
    private final Context context_;
    private final LayoutInflater layoutInflater_;
    String[] items = {"Say...", "Work", "Book", "Family"};

    public TrackAdapter(Context context) {
        super();
        context_ = context;
        layoutInflater_ = LayoutInflater.from(context);
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new WearableListView.ViewHolder(layoutInflater_.inflate(R.layout.track_item_layout, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int i) {
        TextView view = (TextView)holder.itemView.findViewById(R.id.name);
        view.setText(items[i]);
        holder.itemView.setTag(i);
    }

    @Override
    public int getItemCount() {
        return items.length;
    }
}

public class TrackActivity extends Activity {
    private static final int SPEECH_REQUEST_CODE = 2;
    private static final String TAG = "TrackActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_track);
        WearableListView wlv = (WearableListView)findViewById(R.id.listView);
        wlv.setAdapter(new TrackAdapter(this));
        wlv.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(WearableListView.ViewHolder holder) {
                Integer position = (Integer)holder.itemView.getTag();
                if (position == 0) {
                    displaySpeechRecognizer();
                    return;
                }

                TextView tv = (TextView)holder.itemView.findViewById(R.id.name);
                String text = tv.getText().toString();
                sendTrackingContext(text);
            }

            @Override
            public void onTopEmptyRegionClick() {
            }
        });
    }

    private void kickService(String message) {
        Intent intent = new Intent(this, TrackService.class);
        intent.putExtra("context", message);
        intent.setAction(TrackService.ACTION_SEND_CONTEXT);
        startService(intent);
        /*
        PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        startService(pi);
        */
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
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
// This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            // Do something with spokenText
            sendTrackingContext(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
