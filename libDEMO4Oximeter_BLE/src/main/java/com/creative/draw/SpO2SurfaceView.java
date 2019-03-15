package com.creative.draw;

import android.content.Context;
import android.util.AttributeSet;

public class SpO2SurfaceView extends BaseSurfaceView {

	/**
	 * 血氧波形最大值
	 */
	private int nMax;

	/**
	 * 血氧波形最小值
	 */
	private int nMin;

	/**
	 * 波形缩放比例
	 */
	private float zoom;

	/**
	 * 根据取值范围计算出的0到最大值对应的View高度
	 */
	private float mViewH;

	public SpO2SurfaceView(Context context) {
		super(context);
	}

	public SpO2SurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SpO2SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public float getY(int data) {
		if (zoom == 0) {
			zoom = (float) mSurfaceViewHeight / (nMax - nMin);
			mViewH = zoom * nMax;
		}
		return mViewH - zoom * data * mGain;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		step = mDisplayMetrics.density * 2;
		mGain = 1;
	}

	/**
	 * 设置X轴上波形的步长。值越大波形越宽，走速越快。默认为density*2
	 */
	public void setStep(int step) {
		this.step = step;
	}

	/**
	 * 设置血氧波形的取值范围
	 * 
	 * @param max
	 *            最大值
	 * @param min
	 *            最小值
	 */
	public void setScope(int max, int min) {
		nMax = max;
		nMin = min;
	}

	@Override
	public float getX(int data) {
		return data * step;
	}	
	
}
