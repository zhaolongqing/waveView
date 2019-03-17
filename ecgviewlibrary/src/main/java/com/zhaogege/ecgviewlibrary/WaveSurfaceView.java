package com.zhaogege.ecgviewlibrary;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class WaveSurfaceView extends SurfaceView implements SurfaceHolder.Callback {


    /**
     * 设置波形平滑
     */
    protected CornerPathEffect cornerPathEffect = new CornerPathEffect(20);
    //横线数量
    private int xLineCount;
    //竖线数量
    private int yLineCount;
    //x轴一个单元格的小格子个数
    private int xUnitCount;
    //y轴一个单元格的小格子个数
    private int yUnitCount;
    /**
     * 网格画笔
     */
    private Paint mGridPaint;
    /**
     * 波形画笔
     */
    private Paint wavePaint;
    /**
     * surface宽度
     */
    private int mWidth;
    /**
     * surface高度
     */
    private int mHeight;
    /**
     * 波形缓冲区
     */
    private Bitmap mBufferBitmap;
    /**
     * 背景图
     */
    private Bitmap mBackgroundBitmap;
    /**
     * 缓冲画板
     */
    private Canvas mBufferCanvas;

    private SurfaceHolder mHolder;

    public WaveSurfaceView(Context context) {
        super(context);
    }

    public WaveSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public WaveSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WaveSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WaveSurfaceView);
        ta.recycle();
        init(context);
    }


    private void init(Context context) {
        mHolder = getHolder();
        mHolder.addCallback(this);

        wavePaint = new Paint();
        wavePaint.setAntiAlias(true);
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setColor(Color.RED);
        wavePaint.setStrokeWidth(5);
        wavePaint.setPathEffect(cornerPathEffect);

        mGridPaint = new Paint();
        mGridPaint.setColor(Color.GRAY);
        mGridPaint.setStrokeWidth(5);
        mGridPaint.setPathEffect(cornerPathEffect);

        Paint mGainPaint = new Paint();
        mGridPaint.setColor(Color.BLUE);
        mGridPaint.setStrokeWidth(5);
        mGridPaint.setPathEffect(cornerPathEffect);

    }


    private void drawBackground(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        float perLineWidth = mWidth / (float) (yLineCount - 1);
        float perLineHeight = mHeight / (float) (xLineCount - 1);
        float perUnitWidth = perLineWidth / xUnitCount;
        float perUnitHeight = perLineHeight / yUnitCount;
        for (int i = 0; i <= yLineCount - 1; i++) {
//          画竖线
            canvas.drawLine(i * perLineWidth, 0, i * perLineWidth, mHeight, mGridPaint);
            for (int j = 0; j <= xLineCount - 1; j++) {
                for (int xk = 1; xk < xUnitCount; xk++) {
                    for (int yk = 1; yk < yUnitCount; yk++) {
                        canvas.drawPoint(i * perLineWidth + xk * perUnitWidth, j * perLineHeight + yk * perUnitHeight, mGridPaint);
                    }
                }
                if (i != 0) {
                    continue;
                }
//              画横线
                canvas.drawLine(0, j * perLineHeight, mWidth, j * perLineHeight, mGridPaint);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mWidth = this.getWidth();
        mHeight = this.getHeight();

        mBufferBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBackgroundBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(mBackgroundBitmap);
        drawBackground(mBufferCanvas);

        mBufferCanvas.setBitmap(mBufferBitmap);
        Canvas c = holder.lockCanvas();
        c.drawBitmap(mBackgroundBitmap, 0, 0, mGridPaint);
        holder.unlockCanvasAndPost(c);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
