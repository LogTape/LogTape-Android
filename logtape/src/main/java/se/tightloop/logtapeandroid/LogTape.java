package se.tightloop.logtapeandroid;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.squareup.seismic.ShakeDetector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import se.tightloop.logtapeandroid.MessageLogEvent;
import se.tightloop.logtapeandroid.ObjectLogEvent;
import se.tightloop.logtapeandroid.RequestStartedLogEvent;
import se.tightloop.logtapeandroid.RequestLogEvent;
import se.tightloop.logtapeandroid.LogEvent;

/**
 * Created by dnils on 09/10/16.
 */

public class LogTape {
    static Bitmap lastScreenshot = null;

    private static final int MaxNumEvents = 50;
    private static LogTape instance = null;
    private WeakReference<Activity> currentActivity;
    private ShakeDetector shakeDetector = null;
    private String apiKey;
    private File directory;
    private ShakeInterceptor shakeInterceptor = new ShakeInterceptor();

    private Context getActivityContext() {
        return currentActivity.get();
    }

    private class ShakeInterceptor implements ShakeDetector.Listener {
        public void hearShake() {
            LogTape.ShowReportActivity();
        }
    }

    private LogTape(Application application, String apiKey) {
        this.apiKey = apiKey;

        final ShakeDetector detector = new ShakeDetector(this.shakeInterceptor);
        this.shakeDetector = detector;
        SensorManager manager = (SensorManager)application.getSystemService(Context.SENSOR_SERVICE);
        this.shakeDetector.start(manager);

        directory = application.getDir("logtape", Context.MODE_PRIVATE);

        if (!directory.exists()) {
            boolean result = directory.mkdirs();
            if (!result) {
                Log.e("LogTape", "Could not create LogTape data directory!");
                directory = null;
            } else {

            }
        }
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

    // Note: Should always run on background thread
    public static void SaveScreenshotToDisk() {
        if (instance == null || LogTape.lastScreenshot == null) {
            return;
        }

        File outputFile = new File(instance.directory, "screenshot.png");
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(outputFile);
            LogTape.lastScreenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void ClearLog() {
        if (instance != null) {
            instance.clearLog();
        }
    }

    public static void ShowReportActivity() {
        if (instance == null) {
            return;
        }

        final Activity activity = instance.currentActivity.get();

        if (activity == null || activity instanceof ReportIssueActivity) {
            return;
        }

        final Bitmap lastScreenshot = LogTapeUtil.getScreenShot(activity.getWindow().getDecorView().getRootView());

        LogTape.lastScreenshot = lastScreenshot;

        final ProgressDialog progress = ProgressDialog.show(activity, "Saving screenshot..",
                "", true);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                LogTape.SaveScreenshotToDisk();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                progress.dismiss();
                Intent intent = new Intent(activity.getApplicationContext(), ReportIssueActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
        }.execute();
    }

    public static void LogObject(String tag, String message, JSONObject object) {
        if (instance != null) {
            Map<String, String> tags = null;

            if (tag != null) {
                tags = new HashMap<String, String>();
                tags.put(tag, "info");
            }

            LogObject(message, object, tags);
        }
    }
    public static void LogObject(String message, JSONObject object, Map<String, String> tags) {
        if (instance != null) {
            instance.saveLogToDisk(new ObjectLogEvent(message, object, tags));
        }
    }

    public static void Log(String msg) {
        Map<String, String> nullRef = null;
        LogTape.Log(msg, nullRef);
    }

    public static void Log(String tag, String msg) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(tag, "info");
        LogTape.Log(msg, tags);
    }

    public static void Log(String message, Map<String, String> tags) {
        if (instance != null) {
            instance.saveLogToDisk(new MessageLogEvent(message, tags));
        }
    }

    public static Object LogRequestStart(String url,
                                         String method,
                                         Map<String, String> requestHeaders,
                                         byte[] body,
                                         Map<String, String> tags)
    {
        if (instance == null) {
            return null;
        }

        RequestStartedLogEvent ret = new RequestStartedLogEvent(new Date(), url, method, requestHeaders, body, tags);
        instance.saveLogToDisk(ret);

        return ret;
    }


    public static void LogRequestFinished(Object startEvent,
                                          int httpStatusCode,
                                          String httpStatusText,
                                          Map<String, String> responseHeaders,
                                          byte[] responseBody,
                                          String errorText,
                                          Map<String, String> tags)
    {
        RequestStartedLogEvent reqStartedEvent = (RequestStartedLogEvent)startEvent;

        if (instance != null) {
            LogEvent ev = new RequestLogEvent(new Date(), reqStartedEvent, httpStatusCode, httpStatusText, responseHeaders, responseBody, errorText, tags);
            instance.saveLogToDisk(ev);
        }
    }

    private class WriteTaskData {
        public LogEvent event;
        public File directory;
    }

    private class ListTask extends AsyncTask<File, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(File... directories) {
            JSONArray ret = new JSONArray();

            for (int i = 0; i < directories.length; i++) {
                File directory = directories[i];
                File[] files = directory.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".txt");
                    }
                });

                Arrays.sort(files, new Comparator()
                {
                    @Override
                    public int compare(Object f1, Object f2) {
                        return ((File) f1).getName().compareTo(((File) f2).getName());
                    }
                });

                for (File file : files) {
                    try {
                        InputStream is = new BufferedInputStream(new FileInputStream(file));
                        int size = is.available();
                        byte[] buffer = new byte[size];
                        is.read(buffer);
                        String json = new String(buffer, "UTF-8");
                        JSONObject obj = new JSONObject(json);
                        ret.put(obj);
                    } catch (Exception e) {

                    }
                }
            }

            return ret;
        }
    }

    private class WriteTask extends AsyncTask<WriteTaskData, Void, Void> {
        @Override
        protected Void doInBackground(WriteTaskData... logEvents) {

            for (int i = 0; i < logEvents.length; i++) {
                WriteTaskData data = logEvents[i];
                String filename = String.valueOf(data.event.timestamp.getTime()) + "_" + data.event.id + ".txt";
                File outputFile = new File(data.directory, filename);
                try {
                    FileWriter writer = new FileWriter(outputFile);
                    writer.write(data.event.toJSON().toString());
                    writer.close();
                } catch(Exception e) {
                    System.err.println("Failed to write log file to disk: " + e.toString());
                }
            }

            return null;
        }
    }

    private class CleanupTask extends AsyncTask<File, Void, Void> {
        boolean clearAll = false;

        @Override
        protected Void doInBackground(File... directories) {

            for (File directory : directories) {
                File[] files = directory.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".txt");
                    }
                });

                Arrays.sort(files, new Comparator()
                {
                    @Override
                    public int compare(Object f1, Object f2) {
                        // Reversed to get oldest first
                        return ((File) f2).getName().compareTo(((File) f1).getName());
                    }
                });



                int start = LogTape.MaxNumEvents;

                if (clearAll) {
                    start = 0;
                }

                for (int i = start; i < files.length; i++) {
                    files[i].delete();
                }
            }

            return null;
        }
    }

    private void saveLogToDisk(LogEvent event) {
        WriteTask writeTask = new WriteTask();
        WriteTaskData data = new WriteTaskData();
        data.event = event;
        data.directory = directory;
        writeTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, data);
        CleanupTask cleanTask = new CleanupTask();
        cleanTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, directory);
    }

    private void listEvents(final IssueListResultListener listener) {
        ListTask listTask = new ListTask() {
            @Override
            protected void onPostExecute(JSONArray jsonArray) {
                listener.onResultReceived(jsonArray);
            }
        };
        listTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, directory);
    }

    private void clearLog() {
        CleanupTask cleanTask = new CleanupTask();
        cleanTask.clearAll = true;
        cleanTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, directory);
    }


    interface IssueListResultListener {
        public void onResultReceived(JSONArray result);
    }

    // Should always run on background thread
    static Bitmap LoadScreenshotFromDisk() {
        if (instance == null) {
            return null;
        }
        File pngLocation = new File(instance.directory, "screenshot.png");
        Bitmap ret = BitmapFactory.decodeFile(pngLocation.getAbsolutePath());

        if (ret != null) {
            return ret.copy(Bitmap.Config.ARGB_8888, true);
        } else {
            return null;
        }
    }


    static String ApiKey() {
        if (instance != null) {
            return instance.apiKey;
        } else {
            return "";
        }
    }

    static void GetJSONItems(IssueListResultListener listener) {
        if (instance != null) {
            instance.listEvents(listener);
        } else {
            listener.onResultReceived(new JSONArray());
        }
    }
}
