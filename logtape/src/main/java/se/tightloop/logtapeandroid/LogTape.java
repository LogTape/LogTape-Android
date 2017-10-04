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
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dnils on 09/10/16.
 */

public class LogTape {
    static Bitmap lastScreenshot = null;

    private static final int MaxNumEvents = 100;
    private static LogTape instance = null;
    private WeakReference<Activity> currentActivity;
    private ShakeDetector shakeDetector = null;
    private String apiKey;
    private File directory;
    private ShakeInterceptor shakeInterceptor = new ShakeInterceptor();
    private boolean showing = false;
    PropertySupplier propertySupplier = null;

    public interface PropertySupplier {
        void populate(JSONArray items);
    }

    private Comparator<File> sortAlgorithm = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            String s1 = f1.getName();
            String s2 = f2.getName();

            String[] s1_components = s1.split("_");
            String[] s2_components = s2.split("_");

            if (s1_components.length >= 2 && s2_components.length >= 2) {
                Long i1 = Long.parseLong(s1_components[0]);
                Long i2 = Long.parseLong(s2_components[0]);


                if (i1 == i2) {
                    i1 = Long.parseLong(s1_components[1]);
                    i2 = Long.parseLong(s2_components[1]);
                }

                return i1.compareTo(i2);
            } else {
                return s1.compareTo(s2);
            }
        }
    };

    private Context getActivityContext() {
        return currentActivity.get();
    }

    private class ShakeInterceptor implements ShakeDetector.Listener {
        public void hearShake() {
            LogTape.showReportActivity();
        }
    }

    private LogTape(Application application, String apiKey, LogTapeOptions options) {
        if (options == null) {
            options = new LogTapeOptions();
        }

        this.apiKey = apiKey;

        if (options.trigger == LogTapeOptions.Trigger.ShakeGesture) {
            this.shakeDetector = new ShakeDetector(this.shakeInterceptor);
            SensorManager manager = (SensorManager)application.getSystemService(Context.SENSOR_SERVICE);
            this.shakeDetector.start(manager);
        }

        final ShakeDetector detector = this.shakeDetector;

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
                if (detector != null) {
                    detector.stop();
                }

                LogTape.instance.currentActivity = new WeakReference<>(activity);

                if (detector != null) {
                    detector.start((SensorManager)activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE));
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (detector != null) {
                    detector.stop();
                }

                LogTape.instance.currentActivity = new WeakReference<>(activity);
                if (detector != null) {
                    detector.start((SensorManager)activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE));
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                if (detector != null) {
                    detector.stop();
                }
                LogTape.instance.currentActivity = new WeakReference<>(activity);

                if (detector != null) {
                    detector.start((SensorManager)activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE));
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (detector != null) {
                    detector.stop();
                }
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

    public static void init(int apiKeyId, Application application, LogTapeOptions options) {
        init(application.getResources().getString(apiKeyId), application, options);
    }

    public static void init(String apiKey, Application application, LogTapeOptions options) {
        if (apiKey == null || apiKey == "") {
            return;
        } else if (instance != null) {
            instance.apiKey = apiKey;
        } else {
            instance = new LogTape(application, apiKey, options);
        }
    }

    // Note: Should always run on background thread
    public static void saveScreenshotToDisk() {
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

    public static void clearLog() {
        if (instance != null) {
            instance.clearLogFiles();
        }
    }

    public static void showReportActivity() {
        if (instance == null || instance.showing) {
            return;
        }

        final Activity activity = instance.currentActivity.get();

        if (activity == null || activity instanceof ReportIssueActivity) {
            return;
        }

        instance.showing = true;

        final ProgressDialog progress = ProgressDialog.show(activity, "Saving screenshot..",
                "", true);

        final Bitmap lastScreenshot = LogTapeUtil.getScreenShot(activity.getWindow().getDecorView().getRootView());
        LogTape.lastScreenshot = lastScreenshot;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                LogTape.saveScreenshotToDisk();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                progress.dismiss();
                Intent intent = new Intent(activity.getApplicationContext(), ReportIssueActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                instance.showing = false;
            }
        }.execute();
    }

    public static void logObject(String tag, String message, JSONObject object) {
        if (instance != null) {
            Map<String, String> tags = null;

            if (tag != null) {
                tags = new HashMap<String, String>();
                tags.put(tag, "info");
            }

            logObject(message, object, tags);
        }
    }

    public static void logObject(String message, JSONObject object, Map<String, String> tags) {
        if (instance != null) {
            instance.saveLogToDisk(new ObjectLogEvent(message, object, tags));
        }
    }

    public static void Log(String msg) {
        Map<String, String> nullRef = null;
        LogTape.log(msg, nullRef);
    }

    public static void log(String tag, String msg) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(tag, "info");
        LogTape.log(msg, tags);
    }

    public static void log(String message, Map<String, String> tags) {
        if (instance != null) {
            instance.saveLogToDisk(new MessageLogEvent(message, tags));
        }
    }

    public static void logHttpURLConnectionRequest(LogTapeDate startDate,
                                                   Map<String, List<String>> requestHeaders,
                                                   HttpURLConnection connection,
                                                   byte[] body,
                                                   byte[] responseBody,
                                                   Map<String, String> tags)
    {
        if (instance != null) {
            try {
                logRequest(startDate,
                        connection.getURL().toString(),
                        connection.getRequestMethod(),
                        LogTapeUtil.multiValueMapToSingleValueMap(requestHeaders),
                        body,
                        connection.getResponseCode(),
                        connection.getResponseMessage(),
                        LogTapeUtil.multiValueMapToSingleValueMap(connection.getHeaderFields()),
                        responseBody, "", tags, true);
            } catch (IOException e) {

            }

        }
    }


    public static void logRequest(LogTapeDate startDate,
                                  String url,
                                  String method,
                                  Map<String, String> requestHeaders,
                                  byte[] body,
                                  int httpStatusCode,
                                  String httpStatusText,
                                  Map<String, String> responseHeaders,
                                  byte[] responseBody,
                                  String errorText,
                                  Map<String, String> tags,
                                  boolean logStartEvent)
    {
        if (instance != null) {
            RequestStartedLogEvent req = new RequestStartedLogEvent(startDate, url, method, requestHeaders, body, tags);

            if (logStartEvent) {
                instance.saveLogToDisk(req);
            }

            LogEvent res = new RequestLogEvent(new LogTapeDate(), req, httpStatusCode, httpStatusText, responseHeaders, responseBody, errorText, tags);
            instance.saveLogToDisk(res);
        }
    }

    public static Object logRequestStart(String url,
                                         String method,
                                         Map<String, String> requestHeaders,
                                         byte[] body,
                                         Map<String, String> tags)
    {
        if (instance == null) {
            return null;
        }

        RequestStartedLogEvent ret = new RequestStartedLogEvent(new LogTapeDate(), url, method, requestHeaders, body, tags);
        instance.saveLogToDisk(ret);

        return ret;
    }


    public static void logRequestFinished(Object startEvent,
                                          int httpStatusCode,
                                          String httpStatusText,
                                          Map<String, String> responseHeaders,
                                          byte[] responseBody,
                                          String errorText,
                                          Map<String, String> tags)
    {
        RequestStartedLogEvent reqStartedEvent = (RequestStartedLogEvent)startEvent;

        if (instance != null) {
            LogEvent ev = new RequestLogEvent(new LogTapeDate(), reqStartedEvent, httpStatusCode, httpStatusText, responseHeaders, responseBody, errorText, tags);
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

                Arrays.sort(files, sortAlgorithm);

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
                String filename = String.valueOf(data.event.timestamp.date.getTime()) + "_" + data.event.timestamp.index + "_" + data.event.id + ".txt";
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

                Arrays.sort(files, sortAlgorithm);
                Collections.reverse(Arrays.asList(files));

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

    private void clearLogFiles() {
        CleanupTask cleanTask = new CleanupTask();
        cleanTask.clearAll = true;
        cleanTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, directory);
    }


    interface IssueListResultListener {
        public void onResultReceived(JSONArray result);
    }

    // Should always run on background thread
    static Bitmap loadScreenshotFromDisk() {
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


    static String apiKey() {
        if (instance != null) {
            return instance.apiKey;
        } else {
            return "";
        }
    }

    static void getJSONItems(IssueListResultListener listener) {
        if (instance != null) {
            instance.listEvents(listener);
        } else {
            listener.onResultReceived(new JSONArray());
        }
    }


    public static void setPropertySupplier(PropertySupplier supplier) {
        if (instance != null) {
            instance.propertySupplier = supplier;
        }
    }

    static PropertySupplier getPropertySupplier() {
        if (instance != null) {
            return instance.propertySupplier;
        } else {
            return null;
        }
    }

    public static boolean isEnabled() {
        return instance != null;
    }
}
