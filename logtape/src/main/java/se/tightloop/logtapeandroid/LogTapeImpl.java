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
import android.view.View;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 *  Actual implementation code for LogTape, kept in separate class to keep
 *  public API clean.
 */

class LogTapeImpl {
    static Bitmap lastScreenshot = null;
    static LogTapeImpl instance = null;
    String apiKey;
    static boolean enabled = true;

    private boolean showing = false;
    private static final int MaxNumEvents = 100;
    private WeakReference<Activity> currentActivity;
    private ShakeDetector shakeDetector = null;
    private File directory;
    private ShakeInterceptor shakeInterceptor = new ShakeInterceptor();
    private LogTapePropertySupplier propertySupplier = null;
    private ProgressDialog progress = null;

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

    LogTapeImpl(Application application, String apiKey, LogTapeOptions options) {
        if (options == null) {
            options = new LogTapeOptions();
        }

        this.apiKey = apiKey;

        if (options.trigger == LogTapeOptions.Trigger.ShakeGesture) {
            this.shakeDetector = new ShakeDetector(this.shakeInterceptor);
            SensorManager manager = (SensorManager) application.getSystemService(Context.SENSOR_SERVICE);
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

                currentActivity = new WeakReference<>(activity);

                if (detector != null) {
                    detector.start((SensorManager) activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE));
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (detector != null) {
                    detector.stop();
                }

                currentActivity = new WeakReference<>(activity);
                if (detector != null) {
                    detector.start((SensorManager) activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE));
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                if (detector != null) {
                    detector.stop();
                }
                currentActivity = new WeakReference<>(activity);

                if (detector != null) {
                    detector.start((SensorManager) activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE));
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (detector != null) {
                    detector.stop();
                }
                currentActivity = null;
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (progress != null) {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                }
            }
        });
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
                } catch (Exception e) {
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

                int start = LogTapeImpl.MaxNumEvents;

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

    // Note: Should always run on background thread
    public static void saveScreenshotToDisk() {
        if (LogTapeImpl.instance == null || LogTapeImpl.lastScreenshot == null) {
            return;
        }

        File outputFile = new File(instance.directory, "screenshot.png");
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(outputFile);
            LogTapeImpl.lastScreenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
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

    void clearLogFiles() {
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

    static LogTapePropertySupplier getPropertySupplier() {
        if (instance != null) {
            return instance.propertySupplier;
        } else {
            return null;
        }
    }

    void showReportActivity() {
        if (showing || !LogTapeImpl.enabled) {
            return;
        }

        final Activity activity = currentActivity.get();

        if (activity == null || activity instanceof ReportIssueActivity) {
            return;
        }

        showing = true;

        this.progress = ProgressDialog.show(activity, "Saving screenshot..",
                "", true);

        final Bitmap lastScreenshot = getScreenShot(activity.getWindow().getDecorView().getRootView());
        LogTapeImpl.lastScreenshot = lastScreenshot;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                saveScreenshotToDisk();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (progress != null && progress.isShowing() && !activity.isDestroyed()) {
                    progress.dismiss();

                    // If it's not showing, we've probably gone to background
                    Intent intent = new Intent(activity.getApplicationContext(), ReportIssueActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                }

                progress = null;
                showing = false;
            }
        }.execute();
    }


    void logObject(String message, JSONObject object, Map<String, String> tags) {
        saveLogToDisk(new ObjectLogEvent(message, object, tags));
    }

    void log(String message, Map<String, String> tags) {
        saveLogToDisk(new MessageLogEvent(message, tags));
    }

    void logRequest(LogTapeDate startDate,
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
                    boolean logStartEvent) {
        RequestStartedLogEvent req = new RequestStartedLogEvent(startDate, url, method, requestHeaders, body, tags);

        if (logStartEvent) {
            saveLogToDisk(req);
        }

        LogEvent res = new RequestLogEvent(new LogTapeDate(), req, httpStatusCode, httpStatusText, responseHeaders, responseBody, errorText, tags);
        saveLogToDisk(res);
    }

    Object logRequestStart(String url,
                           String method,
                           Map<String, String> requestHeaders,
                           byte[] body,
                           Map<String, String> tags) {
        RequestStartedLogEvent ret = new RequestStartedLogEvent(new LogTapeDate(), url, method, requestHeaders, body, tags);
        saveLogToDisk(ret);
        return ret;
    }

    void logRequestFinished(Object startEvent,
                            int httpStatusCode,
                            String httpStatusText,
                            Map<String, String> responseHeaders,
                            byte[] responseBody,
                            String errorText,
                            Map<String, String> tags) {
        RequestStartedLogEvent reqStartedEvent = (RequestStartedLogEvent) startEvent;
        LogEvent ev = new RequestLogEvent(new LogTapeDate(), reqStartedEvent, httpStatusCode, httpStatusText, responseHeaders, responseBody, errorText, tags);
        saveLogToDisk(ev);
    }

    void setPropertySupplier(LogTapePropertySupplier supplier) {
        this.propertySupplier = supplier;
    }

    private Bitmap getScreenShot(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }
}