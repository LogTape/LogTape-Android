package se.tightloop.logtapeandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import se.tightloop.logtape.R;

/**
 * Created by dnils on 2017-08-02.
 */

public class PaintView extends View {
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private final Path mPath = new Path();
    private final Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    private Paint mPaint = new Paint();

    public PaintView(Context c, AttributeSet attributeSet) {
        super(c, attributeSet);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(10);
        mPaint.setDither(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(R.color.paintBackground);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

    public void setBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
        this.mCanvas = new Canvas(mBitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPath.reset();
                mPath.moveTo(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                mPath.lineTo(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                mPath.lineTo(x, y);
                mCanvas.drawPath(mPath, mPaint);
                mPath.reset();
                invalidate();
                break;
        }


        return true;
    }
}
