package net.mootoh.trackthings;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by takayama.motohiro on 7/17/14.
 */
public class TrackApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_key));
    }
}
