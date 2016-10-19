package se.tightloop.logtapesample;

import android.app.Application;

import se.tightloop.logtapeandroid.LogTape;

/**
 * Created by dnils on 09/10/16.
 */

public class LogTapeSampleApplication extends Application {

    @Override
    public void onCreate() {
        LogTape.init("5f559afc-6248-4e90-b9d0-d185e7a9f0d0", this);
    }
}
