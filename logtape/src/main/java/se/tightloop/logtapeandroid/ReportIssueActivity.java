package se.tightloop.logtapeandroid;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import se.tightloop.logtape.R;

import static se.tightloop.logtapeandroid.LogTape.lastScreenshot;

public class ReportIssueActivity extends AppCompatActivity {
    private AsyncTask<Void, Void, Void> saveTask = null;
    private AsyncTask<Void, Void, Bitmap> loadTask = null;

    static class UploadResult {
        UploadResult(int issueNum, Integer deletedIssueNumber) {
            this.issueNum = issueNum;
            this.deletedIssueNumber = deletedIssueNumber;
        }

        int issueNum;
        Integer deletedIssueNumber;
    }

    static UploadResult uploadIssue(JSONObject body)
    {
        URL url;
        HttpURLConnection connection = null;
        try {
            url = new URL("https://www.logtape.io:443/api/issues");

            connection = (HttpURLConnection)url.openConnection();
            connection.setDoOutput(true);

            String authString = "issues:" + LogTape.ApiKey();
            connection.setRequestMethod("POST"); // hear you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
            connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(authString.getBytes("UTF-8"), Base64.DEFAULT));
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.connect();

            //Send request
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            System.out.println(body.toString());
            wr.write(body.toString());
            wr.flush();
            wr.close ();

            InputStream is;
            int response = connection.getResponseCode();
            if (response >= 200 && response <=399){
                is = connection.getInputStream();
                System.out.println("Got OK response");
                String responseStr = LogTapeUtil.readStreamToString(is);
                JSONObject jsonObject = new JSONObject(responseStr);
                int issueNumber = jsonObject.getInt("issueNumber");

                Integer deletedIssueNumber = null;

                if(jsonObject.has("deletedIssueNumber")) {
                    deletedIssueNumber = jsonObject.getInt("deletedIssueNumber");
                }

                System.out.println("Response: " + responseStr);

                return new UploadResult(issueNumber, deletedIssueNumber);
            } else {
                System.out.println("Got fail response");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
    }

    JSONObject labelValueObject(String label, String value) throws JSONException {
        JSONObject ret = new JSONObject();

        ret.put("label", label);
        ret.put("value", value);

        return ret;
    }

    public void editImage(View view) {
        Intent intent = new Intent(getApplicationContext(), EditImageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void submit(View view) {
        final JSONObject body = new JSONObject();
        try {
            EditText e = (EditText)findViewById(R.id.textField);
            String description = "";

            if (e != null) {
                description = e.getText().toString();
            }

            JSONArray images = new JSONArray();

            if (lastScreenshot != null) {
                images.put(LogTapeUtil.encodeImageToBase64(lastScreenshot));
            }

            JSONArray properties = new JSONArray();
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            properties.put(labelValueObject("Android OS version", Build.VERSION.RELEASE));
            properties.put(labelValueObject("Application version", packageInfo.versionCode + "." + packageInfo.versionName));
            properties.put(labelValueObject("Device model", Build.MODEL));
            properties.put(labelValueObject("Device brand", Build.BRAND));

            body.put("images", images);
            body.put("properties", properties);
            body.put("timestamp", LogTapeUtil.getUTCDateString(new Date()));
            body.put("title", description);


            final ProgressDialog progress = ProgressDialog.show(this, "Uploading issue..",
                    "", true);

            LogTape.GetJSONItems(new LogTape.IssueListResultListener() {
                @Override
                public void onResultReceived(JSONArray result) {
                    try {
                        body.put("events", result);
                    } catch (JSONException exception) {

                    }

                    uploadBody(body, progress);
                }
            });
        } catch (Exception e) {

        }
    }


    void uploadBody(final JSONObject body, final ProgressDialog progress) {
        final ReportIssueActivity activity = this;

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final UploadResult result = uploadIssue(body);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        if (result != null) {
                            String format = getResources().getString(R.string.issue_uploaded_with_id);
                            String text = String.format(format, result.issueNum);

                            if (result.deletedIssueNumber != null) {
                                String deletedFormat = getResources().getString(R.string.issue_deleted_with_id);
                                text += String.format(deletedFormat, result.deletedIssueNumber.intValue());
                            }

                            Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
                            activity.finish();
                        } else {
                            Toast.makeText(activity, R.string.issue_upload_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);
        System.out.println("**LOGTAPE: Report issue activity onCreate");

        if (LogTape.lastScreenshot != null) {
            final ImageView header = (ImageView)findViewById(R.id.imageView);
            header.setImageBitmap(LogTape.lastScreenshot);
            saveScreenshot();
        } else {
            final WeakReference<ReportIssueActivity> reportActivity = new WeakReference<ReportIssueActivity>(this);

            this.loadTask = new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... voids) {
                    return LogTape.LoadScreenshotFromDisk();
                }

                @Override
                protected void onPostExecute(Bitmap file) {
                    if (!isCancelled()) {
                        ReportIssueActivity activity = reportActivity.get();
                        activity.loadTask = null;

                        if (activity != null) {
                            final ImageView header = (ImageView)activity.findViewById(R.id.imageView);
                            if (header != null) {
                                header.setImageBitmap(file);
                            }
                        }

                        LogTape.lastScreenshot = file;
                    }
                }
            }.execute();
        }
    }
    private void saveScreenshot() {

        final WeakReference<ReportIssueActivity> issueActivity = new WeakReference<ReportIssueActivity>(this);

        saveTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                LogTape.SaveScreenshotToDisk();
                return null;
            }

            @Override
            protected void onPostExecute(Void res) {
                if (!isCancelled()) {
                    ReportIssueActivity activity = issueActivity.get();
                    if (activity != null) {
                        activity.saveTask = null;
                    }
                }
            }
        }.execute();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        System.out.println("**LOGTAPE: Report issue activity onRestart");
        saveScreenshot();
    }
}
