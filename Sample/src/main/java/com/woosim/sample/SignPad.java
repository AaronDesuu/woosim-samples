package com.woosim.sample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.woosim.printer.WoosimCmd;
import com.woosim.printer.WoosimImage;


public class SignPad extends AppCompatActivity {
    private SignView mSign;
    private Bitmap mBitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_pad);

        // append a custom view to the base layout
        LinearLayout layout = findViewById(R.id.layout1);
        mSign = new SignView(this);
        if (layout != null) {
            layout.addView(mSign);
        }
    }

    @Override
    protected void onDestroy() {
        if (mBitmap != null)
            mBitmap.recycle();
        super.onDestroy();
    }


    public void printSignature(View v) {
        Bitmap bmp = Bitmap.createScaledBitmap(mBitmap, 384, 384*mBitmap.getHeight()/mBitmap.getWidth(), false);
        byte[] data = WoosimImage.printCompressedBitmap(0, 0, 0, 0, bmp);
        bmp.recycle();

        MainActivity.mPrintService.write(WoosimCmd.initPrinter());
        MainActivity.mPrintService.write(WoosimCmd.setPageMode());
        MainActivity.mPrintService.write(data);
        MainActivity.mPrintService.write(WoosimCmd.PM_setStdMode());
        MainActivity.mPrintService.write(WoosimCmd.printLineFeed(2));
    }

    public void clearSignature(View v) {
        mBitmap.eraseColor(Color.TRANSPARENT);
        mSign.invalidate();
    }

    /**
     * Custom view to draw a signature.
     */
    private class SignView extends View {
        private final Paint mPaint;
        private Canvas mCanvas;
        private final Path mPath;
        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        public SignView(Context c) {
            super(c);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            mPaint.setColor(Color.BLUE);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(5);
            mPath = new Path();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.LTGRAY);
            canvas.drawBitmap(mBitmap, 0, 0, null);
            canvas.drawPath(mPath, mPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                break;
            }
            invalidate();
            return true;
        }

        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }

        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);

            // if movement is too small, ignore it
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                mX = x;
                mY = y;
            }
        }

        private void touch_up() {
            mPath.lineTo(mX, mY);
            // commit the path to our offscreen
            mCanvas.drawPath(mPath, mPaint);
            // kill this so we don't double draw
            mPath.reset();
        }
    }
}
