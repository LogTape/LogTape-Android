package se.tightloop.logtapeandroid;

import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class ReportIssueActivity extends AppCompatActivity {

    public static Boolean uploadIssue(JSONObject body)
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
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream ());
            wr.writeBytes(body.toString());
            wr.flush();
            wr.close ();

            InputStream is;
            int response = connection.getResponseCode();
            if (response >= 200 && response <=399){
                is = connection.getInputStream();
                System.out.println("Got OK response");
                String responseStr = LogTapeUtil.readStreamToString(is);
                System.out.println("Response: " + responseStr);
                return true;
            } else {
                System.out.println("Got fail response");
                //return is = connection.getErrorStream();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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


    public void submit(View view) {
        final JSONObject body = new JSONObject();
        try {
            EditText e = (EditText)findViewById(se.tightloop.logtapeandroid.R.id.textField);
            String description = "";

            if (e != null) {
                description = e.getText().toString();
            }

            JSONArray images = new JSONArray();

            if (LogTape.lastScreenshot != null) {
                images.put(LogTapeUtil.encodeImageToBase64(LogTape.lastScreenshot));
            }

            JSONArray properties = new JSONArray();
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            properties.put(labelValueObject("Android OS version", Build.VERSION.RELEASE));
            properties.put(labelValueObject("Application version", packageInfo.versionCode + "." + packageInfo.versionName));
            properties.put(labelValueObject("Device model", Build.MODEL));
            properties.put(labelValueObject("Device brand", Build.BRAND));

            body.put("events", LogTape.GetJSONItems());
            body.put("images", images);
            body.put("properties", properties);
            body.put("timestamp", LogTapeUtil.getUTCDateString(new Date()));
            body.put("title", description);


            final ProgressDialog progress = ProgressDialog.show(this, "Uploading issue..",
                    "", true);

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    uploadIssue(body);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                        }
                    });
                }
            });
        } catch (Exception e) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(se.tightloop.logtapeandroid.R.layout.activity_report_issue);
        ImageView header = (ImageView)findViewById(se.tightloop.logtapeandroid.R.id.imageView);
        header.setImageBitmap(LogTape.lastScreenshot);
    }
}
