package se.tightloop.logtapeandroid;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
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
        mPaintView = (PaintView)findViewById(R.id.paintView);

        if (LogTapeImpl.lastScreenshot != null) {
            mPaintView.setBitmap(LogTapeImpl.lastScreenshot);
        } else {
            System.out.println("**LOGTAPE: Report issue activity onCreate");

            final ProgressDialog progress = ProgressDialog.show(this, "Saving bitmap..",
                    "", true);
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... voids) {
                    return LogTapeImpl.loadScreenshotFromDisk();
                }

                @Override
                protected void onPostExecute(Bitmap file) {
                    System.out.println("**LOGTAPE: On post execute, EditImageAct");
                    LogTapeImpl.lastScreenshot = file;
                    progress.dismiss();
                    if (mPaintView != null) {
                        mPaintView.setBitmap(file);
                        System.out.println("**LOGTAPE: Setting bitmap");
                    }
                }
            }.execute();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        Toast.makeText(this, getString(R.string.draw_image_instructions), Toast.LENGTH_LONG).show();
    }
}
