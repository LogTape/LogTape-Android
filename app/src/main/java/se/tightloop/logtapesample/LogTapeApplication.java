package se.tightloop.logtapesample;

import android.app.Application;
import se.tightloop.logtapeandroid.BuildConfig;

import se.tightloop.logtapeandroid.LogTape;
import se.tightloop.logtapeandroid.R;

/**
 * Created by dnils on 2017-09-16.
 */

public class LogTapeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            LogTape.init(R.string.log_tape_key, this, null);
        }

        System.out.println("**LOGTAPE: Application onCreate");
    }
}
