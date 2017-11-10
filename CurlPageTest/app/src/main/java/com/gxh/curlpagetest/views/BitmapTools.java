package com.gxh.curlpagetest.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Created by fft123 on 2016/12/3.
 */
public class BitmapTools {

    public static Bitmap drawTextAtBitmap(Bitmap bitmap, String text, String color) {
        Bitmap aBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        Canvas canvas = new Canvas(aBitmap);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        paint.setTextSize(100);
        paint.setColor(Color.parseColor(color));
        int iWordWidth = (int) paint.measureText(text);
        canvas.drawText(text, bitmap.getWidth() / 2 - iWordWidth / 2, bitmap.getWidth() / 2, paint);

        return aBitmap;
    }

    public static Bitmap drawImageAtBitmap(Bitmap src, Bitmap add, boolean isNeedMatchParent) {
        Bitmap aBitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.RGB_565);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(aBitmap);
        canvas.drawBitmap(src, 0, 0, paint);
        if (!isNeedMatchParent) {
            canvas.drawBitmap(add, src.getWidth() / 2 - add.getWidth() / 2, src.getHeight() / 2 - add.getHeight() / 2, paint);
        } else {
            Rect srcRect = new Rect();
            srcRect.left = 0;
            srcRect.top = 0;
            srcRect.right = add.getWidth();
            srcRect.bottom = add.getHeight();

            RectF disRect = new RectF();
            disRect.left = 0;
            disRect.top = 0;
            disRect.bottom = src.getHeight();
            disRect.right = src.getWidth();

            canvas.drawBitmap(add, srcRect, disRect, paint);
        }
        return aBitmap;
    }

    public static Bitmap drawShadowLine(Bitmap src, Bitmap line) {
        Bitmap aBitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.RGB_565);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(aBitmap);
        canvas.drawBitmap(src, 0, 0, paint);
        Rect srcRect = new Rect();
        srcRect.left = 0;
        srcRect.top = 0;
        srcRect.right = line.getWidth();
        srcRect.bottom = line.getHeight();

        RectF disRect = new RectF();
        disRect.left = 0;
        disRect.top = 0;
        disRect.bottom = src.getHeight();
        disRect.right = 30;

        canvas.drawBitmap(line, srcRect, disRect, paint);

        return aBitmap;
    }


}
