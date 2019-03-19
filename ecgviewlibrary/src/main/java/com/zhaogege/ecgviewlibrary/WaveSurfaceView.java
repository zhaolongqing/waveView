package com.zhaogege.ecgviewlibrary;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class WaveSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "WaveSurfaceView";
    private int mWidth;
    private int mHeight;
    //横线数量
    private int xLineCount;
    //竖线数量
    private int yLineCount;
    //x轴一个单元格的小格子个数
    private int xUnitCount;
    //y轴一个单元格的小格子个数
    private int yUnitCount;
    //主线颜色
    private int mainColor;
    //副颜色
    private int subColor;
    //折线颜色
    private int waveColor;
    //x轴最大值
    private float xMax;
    //y轴最大值
    private float yMax;
    //x轴最小值
    private float xMin;
    //y轴最小值
    private float yMin;
    //x轴行进的速度xMax-xMin为总值
    private float xSpeed;
    //y轴行进的速度yMax-yMin为总值
    private float ySpeed;
    //每次行进图像长度
    private float perWidth;
    private float perHeight;
    //绘制行进次数
    private int xDrawCount;
    /**
     * 上一次的缓冲值
     */
    private float yTemp;
    private float xTemp;
    //线条
    private Paint mainLinePaint = new Paint();
    //副线条（点）
    private Paint secondLinePaint = new Paint();
    //折线
    private Paint waveLinePaint = new Paint();
    /**
     * 背景主线宽度
     */
    private float mainStrokeWidth;
    /**
     * 背景副线宽度
     */
    private float subStrokeWidth;
    /**
     * 折线宽度
     */
    private float waveStrokeWidth;
    /**
     * 背景色
     */
    private int backgroundColor;
    /**
     * 缓冲画板
     */
    private Canvas mBufferCanvas;
    /**
     * 缓冲线
     */
    private Bitmap mBufferBitmap;
    /**
     * 背景图
     */
    private Bitmap backgroundBitmap;
    private Path wavePath = new Path();
    private SurfaceHolder holder;
    private int lx;

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

    /**
     * 初始化属性
     */
    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WaveSurfaceView);
        xLineCount = ta.getInt(R.styleable.WaveSurfaceView_x_line_count, 10);
        yLineCount = ta.getInt(R.styleable.WaveSurfaceView_y_line_count, 5);
        xUnitCount = ta.getInt(R.styleable.WaveSurfaceView_x_unit_count, 5);
        yUnitCount = ta.getInt(R.styleable.WaveSurfaceView_y_unit_count, 5);
        xMax = ta.getFloat(R.styleable.WaveSurfaceView_x_max, 200);
        yMax = ta.getFloat(R.styleable.WaveSurfaceView_y_max, 100);
        xMin = ta.getFloat(R.styleable.WaveSurfaceView_x_min, 0);
        yMin = ta.getFloat(R.styleable.WaveSurfaceView_y_min, 0);
        xSpeed = ta.getFloat(R.styleable.WaveSurfaceView_x_speed, 3);
        ySpeed = ta.getFloat(R.styleable.WaveSurfaceView_y_speed, 3);
        mainColor = ta.getColor(R.styleable.WaveSurfaceView_main_line_color, getResources().getColor(R.color.main_color));
        subColor = ta.getColor(R.styleable.WaveSurfaceView_sub_line_color, getResources().getColor(R.color.sub_color));
        waveColor = ta.getColor(R.styleable.WaveSurfaceView_wave_line_color, getResources().getColor(R.color.wave_color));
        backgroundColor = ta.getColor(R.styleable.WaveSurfaceView_background_color, getResources().getColor(android.R.color.white));
        mainStrokeWidth = ta.getDimension(R.styleable.WaveSurfaceView_main_stroke_width, 3);
        subStrokeWidth = ta.getDimension(R.styleable.WaveSurfaceView_sub_stroke_width, 1);
        waveStrokeWidth = ta.getDimension(R.styleable.WaveSurfaceView_wave_stroke_width, 1);
        initDraw();
        init();
        ta.recycle();
    }

    /**
     * 初始化画笔
     */
    private void initDraw() {
        mainLinePaint.setColor(mainColor);
        mainLinePaint.setStyle(Paint.Style.STROKE);
        mainLinePaint.setStrokeWidth(mainStrokeWidth);
        mainLinePaint.setAntiAlias(true);

        secondLinePaint.setColor(subColor);
        secondLinePaint.setStrokeWidth(subStrokeWidth);
        secondLinePaint.setAntiAlias(true);

        waveLinePaint.setColor(waveColor);
        waveLinePaint.setStyle(Paint.Style.STROKE);
        waveLinePaint.setStrokeWidth(waveStrokeWidth);
        waveLinePaint.setAntiAlias(true);
    }

    /**
     * 初始化
     */
    private void init() {
        holder = getHolder();
        holder.addCallback(this);
    }

    /**
     * 背景绘制
     */
    private void drawBackground(Canvas canvas) {
        canvas.drawColor(backgroundColor);
        // 每个大格子x轴的长度
        float perLineWidth = mWidth / (float) (yLineCount - 1);
        //每个大格子y轴的长度
        float perLineHeight = mHeight / (float) (xLineCount - 1);
        //每个小格子的x轴长度
        float perUnitWidth = perLineWidth / xUnitCount;
        //每个小格子的y轴长度
        float perUnitHeight = perLineHeight / yUnitCount;
        for (int i = 0; i <= yLineCount - 1; i++) {
//          画竖线
            canvas.drawLine(i * perLineWidth, 0, i * perLineWidth, mHeight, mainLinePaint);
            for (int j = 0; j <= xLineCount - 1; j++) {
                for (int xk = 1; xk < xUnitCount; xk++) {
                    for (int yk = 1; yk < yUnitCount; yk++) {
                        canvas.drawPoint(i * perLineWidth + xk * perUnitWidth, j * perLineHeight + yk * perUnitHeight, secondLinePaint);
                    }
                }
                if (i != 0) {
                    continue;
                }
//              画横线
                canvas.drawLine(0, j * perLineHeight, mWidth, j * perLineHeight, mainLinePaint);
            }
        }
    }

    /**
     * 测量
     */
    private void measure() {
        //x轴范围每个值所代表的像素值
        perWidth = mWidth / (xMax - xMin) * xSpeed;
        //每个y轴范围值所代表的像素值
        perHeight = mHeight / (yMax - yMin) * ySpeed;
        //        模
        lx = (int) ((xMax - xMin) / xSpeed);
    }


    /**
     * 画线
     */
    public void drawLine(int y) {
        if (xTemp > mWidth) {
            xDrawCount = 0;
        }

        if ((int) xTemp == 0) {
            xTemp = 0;
        }
        Rect rect = new Rect((int) xTemp, 0, (int) (xTemp + perWidth * 3), mHeight);

        mBufferCanvas.save();
        mBufferCanvas.clipRect(rect);
        mBufferCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mBufferCanvas.restore();
        if (xTemp == 0) {
            wavePath.reset();
            wavePath.moveTo(xTemp, (mHeight >> 1) + yTemp * perHeight);
        }
        wavePath.lineTo(xTemp + perWidth, (mHeight >> 1) + y * perHeight);
        mBufferCanvas.drawPath(wavePath, waveLinePaint);

        Canvas c = holder.lockCanvas();
        if (c == null) {
            return;
        }
        c.drawBitmap(backgroundBitmap, 0, 0, waveLinePaint);
        c.drawBitmap(mBufferBitmap, 0, 0, waveLinePaint);
        holder.unlockCanvasAndPost(c);

        yTemp = y;
        xTemp = (xDrawCount % lx) * perWidth;
        xDrawCount++;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        mWidth = getWidth();
        mHeight = getHeight();
        Log.i(TAG, "surfaceChanged-->width:" + mWidth + "--mHeight:" + mHeight);

        measure();

        backgroundBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(backgroundBitmap);
        drawBackground(mBufferCanvas);

        Canvas c = holder.lockCanvas();
        c.drawBitmap(backgroundBitmap, 0, 0, mainLinePaint);
        holder.unlockCanvasAndPost(c);

        mBufferBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBufferCanvas.setBitmap(mBufferBitmap);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
    }
}
