package com.zhaogege.ecgviewlibrary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

/**
 * author: ZlqPC
 * created on: 2019/3/14 20:40
 * description:
 */
public class ECGPathWaveView extends View {

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
    //线条
    private Paint mainLinePaint = new Paint();
    //副线条（点）
    private Paint secondLinePaint = new Paint();
    //折线
    private Paint waveLinePaint = new Paint();
    //折线绘制路径
    private Path wavePath = new Path();
    //折线测量工具
    private PathMeasure pathMeasure = new PathMeasure();
    //背景图
    private Bitmap backgroundBitmap;
    //复制背景图
    private Bitmap backCacheBitmap;
    //折线画板
    private Canvas waveCanvas = new Canvas();
    //上一个x值
    private float xTemp;
    //绘制行进次数
    private int xDrawCount;
    //上一个y值
    private float yTemp;
    //上一个点
    private float[] xyTemp = new float[2];

    private float mainStrokeWidth;
    private float subStrokeWidth;
    private float waveStrokeWidth;
    // 每个大格子x轴的长度
    private float perLineWidth;
    //每个大格子y轴的长度
    private float perLineHeight;
    //每个小格子的x轴长度
    private float perUnitWidth;
    //每个小格子的y轴长度
    private float perUnitHeight;
    //每次行进图像长度
    private float travelWidth;
    private float travelHeight;
    //x轴画线的队列值
    private ArrayQueue xQueue = new ArrayQueue(200);
    //判断x轴是否画满
    private boolean isXFull;

    private LifecycleStatue LIFECYCLE_STATUE = LifecycleStatue.STOP;
    ;

    private enum LifecycleStatue {
        START, STOP;
    }

    private Application.ActivityLifecycleCallbacks lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (xDrawCount > 0) {
                isXFull = false;
                xDrawCount = 0;
                xQueue.clear();
                backgroundBitmap = backCacheBitmap.copy(Bitmap.Config.ARGB_8888, true);
                wavePath = new Path();
            }
            LIFECYCLE_STATUE = LifecycleStatue.START;
        }

        @Override
        public void onActivityPaused(Activity activity) {
            LIFECYCLE_STATUE = LifecycleStatue.STOP;
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            activity.getApplication().unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
        }
    };

    public ECGPathWaveView(Context context) {
        super(context);
    }

    public ECGPathWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public ECGPathWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ECGPathWaveView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ECGPathWaveView);
        xLineCount = ta.getInt(R.styleable.ECGPathWaveView_x_line_count, 10);
        yLineCount = ta.getInt(R.styleable.ECGPathWaveView_y_line_count, 5);
        xUnitCount = ta.getInt(R.styleable.ECGPathWaveView_x_unit_count, 5);
        yUnitCount = ta.getInt(R.styleable.ECGPathWaveView_y_unit_count, 5);
        xMax = ta.getFloat(R.styleable.ECGPathWaveView_x_max, 200);
        yMax = ta.getFloat(R.styleable.ECGPathWaveView_y_max, 100);
        xMin = ta.getFloat(R.styleable.ECGPathWaveView_x_min, 0);
        yMin = ta.getFloat(R.styleable.ECGPathWaveView_y_min, 0);
        xSpeed = ta.getFloat(R.styleable.ECGPathWaveView_x_speed, 2);
        ySpeed = ta.getFloat(R.styleable.ECGPathWaveView_y_speed, 2);
        mainColor = ta.getColor(R.styleable.ECGPathWaveView_main_line_color, getResources().getColor(R.color.main_color));
        subColor = ta.getColor(R.styleable.ECGPathWaveView_sub_line_color, getResources().getColor(R.color.sub_color));
        waveColor = ta.getColor(R.styleable.ECGPathWaveView_wave_line_color, getResources().getColor(R.color.wave_color));
        mainStrokeWidth = ta.getDimension(R.styleable.ECGPathWaveView_main_stroke_width, 3);
        subStrokeWidth = ta.getDimension(R.styleable.ECGPathWaveView_sub_stroke_width, 1);
        waveStrokeWidth = ta.getDimension(R.styleable.ECGPathWaveView_wave_stroke_width, 1);
        initDraw();
        registerRecycle(context);
        ta.recycle();
    }

    private void registerRecycle(Context context) {
        ((Activity) context).getApplication().registerActivityLifecycleCallbacks(lifecycleCallbacks);
    }

    /**
     * 初始化画笔
     */
    private void initDraw() {
        mainLinePaint.setColor(mainColor);
        mainLinePaint.setStyle(Paint.Style.STROKE);
        mainLinePaint.setStrokeWidth(mainStrokeWidth);

        secondLinePaint.setColor(subColor);
        secondLinePaint.setStrokeWidth(subStrokeWidth);

        waveLinePaint.setColor(waveColor);
        waveLinePaint.setStyle(Paint.Style.STROKE);
        waveLinePaint.setStrokeWidth(waveStrokeWidth);
    }

    private void initMeasure() {
        //x轴范围每个值所代表的像素值
        float perWidth = mWidth / (xMax - xMin);
        travelWidth = perWidth * xSpeed;
        //每个y轴范围值所代表的像素值
        float perHeight = mHeight / (yMax - yMin);
        travelHeight = perHeight * ySpeed;
    }

    /**
     * 画背景
     */
    private void initBackground() {
        Canvas backCanvas = new Canvas();
        backgroundBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        backCanvas.setBitmap(backgroundBitmap);
        waveCanvas.setBitmap(backgroundBitmap);
        perLineWidth = mWidth / (float) (yLineCount - 1);
        perLineHeight = mHeight / (float) (xLineCount - 1);
        perUnitWidth = perLineWidth / xUnitCount;
        perUnitHeight = perLineHeight / yUnitCount;
        for (int i = 0; i <= yLineCount - 1; i++) {
//          画竖线
            backCanvas.drawLine(i * perLineWidth, 0, i * perLineWidth, mHeight, mainLinePaint);
            for (int j = 0; j <= xLineCount - 1; j++) {
                for (int xk = 1; xk < xUnitCount; xk++) {
                    for (int yk = 1; yk < yUnitCount; yk++) {
                        backCanvas.drawPoint(i * perLineWidth + xk * perUnitWidth, j * perLineHeight + yk * perUnitHeight, secondLinePaint);
                    }
                }
                if (i != 0) {
                    continue;
                }
//              画横线
                backCanvas.drawLine(0, j * perLineHeight, mWidth, j * perLineHeight, mainLinePaint);
            }
        }
    }

    public void setXQueue(ArrayQueue arrayQueue) {
        this.xQueue = arrayQueue;
    }

    /**
     * 沿x轴画线
     */
    public void drawXLine() {
        if (LIFECYCLE_STATUE.equals(LifecycleStatue.STOP)) {
            return;
        }
        wavePath.moveTo(travelWidth * xDrawCount, ((yMax - yMin) / 2 / ySpeed) * travelHeight - yTemp / ySpeed * travelHeight);
        yTemp = (float) xQueue.select();
        if (xDrawCount * travelWidth >= mWidth) {
            xDrawCount = 0;
            isXFull = true;
        } else {
            xDrawCount++;
        }
        if (xDrawCount == 0) {
            return;
        }
        wavePath.lineTo(travelWidth * xDrawCount, ((yMax - yMin) / 2 / ySpeed) * travelHeight - yTemp / ySpeed * travelHeight);
        pathMeasure.setPath(wavePath, false);
        if (isXFull)
            clearFirst();
        waveCanvas.drawPath(wavePath, waveLinePaint);
        postInvalidate();
    }

    /**
     * 沿y轴画线
     */
    public void drawYLine(int x) {
    }

    public void drawXYLine(int x, int y) {
    }

    private void clearFirst() {
        if (pathMeasure.getLength() != 0) {
            Path path = new Path();
            while (pathMeasure.nextContour()) {
                pathMeasure.getSegment((float) 0, pathMeasure.getLength(), path, true);
            }
            wavePath = path;
            backgroundBitmap = backCacheBitmap.copy(Bitmap.Config.ARGB_8888, true);
            waveCanvas.setBitmap(backgroundBitmap);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        initBackground();
        backCacheBitmap = backgroundBitmap.copy(Bitmap.Config.ARGB_8888, true);
        initMeasure();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(backgroundBitmap, 0, 0, mainLinePaint);
        waveLinePaint.setStrokeWidth(40);
        canvas.drawLine(0, 0, mWidth, mHeight, waveLinePaint);
//        Bitmap bitmap = backgroundBitmap.copy(Bitmap.Config.ARGB_8888, true);
//        canvas.drawBitmap();
    }
}
