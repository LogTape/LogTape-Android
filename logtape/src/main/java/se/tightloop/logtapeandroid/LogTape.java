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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import se.tightloop.logtapeandroid.events.LogEvent;
import se.tightloop.logtapeandroid.events.MessageLogEvent;
import se.tightloop.logtapeandroid.events.RequestLogEvent;
import se.tightloop.logtapeandroid.events.RequestStartedLogEvent;

/**
 * Created by dnils on 09/10/16.
 */

public class LogTape implements ShakeDetector.Listener {
    private List<LogEvent> events = new ArrayList<LogEvent>();
    private static LogTape instance = null;
    private WeakReference<Activity> currentActivity;

    private ShakeDetector shakeDetector = null;
    String apiKey;
    static Bitmap lastScreenshot = null;

    public Context getActivityContext() {
        return currentActivity.get();
    }

    private LogTape(Application application, String apiKey) {
        this.apiKey = apiKey;

        final ShakeDetector detector = new ShakeDetector(this);
        this.shakeDetector = detector;
        SensorManager manager = (SensorManager)application.getSystemService(Context.SENSOR_SERVICE);
        this.shakeDetector.start(manager);

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                detector.stop();
                LogTape.instance.currentActivity = new WeakReference<>(activity);
                detector.start((SensorManager)activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE));
            }

            @Override
            public void onActivityStarted(Activity activity) {
                detector.stop();
                LogTape.instance.currentActivity = new WeakReference<>(activity);
                detector.start((SensorManager)activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE));
            }

            @Override
            public void onActivityResumed(Activity activity) {
                detector.stop();
                LogTape.instance.currentActivity = new WeakReference<>(activity);
                detector.start((SensorManager)activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE));
            }

            @Override
            public void onActivityPaused(Activity activity) {
                detector.stop();
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

    public static void init(int apiKeyId, Application application) {
        init(application.getResources().getString(apiKeyId), application);
    }

    public static void init(String apiKey, Application application) {
        if (instance != null) {
            instance.apiKey = apiKey;
        } else {
            instance = new LogTape(application, apiKey);
        }
    }

    public static void ShowReportActivity() {
        if (instance == null) {
            return;
        }

        Activity activity = instance.currentActivity.get();

        if (activity == null || activity instanceof ReportIssueActivity) {
            return;
        }

        lastScreenshot = LogTapeUtil.getScreenShot(activity.getWindow().getDecorView().getRootView());

        Intent intent = new Intent(activity.getApplicationContext(), ReportIssueActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    public void hearShake() {

        ShowReportActivity();

    }

    public static void Log(String message) {
        if (instance != null) {
            instance.events.add(new MessageLogEvent(message));
        }
    }

    public static RequestStartedLogEvent LogRequestStart(String url,
                                                         String method,
                                                         Map<String, String> requestHeaders,
                                                         byte[] body)
    {
        if (instance == null) {
            return null;
        }

        RequestStartedLogEvent ret = new RequestStartedLogEvent(new Date(), url, method, requestHeaders, body);
        instance.events.add(ret);
        return ret;
    }

    public static void LogRequestFinished(RequestStartedLogEvent startEvent,
                                          int httpStatusCode,
                                          String httpStatusText,
                                          Map<String, String> responseHeaders,
                                          byte[] responseBody,
                                          String errorText)
    {

        if (instance != null) {
            instance.events.add(new RequestLogEvent(new Date(), startEvent, httpStatusCode, httpStatusText, responseHeaders, responseBody, errorText));
        }
    }

    public static String ApiKey() {
        if (instance != null) {
            return instance.apiKey;
        } else {
            return "";
        }
    }

    public static JSONArray GetJSONItems() {
        JSONArray eventArray = new JSONArray();

        if (instance != null) {
            for (LogEvent e : instance.events) {
                eventArray.put(e.toJSON());
            }
        }

        return eventArray;
    }
}
