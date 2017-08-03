package se.tightloop.logtapeandroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import se.tightloop.logtape.R;

/**
 * Created by dnils on 2017-07-06.
 */

public class EditImageActivity extends AppCompatActivity {

    private PaintView mPaintView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_image_activity);

    }


    @Override
    protected void onStart() {
        super.onStart();
        mPaintView = (PaintView)findViewById(R.id.paintView);
        mPaintView.setBitmap(LogTape.lastScreenshot);

        Toast.makeText(this, getString(R.string.draw_image_instructions), Toast.LENGTH_LONG).show();
    }
}
