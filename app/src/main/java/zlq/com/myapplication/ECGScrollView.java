package zlq.com.myapplication;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
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

    private int[] data;

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


    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ECGScrollView);
        xTotal = ta.getInt(R.styleable.ECGScrollView_x_total, 200);
        yTotal = ta.getInt(R.styleable.ECGScrollView_y_total, 50);
        xMax = ta.getInt(R.styleable.ECGScrollView_x_max, xTotal);
        yMax = ta.getInt(R.styleable.ECGScrollView_y_max, xTotal);
        stepCount = ta.getInt(R.styleable.ECGScrollView_step_count, 1);
        xTrueTotal = xTotal / stepCount;
        yTrueTotal = yTotal / stepCount;
        data = new int[xTotal];
        initDraw(ta);
        ta.recycle();
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
        mainLinePaint.setStrokeWidth(strokeWidth);

        secondLinePaint.setColor(secondLineColor);
        secondLinePaint.setStrokeWidth(1);

        waveLinePaint.setColor(waveLineColor);
        waveLinePaint.setStrokeWidth(waveLineWidth);
    }

    /**
     * 画背景
     */
    private void initBackground(Canvas canvas) {
//        一个单位的宽的长度
        float perLineWidth = mWidth / (float) xTrueTotal;
//        一个单位的高的长度
        float perLineHeight = mHeight / (float) yTrueTotal;
//         每个宽度单位中的每小格长度
        float perUnitWidth = perLineWidth / stepCount;
//         每个高度单位中的每小格长度
        float perUnitHeight = perLineHeight / stepCount;

        for (int i = 0; i < xTrueTotal; i++) {
//          画竖线
            canvas.drawLine(i * perLineWidth, 0, i * perLineWidth, mHeight, mainLinePaint);
            for (int j = 0; j < yTrueTotal; j++) {
                for (int xk = 1; xk < stepCount; xk++) {
                    for (int yk = 1; yk < stepCount; yk++) {
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

    private void drawPath(int data) {
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mHeight = h;
        mWidth = w;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        initBackground(canvas);
    }


}
