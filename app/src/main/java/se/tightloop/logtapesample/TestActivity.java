package se.tightloop.logtapesample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.rest.spring.annotations.RestService;

import se.tightloop.logtapeandroid.LogTape;
import se.tightloop.logtapeandroid.R;
import se.tightloop.logtapesample.model.GetData;

/**
 * Created by dnils on 09/10/16.
 */

@EActivity(R.layout.test_activity)
public class TestActivity extends Activity {
    @ViewById(R.id.textView)
    TextView textView;

    @RestService
    RestClient restClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test_activity);
    }

    @Background
    void fetchWithSpring() {
        GetData data = restClient.get();
        System.out.println("Result: " + data.url);
    }

    @AfterInject
    void afterInject() {
        LogTape.Log("Log from TestActivity");
        makeNetworkCalls();
    }

    public void makeNetworkCalls() {
        fetchWithSpring();
    }

    public void launchReportActivity(View button) {
        LogTape.ShowReportActivity();
    }
}
