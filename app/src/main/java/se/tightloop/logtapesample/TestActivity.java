package se.tightloop.logtapesample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.rest.spring.annotations.RestService;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import se.tightloop.logtape.okhttp.LogTapeLoggingInterceptor;
import se.tightloop.logtape.volley.LogTapeVolleyStack;
import se.tightloop.logtapeandroid.LogTape;
import se.tightloop.logtapeandroid.R;
import se.tightloop.logtapesample.model.GetData;
//import se.tightloop.logtape.okhttp;

/**
 * Created by dnils on 09/10/16.
 */

@EActivity(R.layout.test_activity)
public class TestActivity extends Activity {
    @ViewById(R.id.textView)
    TextView textView;

    @RestService
    RestClient restClient;

    RequestQueue volleyQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test_activity);

        LogTape.ClearLog();

        LogTape.Log("TestActivity", "Log from TestActivity");

        JSONObject testObject = new JSONObject();

        try {
            testObject.put("key", "value");
            testObject.put("otherKey", 3232);
         } catch(JSONException exc) {

        }

        LogTape.LogObject("TestActivity", "A test object", testObject);


        fetchWithSpring();
        fetchWithOkHTTP();
        fetchWithVolley();
    }


    @Background
    void fetchWithVolley() {
        volleyQueue = Volley.newRequestQueue(this, new LogTapeVolleyStack());
        String url = "http://www.httpbin.org/get";
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (com.android.volley.Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println("Got JSON response");
                    }
                }, new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("Got volley error");
                    }
                });
        volleyQueue.add(jsObjRequest);
    }

    @Background
    void fetchWithOkHTTP() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new LogTapeLoggingInterceptor())
                .build();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://www.httpbin.org/get")
                .build();

        try {
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            if (body != null) {
                System.out.println(body.string());

            }
        } catch (java.io.IOException exception) {

        }
    }

    @Background
    void fetchWithSpring() {
        GetData data = restClient.get();
        System.out.println("Result: " + data.url);
    }

    public void launchReportActivity(View button) {
        LogTape.ShowReportActivity();
    }
}
