package se.tightloop.logtapeandroid;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.squareup.seismic.ShakeDetector;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import se.tightloop.logtapeandroid.events.LogEvent;
import se.tightloop.logtapeandroid.events.MessageLogEvent;
import se.tightloop.logtapeandroid.events.RequestLogEvent;

/**
 * Created by dnils on 09/10/16.
 */

public class LogTape implements ShakeDetector.Listener {

    private Application application;
    private List<LogEvent> events = new ArrayList<LogEvent>();
    private static LogTape instance = null;
    private Activity currentActivity;

    private ShakeDetector shakeDetector = null;
    String apiKey;
    static Bitmap lastScreenshot = null;

    public Context getActivityContext() {
        return currentActivity;
    }

    private LogTape(Application application, String apiKey) {
        System.out.println("LogTape created");
        this.application = application;
        this.apiKey = apiKey;

        this.shakeDetector = new ShakeDetector(this);
        SensorManager manager = (SensorManager)application.getSystemService(Context.SENSOR_SERVICE);
        this.shakeDetector.start(manager);

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                LogTape.instance.currentActivity = activity;
            }

            @Override
            public void onActivityStarted(Activity activity) {
                LogTape.instance.currentActivity = activity;
            }

            @Override
            public void onActivityResumed(Activity activity) {
                LogTape.instance.currentActivity = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {
                LogTape.instance.currentActivity = null;
            }

            @Override
            public void onActivityStopped(Activity activity) {
                // don't clear current activity because activity may get stopped after
                // the new activity is resumed
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                // don't clear current activity because activity may get destroyed after
                // the new activity is resumed
            }
        });
    }

    public static void init(String apiKey, Application application) {
        instance = new LogTape(application, apiKey);
    }

    public static void ShowReportActivity() {

        if (instance.currentActivity instanceof ReportIssueActivity) {
            return;
        }

        lastScreenshot = LogTapeUtil.getScreenShot(instance.currentActivity.getWindow().getDecorView().getRootView());

        Intent intent = new Intent(instance.application, ReportIssueActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        instance.application.startActivity(intent);
    }

    public void hearShake() {
        ShowReportActivity();
    }

    public static void Log(String message) {
        instance.events.add(new MessageLogEvent(message));
    }

    public static void LogRequest(Date timestamp,
                                  String url,
                                  String method,
                                  Map<String, String> requestHeaders,
                                  byte[] body,
                                  int httpStatusCode,
                                  String httpStatusText,
                                  Map<String, String> responseHeaders,
                                  byte[] responseBody,
                                  String errorText,
                                  int elapsedTimeMs)
    {
        instance.events.add(new RequestLogEvent(timestamp, url, method, requestHeaders, body, httpStatusCode, httpStatusText, responseHeaders, responseBody, errorText, elapsedTimeMs));
    }

    public static String ApiKey() {
        return instance.apiKey;
    }

    public static JSONArray GetJSONItems() {
        JSONArray eventArray = new JSONArray();

        for (LogEvent e : instance.events) {
            eventArray.put(e.toJSON());
        }

        return eventArray;
    }
}
