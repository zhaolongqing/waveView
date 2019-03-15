package com.creative.draw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class BaseSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

	/** 设置波形平滑 */
	protected CornerPathEffect cornerPathEffect = new CornerPathEffect(20);

	protected SurfaceHolder mHolder;

	/** 绘制波形的画笔 */
	private Paint mWavePaint;

	/** view宽 */
	protected int mSurfaceViewWidth;

	/** view高 */
	protected int mSurfaceViewHeight;

	protected DisplayMetrics mDisplayMetrics;

	/** 每mm所占的像素 单位：像素点/mm , px/mm */
	protected float mResx = 0.0f;

	/** 像素转毫米的单位 ,px -> mm */
	protected float PX2MMUnit = 0.0f;

	protected Paint mScanPaint;

	/** 波形缓冲区, wave buffer area */
	protected Bitmap mBufferBitmap;

	/** 图像缓冲画板, bitmap buffer area */
	protected Canvas mBufferCanvas;
	
	/** 绘制背景的图片 */
	private Bitmap mBackgroundBitmap;

	/** 绘制背景网格的画笔 */
	private Paint mGridPaint;

	/** 绘制心电增益的画笔 */
	private Paint mGainPaint;

	/** 背景颜色 */
	public static int mBackgroundColor = Color.WHITE;

	/** 是否绘制背景网格, wether or not draw background grid */
	private boolean bDrawGrid = true;

	/**
	 * 背景网格一格的高度(5mm对应的像素值),1mV对应5mm。
	 * 1 small grid height -> 5 mm
	 */
	private float gridHeight = 0.0f;

	/** 是否绘制ECG增益标尺,wether or not draw gain */
	private boolean bDrawGain = false;

	/** 心电波形增益, ECG wave gain */
	protected int mGain = 2;
	
	/**
	 * 两点之间的步长 由它控制波形走速(血氧spo2用到) 
	 * spo2 step
	 */
	protected float step = 0.0f;

	
	public BaseSurfaceView(Context context) {
		super(context);
		init(context);
	}

	public BaseSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public BaseSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		mHolder = getHolder();
		mHolder.addCallback(this);

		mDisplayMetrics = context.getResources().getDisplayMetrics();

		//波形画笔, wave paint
		mWavePaint = new Paint();
		mWavePaint.setAntiAlias(true);
		mWavePaint.setStyle(Style.STROKE);
		mWavePaint.setColor(Color.RED);
		mWavePaint.setStrokeWidth(mDisplayMetrics.density * 2);
		mWavePaint.setPathEffect(cornerPathEffect);

		//扫描画笔, scan paint
		mScanPaint = new Paint();
		mScanPaint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));

		PX2MMUnit = 25.4f / mDisplayMetrics.densityDpi;
		mResx = mDisplayMetrics.densityDpi / 25.4f;

		//网格画笔, grid paint
		mGridPaint = new Paint();
		mGridPaint.setAntiAlias(true);
		mGridPaint.setColor(Color.GRAY);
		mGridPaint.setStrokeWidth(mDisplayMetrics.density);

		//增益画笔
		mGainPaint = new Paint(mGridPaint);
		mGainPaint.setColor(Color.BLUE);
		
		gridHeight = fMMgetPx(5);
	}

	/**
	 * 绘制背景网格
	 */
	private void drawBackground(Canvas canvas) {
		canvas.drawColor(mBackgroundColor);
		if (!bDrawGrid)
			return;

		// 绘制竖线, vertical line
		for (float i = 0f; i < mSurfaceViewWidth; i += gridHeight) {
			canvas.drawLine(i, 0, i, mSurfaceViewHeight, mGridPaint);
		}

		// 绘制横线,horizontal line
		for (float i = mSurfaceViewHeight / 2f; i >= 0; i -= gridHeight) {
			canvas.drawLine(0, i, mSurfaceViewWidth, i, mGridPaint);
		}

		for (float i = mSurfaceViewHeight / 2f; i <= mSurfaceViewHeight; i += gridHeight) {
			canvas.drawLine(0, i, mSurfaceViewWidth, i, mGridPaint);
		}

		if (bDrawGain) {
			// 绘制增益, draw gain
			float i = (gridHeight * mGain) / 2f;
			canvas.drawLine(0, mSurfaceViewHeight / 2 - i, gridHeight / 2, mSurfaceViewHeight / 2 - i, mGainPaint);
			canvas.drawLine(0, mSurfaceViewHeight / 2 + i, gridHeight / 2, mSurfaceViewHeight / 2 + i, mGainPaint);
			canvas.drawLine(gridHeight / 4, mSurfaceViewHeight / 2 - i, gridHeight / 4, mSurfaceViewHeight / 2 + i,
					mGainPaint);
		}
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder paramSurfaceHolder, int paramInt1, int paramInt2, int paramInt3) {
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mSurfaceViewWidth = this.getWidth();
		mSurfaceViewHeight = this.getHeight();
		
		mBufferBitmap = Bitmap.createBitmap(mSurfaceViewWidth, mSurfaceViewHeight, Config.ARGB_8888);
		mBackgroundBitmap = Bitmap.createBitmap(mSurfaceViewWidth, mSurfaceViewHeight, Config.ARGB_8888);
		mBufferCanvas = new Canvas(mBackgroundBitmap);
		drawBackground(mBufferCanvas);

		mBufferCanvas.setBitmap(mBufferBitmap);
		Canvas c = holder.lockCanvas();
		c.drawBitmap(mBackgroundBitmap, 0, 0, mWavePaint);
		holder.unlockCanvasAndPost(c);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	/**
	 * 将毫米转成像素, mm -> px
	 */
	protected float fMMgetPx(float mm) {
		return mm / PX2MMUnit;
	}

	/**
	 * 将像素转换为毫米, px -> mm
	 */
	protected float fPXgetMM(int px) {
		return px * PX2MMUnit;
	}

	private Path mPath = new Path();
	private Point p = new Point();
	private int count = 0;

	private Rect mRect = new Rect();
	
	//一次画多条线
	public void addData(int[] data) {
		synchronized (this) {
			int size = data.length; 
			
			// 绘制矩形范围
			mRect.set(p.x-5, 0, (p.x + size + 50), mSurfaceViewHeight);

			Canvas c = mHolder.lockCanvas(mRect);
			if (c == null)
				return;

			for (int i = 0; i < size; i++) {
				int x = (int) getX(count);
				int y = (int) getY(data[i]);

				count++;
				if (x < mSurfaceViewWidth) { // 小于可视区的宽
					// mPath.reset();
					// mPath.moveTo(p.x, p.y);
					// mPath.quadTo((p.x + x) / 2f, (p.y + y) / 2f, x, y);
					// mBufferCanvas.drawPath(mPath, wavePaint);

					mBufferCanvas.drawLine(p.x, p.y, x, y, mWavePaint);
				} else { // 大于可视区的宽
					count = 0;
					x = 0;
					mBufferCanvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
					mBufferCanvas.drawRect(0, 0, 20, mSurfaceViewWidth, mScanPaint);
				}
				// 记录当前点
				p.x = x;
				p.y = y;
			}
			
			c.drawBitmap(mBackgroundBitmap, 0, 0, mWavePaint);
			c.drawBitmap(mBufferBitmap, 0, 0, mWavePaint);
			mHolder.unlockCanvasAndPost(c);				
		}
	}
	
	
	//一次画1条线,bitmap绘图
	public void addData(int data) {
		synchronized (this) {
			int x = (int) getX(count);
			int y = (int) getY(data);
			
			// 绘制矩形范围
			mRect.set(p.x - 5, 0, (x + 10), mSurfaceViewHeight);

			Canvas c = mHolder.lockCanvas(mRect);
			if (c == null)
				return;

			count++;
			if (x < mSurfaceViewWidth) { // 小于可视区的宽
				mPath.reset();
				mPath.moveTo(p.x, p.y);
				mPath.quadTo((p.x + x) / 2f, (p.y + y) / 2f, x, y);
				mBufferCanvas.drawPath(mPath, mWavePaint);
			} else { // 大于可视区的宽
				count = 0;
				x = 0;
				mBufferCanvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
				mBufferCanvas.drawRect(0, 0, 20, mSurfaceViewWidth, mScanPaint);
			}
			// 绘制扫描线
			//mBufferCanvas.drawRect(x + 5, 0, x + 10, mSurfaceViewHeight, mScanPaint);
			
			// 记录当前点
			p.x = x;
			p.y = y;

			c.drawBitmap(mBackgroundBitmap, 0, 0, mWavePaint);
			c.drawBitmap(mBufferBitmap, 0, 0, mWavePaint);
			mHolder.unlockCanvasAndPost(c);
		}
	}
	
//	//一次画1条线， 无bitmap绘图
//	private int mScanLineWidth =5;
//	public void addData2(int data) {
//		synchronized (this) {
//			int newX = (int) getX(count);
//			int newY = (int) getY(data);
//			
//			count++;	
//						
//			mRect.set(p.x-2, 0, (newX + mScanLineWidth), mSurfaceViewHeight);		
//					
//			Canvas c = mHolder.lockCanvas(mRect);
//			
//			drawBackground(c);				  			 
//			
//			if(newX < mSurfaceViewWidth){
//				c.drawLine(p.x, p.y, newX, newY, mWavePaint);
//			}else {
//				count = 0;
//				newX=0;
//			}			
//			p.x = newX;
//			p.y = newY;		
//			
//			mHolder.unlockCanvasAndPost(c);			
//		}
//	}
	
	public void clean() {
		synchronized (mHolder) {
			if (mBufferCanvas != null) {
				mBufferCanvas.drawColor(Color.WHITE, Mode.CLEAR);
				Canvas c = mHolder.lockCanvas();
				if (c != null) {
					c.drawBitmap(mBufferBitmap, 0, 0, mWavePaint);
				}
				mHolder.unlockCanvasAndPost(c);
			}
		}
	}

	/**
	 * 计算该点在Y轴上的坐标
	 */
	public abstract float getY(int data);

	public abstract float getX(int data);

	/**
	 * 是否绘制背景网格
	 */
	public boolean isDrawGrid() {
		return bDrawGrid;
	}

	/**
	 * 设置是否绘制背景上的网格
	 */
	public void setDrawGrid(boolean drawGrid) {
		bDrawGrid = drawGrid;
	}

	/**
	 * 是否绘制增益标尺
	 */
	public boolean isDrawGain() {
		return bDrawGain;
	}

	/**
	 * 设置是否绘制增益标尺
	 */
	public void setDrawGain(boolean isDrawGain) {
		this.bDrawGain = isDrawGain;
	}

	/**
	 * 获取心电增益
	 */
	public int getGain() {
		return mGain;
	}

	/**
	 * 设置心电增益
	 */
	public void setGain(int gain) {
		this.mGain = gain;
	}

	/**
	 * 返回绘制背景网格的画笔
	 */
	public Paint getmGridPaint() {
		return mGridPaint;
	}

	/**
	 * 设置绘制背景网格的画笔
	 */
	public void setmGridPaint(Paint mGridPaint) {
		this.mGridPaint = mGridPaint;
	}

	/**
	 * 获取增益的画笔
	 */
	public Paint getmGainPaint() {
		return mGainPaint;
	}

	/**
	 * 设置增益的画笔
	 */
	public void setmGainPaint(Paint mGainPaint) {
		this.mGainPaint = mGainPaint;
	}

	/**
	 * 获取背景颜色
	 */
	public int getmBackgroundColor() {
		return mBackgroundColor;
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mSurfaceViewWidth= w;
		mSurfaceViewHeight = h ;
	}

	
}
