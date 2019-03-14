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
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Create by ZhaoLongQing
 * on 2019/3/11.
 */
public class ECGScrollView extends View {

    //x轴总长度
    private int xTotal;
    //y轴总长度
    private int yTotal;
    //设置最高值
    private int xMax;
    private int yMax;

    //每个单位里面多少格
    private int stepCount;
    //总共x/y个单位长度
    private int xTrueTotal;
    private int yTrueTotal;
    //view高
    private int mHeight;
    //view宽
    private int mWidth;
    //线条
    private Paint mainLinePaint = new Paint();
    //副线条（点）
    private Paint secondLinePaint = new Paint();
    //折线
    private Paint waveLinePaint = new Paint();
    //绘制的路径
    private Path wavePath = new Path();
    //path的测量工具
    private PathMeasure pathMeasure = new PathMeasure();
    //背景图
    private Bitmap backgroundBitmap;
    //复制背景图
    private Bitmap backCacheBitmap;
    //        一个单位的宽的长度
    private float perLineWidth;
    //        一个单位的高的长度
    private float perLineHeight;
    //         每个宽度单位中的每小格长度
    private float perUnitWidth;
    //         每个高度单位中的每小格长度
    private float perUnitHeight;
    //         给定一个数据队列
    private ArrayQueue arrayQueue;
    //         绘制波形画板
    private Canvas waveCanvas = new Canvas();
    //绘制次数
    private int drawCount;
    //上一次绘制路径的y值
    private float pathTmp;
    //路径的坐标值
    private float[] point = new float[2];
    //路径的tan角度
    private float[] tan = new float[2];
    private LifeCycle lifeCycle;

    private enum LifeCycle {
        CREATE, START, RESUME, PAUSE, STOP, DESTROY
    }

    ;

    /**
     * 画线
     */
    private boolean isFull = false;

    /**
     * 监听生命周期
     */
    private Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            lifeCycle = LifeCycle.CREATE;
        }

        @Override
        public void onActivityStarted(Activity activity) {
            lifeCycle = LifeCycle.START;
        }

        @Override
        public void onActivityResumed(Activity activity) {
            lifeCycle = LifeCycle.RESUME;
        }

        @Override
        public void onActivityPaused(Activity activity) {
            lifeCycle = LifeCycle.PAUSE;
            backgroundBitmap = backCacheBitmap.copy(Bitmap.Config.ARGB_8888, true);
            waveCanvas.setBitmap(backgroundBitmap);
            ECGScrollView.this.invalidate();
        }

        @Override
        public void onActivityStopped(Activity activity) {
            lifeCycle = LifeCycle.STOP;
            isFull = false;
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            lifeCycle = LifeCycle.DESTROY;
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
        }
    };

    ;

    public ECGScrollView(Context context) {
        super(context);
    }

    public ECGScrollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public ECGScrollView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ECGScrollView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs);
    }


    /**
     * 初始化属性
     */
    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ECGScrollView);
        xTotal = ta.getInt(R.styleable.ECGScrollView_x_total, 200);
        yTotal = ta.getInt(R.styleable.ECGScrollView_y_total, 50);
        xMax = ta.getInt(R.styleable.ECGScrollView_x_max, xTotal);
        yMax = ta.getInt(R.styleable.ECGScrollView_y_max, xTotal);
        stepCount = ta.getInt(R.styleable.ECGScrollView_step_count, 1);
        xTrueTotal = xTotal / stepCount;
        yTrueTotal = yTotal / stepCount;
        initDraw(ta);
        registerRecycle(context);
        ta.recycle();
    }

    private void registerRecycle(Context context) {
        ((Activity) context).getApplication().registerActivityLifecycleCallbacks(callbacks);
    }

    /**
     * 设置队列
     */
    public void setData(ArrayQueue arrayQueue) {
        this.arrayQueue = arrayQueue;
    }

    /**
     * 初始化绘制属性
     */
    private void initDraw(TypedArray ta) {
        int mainLineColor = ta.getColor(R.styleable.ECGScrollView_main_line_color, 0);
        int secondLineColor = ta.getColor(R.styleable.ECGScrollView_second_line_color, 0);
        int waveLineColor = ta.getColor(R.styleable.ECGScrollView_wave_line_color, 0);
        float strokeWidth = ta.getDimension(R.styleable.ECGScrollView_line_width, 2);
        float waveLineWidth = ta.getDimension(R.styleable.ECGScrollView_wave_line_width, 2);
        mainLinePaint.setColor(mainLineColor);
        mainLinePaint.setStyle(Paint.Style.STROKE);
        mainLinePaint.setStrokeWidth(strokeWidth);

        secondLinePaint.setColor(secondLineColor);
        secondLinePaint.setStrokeWidth(1);

        waveLinePaint.setColor(waveLineColor);
        waveLinePaint.setStyle(Paint.Style.STROKE);
        waveLinePaint.setStrokeWidth(waveLineWidth);

    }

    /**
     * 画背景
     */
    private void initBackground() {
        Canvas backCanvas = new Canvas();
        backgroundBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        backCanvas.setBitmap(backgroundBitmap);
        waveCanvas.setBitmap(backgroundBitmap);

        perLineWidth = mWidth / (float) xTrueTotal;
        perLineHeight = mHeight / (float) yTrueTotal;
        perUnitWidth = perLineWidth / stepCount;
        perUnitHeight = perLineHeight / stepCount;

        for (int i = 0; i <= xTrueTotal; i++) {
//          画竖线
            backCanvas.drawLine(i * perLineWidth, 0, i * perLineWidth, mHeight, mainLinePaint);
            for (int j = 0; j <= yTrueTotal; j++) {
                for (int xk = 1; xk < stepCount; xk++) {
                    for (int yk = 1; yk < stepCount; yk++) {
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


    /**
     * 按x轴的最小单位定位y值并画线
     */
    public void drawWave() {
        if (lifeCycle.equals(LifeCycle.PAUSE))
            return;
        wavePath.moveTo(perUnitWidth * drawCount, (yTrueTotal >> 1) * perLineHeight - pathTmp);
        pathTmp = (float) arrayQueue.select();
        if (drawCount * perUnitWidth >= mWidth) {
            drawCount = 0;
            isFull = true;
        } else {
            drawCount += 1;
        }
        if (drawCount == 0) {
            return;
        }
        wavePath.lineTo(perUnitWidth * drawCount, (yTrueTotal >> 1) * perLineHeight - pathTmp);
        pathMeasure.setPath(wavePath, false);
        if (isFull)
            clearFirst();
        waveCanvas.drawPath(wavePath, waveLinePaint);
        postInvalidate();
    }


    /**
     * 删除path最先绘制的第一个路径
     */
    private void clearFirst() {
        if (pathMeasure.getLength() != 0) {
            Path path = new Path();
            while (pathMeasure.nextContour()) {
                pathMeasure.getPosTan(0, point, tan);
                pathMeasure.getSegment((float) 0, pathMeasure.getLength(), path, true);
            }
            wavePath = path;
            backgroundBitmap = backCacheBitmap.copy(Bitmap.Config.ARGB_8888, true);
            waveCanvas.setBitmap(backgroundBitmap);
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mHeight = h;
        mWidth = w;
        initBackground();
        backCacheBitmap = backgroundBitmap.copy(Bitmap.Config.ARGB_8888, false);
        super.onSizeChanged(w, h, oldw, oldh);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(backgroundBitmap, 0, 0, mainLinePaint);
    }


}
