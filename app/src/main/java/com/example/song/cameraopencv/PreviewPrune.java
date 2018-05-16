package com.example.song.cameraopencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by song on 2018/3/20.
 */

public class PreviewPrune extends View {

    private Dot startDot;
    private Dot endDot;
    private Bitmap mBitmap;
    private Bitmap ocrBitmap;
    private int screenHeight;
    private int screenWidth;
    private Dot leftTopDot;
    private Dot rightBottomDot;
    private Paint paintShadow;
    int shadow = 0xaa000000;
    int clear = 0x0000000;


    public PreviewPrune(Context context) {
        super(context);
        startDot = new Dot();
        endDot = new Dot();
        leftTopDot = new Dot();
        rightBottomDot = new Dot();
    }

    public PreviewPrune(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        startDot = new Dot();
        endDot = new Dot();
        leftTopDot = new Dot();
        rightBottomDot = new Dot();
    }

    public PreviewPrune(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        startDot = new Dot();
        endDot = new Dot();
        leftTopDot = new Dot();
        rightBottomDot = new Dot();
    }

    public void setBitmap(Bitmap bitmap, int screenHeight, int screenWidth) {
        mBitmap = bitmap;
        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;
        changeBitmapSize();
        /*invalidate()函数的作用是使整个窗口客户区无效，此时就需要重汇，这个就会自动调用
        * 窗口类的OnPaint函数，OnPaint负责重绘窗口。视图类中就调用OnDraw函数，实际的重绘工作由
        * OnPaint或者OnDraw来完成。*/
        invalidate();
    }

    public void restart(){
        startDot = new Dot();
        endDot = new Dot();
        leftTopDot = new Dot();
        rightBottomDot = new Dot();
        invalidate();
    }
    /**
     *将 将要显示的bitmap进行变形，使其铺满屏幕
     *
     */
    private void changeBitmapSize() {
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        float scaleWidth = ((float) screenWidth) / width;
        float scaleHeight = ((float) screenHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        getLeftTopDot();
        getRightBottomDot();
        drawBitmap(canvas);
        drawArea(canvas);
        drawShadowTop(canvas);
        drawShadowLeft(canvas);
        drawShadowRight(canvas);
        drawShadowBottom(canvas);
    }

    /**
     *绘制阴影
     *
     */
    private void drawShadowBottom(Canvas canvas) {
        paintShadow = new Paint();
        paintShadow.setColor(shadow);
        canvas.drawRect(0, rightBottomDot.getY(), screenWidth, screenHeight, paintShadow);
    }

    private void drawShadowRight(Canvas canvas) {
        paintShadow = new Paint();
        paintShadow.setColor(shadow);
        canvas.drawRect(rightBottomDot.getX(), leftTopDot.getY(), screenWidth, rightBottomDot.getY(), paintShadow);
    }

    private void drawShadowLeft(Canvas canvas) {
        paintShadow = new Paint();
        paintShadow.setColor(shadow);
        canvas.drawRect(0, leftTopDot.getY(), leftTopDot.getX(), rightBottomDot.getY(), paintShadow);
    }

    private void drawShadowTop(Canvas canvas) {
        paintShadow = new Paint();
        paintShadow.setColor(shadow);
        canvas.drawRect(0, 0, screenWidth, leftTopDot.getY(), paintShadow);
    }

    private void drawBitmap(Canvas canvas) {
        Paint paint = new Paint();
        canvas.drawBitmap(mBitmap, 0, 0, paint);
    }

    /**
     * 画出截图区域
     *
     * @param canvas
     */
    private void drawArea(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(clear);
        canvas.drawRect(leftTopDot.getX(), leftTopDot.getY(), rightBottomDot.getX(), rightBottomDot.getY(), paint);
    }


    /**
     * 获取截图区域bitmap
     *
     * @return 截图
     */
    public Bitmap getBitmap() {
        if (mBitmap != null) {
            getLeftTopDot();
            getRightBottomDot();
            if (getBitmapOutWidth() > 0 && getBitmapOutHeight() > 0) {
                if(leftTopDot.getY()<0){
                    leftTopDot.setY(0);
                }
                ocrBitmap = Bitmap.createBitmap(mBitmap, (int) leftTopDot.getX(), (int) leftTopDot.getY(), getBitmapOutWidth(), getBitmapOutHeight());
            }
        }
        return ocrBitmap;
    }

    /**
     * 获取截图区域宽度
     *
     * @return
     */
    private int getOutWidth() {
        return (int) (rightBottomDot.getX() - leftTopDot.getX());
    }

    /**
     * 获取截图区域高度
     *
     * @return
     */
    private int getOutHeight() {
        return (int) (rightBottomDot.getY() - leftTopDot.getY());
    }

    private int getBitmapOutWidth() {
        int bitmapOutWidth;
        int scale = getOutWidth() * mBitmap.getWidth();
        bitmapOutWidth = scale / screenWidth;
        return bitmapOutWidth;
    }

    private int getBitmapOutHeight() {
        int bitmapOutHeight;
        int scale = getOutHeight() * mBitmap.getHeight();
        bitmapOutHeight = scale / screenHeight;
        return bitmapOutHeight;
    }

    private void getLeftTopDot() {
        if (endDot.getX() > startDot.getX()) {
            leftTopDot.setX(startDot.getX());
        } else {
            leftTopDot.setX(endDot.getX());
        }
        if (endDot.getY() > startDot.getY()) {
            leftTopDot.setY(startDot.getY());
        } else {
            leftTopDot.setY(endDot.getY());
        }
    }

    private void getRightBottomDot() {
        if (startDot.getX() > endDot.getX()) {
            rightBottomDot.setX(startDot.getX());
        } else {
            rightBottomDot.setX(endDot.getX());
        }
        if (startDot.getY() > endDot.getY()) {
            rightBottomDot.setY(startDot.getY());
        } else {
            rightBottomDot.setY(endDot.getY());
        }

    }

    public Dot getStartDot() {
        return startDot;
    }

    public void setStartDot(Dot startDot) {
        this.startDot = startDot;
    }

    public Dot getEndDot() {
        return endDot;
    }

    public void setEndDot(Dot endDot) {
        this.endDot = endDot;
    }

}
