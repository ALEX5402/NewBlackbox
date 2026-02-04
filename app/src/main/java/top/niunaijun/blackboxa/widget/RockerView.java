package top.niunaijun.blackboxa.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import top.niunaijun.blackboxa.util.MathUtil;



public class RockerView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private static final int DEFAULT_AREA_RADIUS = 100;
    private static final int DEFAULT_ROCKER_RADIUS = 35;

    private static final int DEFAULT_AREA_COLOR = Color.argb(128,0,0,0);
    private static final int DEFAULT_ROCKER_COLOR = Color.argb(128,0,0,0);

    private static final int DEFAULT_REFRESH_CYCLE = 30;
    private static final int DEFAULT_CALLBACK_CYCLE = 300;

    private SurfaceHolder mHolder;
    private static Thread mDrawThread;
    private static Thread mCallbackThread;
    private static boolean mDrawOk = true;
    private static boolean mCallbackOk = true;

    private Paint mPaint;

    
    private Point mAreaPosition;

    
    private Point mRockerPosition;


    private int mAreaRadius = -1;
    private int mRockerRadius = -1;

    private int mAreaColor;
    private int mRockerColor;
    private Bitmap mAreaBitmap;
    private Bitmap mRockerBitmap;

    private boolean canMove = true;


    private RockerListener mListener;
    public static final int EVENT_ACTION = 1;
    public static final int EVENT_CLOCK = 2;

    private int mRefreshCycle = DEFAULT_REFRESH_CYCLE;
    private int mCallbackCycle = DEFAULT_CALLBACK_CYCLE;


    

    public RockerView(Context context) {
        this(context, null);
    }

    public RockerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RockerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        
        initAttrs(context, attrs);

        
        setPaint();

        if (isInEditMode()) {
            return;
        }

        
        configSurfaceView();

        
        configSurfaceHolder();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        mAreaColor = DEFAULT_AREA_COLOR;
        mRockerColor = DEFAULT_ROCKER_COLOR;
        mAreaRadius = DEFAULT_AREA_RADIUS;
        mRockerRadius = DEFAULT_ROCKER_RADIUS;

    }

    private void setPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    private void configSurfaceView() {
        setKeepScreenOn(true);          
        setFocusable(true);             
        setFocusableInTouchMode(true);  
        setZOrderOnTop(true);           
    }

    private void configSurfaceHolder() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setFormat(PixelFormat.TRANSPARENT); 
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth = 0, measureHeight = 0;
        int defaultWidth = (mAreaRadius + mRockerRadius) * 2;
        int defalutHeight = defaultWidth;

        int widthsize = MeasureSpec.getSize(widthMeasureSpec);      
        int widthmode = MeasureSpec.getMode(widthMeasureSpec);      

        int heightsize = MeasureSpec.getSize(heightMeasureSpec);    
        int heightmode = MeasureSpec.getMode(heightMeasureSpec);    

        if (widthmode == MeasureSpec.AT_MOST || widthmode == MeasureSpec.UNSPECIFIED || widthsize < 0) {
            measureWidth = defaultWidth;
        } else {
            measureWidth = widthsize;
        }


        if (heightmode == MeasureSpec.AT_MOST || heightmode == MeasureSpec.UNSPECIFIED || heightsize < 0) {
            measureHeight = defalutHeight;
        } else {
            measureHeight = heightsize;
        }

        setMeasuredDimension(measureWidth, measureHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mAreaPosition = new Point(w / 2, h / 2);
        mRockerPosition = new Point(mAreaPosition);

        
        int tempRadius = Math.min(w - getPaddingLeft() - getPaddingRight(), h - getPaddingTop() - getPaddingBottom());
        tempRadius /= 2;
        if (mAreaRadius == -1)
            mAreaRadius = (int) (tempRadius * 0.75);
        if (mRockerRadius == -1)
            mRockerRadius = (int) (tempRadius * 0.25);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mDrawThread = new Thread(this);
            mDrawThread.start();

            mCallbackThread = new Thread(() -> {
                while (mCallbackOk) {

                    
                    listenerCallback();

                    try {
                        Thread.sleep(mCallbackCycle);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            mCallbackThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mDrawOk = false;
        mCallbackOk = false;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            mDrawOk = true;
            mCallbackOk = true;
        } else {
            mDrawOk = false;
            mCallbackOk = false;
        }
    }

    

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            int len = MathUtil.getDistance(mAreaPosition.x, mAreaPosition.y, event.getX(), event.getY());

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                
                if (len > mAreaRadius) {
                    return true;
                }
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (len <= mAreaRadius) {
                    
                    mRockerPosition.set((int) event.getX(), (int) event.getY());

                } else {
                    
                    mRockerPosition = MathUtil.getPointByCutLength(mAreaPosition,
                            new Point((int) event.getX(), (int) event.getY()), mAreaRadius);
                }
                if (mListener != null) {
                    float radian = MathUtil.getRadian(mAreaPosition, new Point((int) event.getX(), (int) event.getY()));
                    float angle = RockerView.this.getAngleConvert(radian);
                    float distance = MathUtil.getDistance(mAreaPosition.x, mAreaPosition.y, event.getX(), event.getY());
                    mListener.callback(EVENT_ACTION, angle, distance);
                }
            }
            
            if (event.getAction() == MotionEvent.ACTION_UP) {
                mRockerPosition = new Point(mAreaPosition);
                if (mListener != null) {
                    mListener.callback(EVENT_ACTION, -1, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }


    

    @Override
    public void run() {
        if (isInEditMode()) {
            return;
        }

        Canvas canvas = null;

        while (mDrawOk) {
            boolean canMove = this.canMove;
            try {
                if (canMove) {
                    canvas = mHolder.lockCanvas();
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    drawArea(canvas);
                    drawRocker(canvas);
                }
                Thread.sleep(mRefreshCycle);    

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null && canMove) {
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void drawArea(Canvas canvas) {

        if (null != mAreaBitmap) {
            mPaint.setColor(Color.BLACK);
            Rect src = new Rect(0, 0, mAreaBitmap.getWidth(), mAreaBitmap.getHeight());
            Rect dst = new Rect(
                    mAreaPosition.x - mAreaRadius,
                    mAreaPosition.y - mAreaRadius,
                    mAreaPosition.x + mAreaRadius,
                    mAreaPosition.y + mAreaRadius);
            canvas.drawBitmap(mAreaBitmap, src, dst, mPaint);
        } else {
            mPaint.setColor(mAreaColor);
            canvas.drawCircle(mAreaPosition.x, mAreaPosition.y, mAreaRadius, mPaint);
        }
    }

    private void drawRocker(Canvas canvas) {
        if (null != mRockerBitmap) {
            mPaint.setColor(Color.BLACK);
            Rect src = new Rect(0, 0, mRockerBitmap.getWidth(), mRockerBitmap.getHeight());
            Rect dst = new Rect(
                    mRockerPosition.x - mRockerRadius,
                    mRockerPosition.y - mRockerRadius,
                    mRockerPosition.x + mRockerRadius,
                    mRockerPosition.y + mRockerRadius);
            canvas.drawBitmap(mRockerBitmap, src, dst, mPaint);
        } else {
            mPaint.setColor(mRockerColor);
            canvas.drawCircle(mRockerPosition.x, mRockerPosition.y, mRockerRadius, mPaint);
        }
    }

    private void listenerCallback() {
        if (mListener != null) {
            if (mRockerPosition.x == mAreaPosition.x && mRockerPosition.y == mAreaPosition.y) {
                mListener.callback(EVENT_CLOCK, -1, 0);
            } else {
                float radian = MathUtil.getRadian(mAreaPosition, new Point(mRockerPosition.x, mRockerPosition.y));
                float angle = RockerView.this.getAngleConvert(radian);
                float distance = MathUtil.getDistance(mAreaPosition.x, mAreaPosition.y, mRockerPosition.x, mRockerPosition.y);
                mListener.callback(EVENT_CLOCK, angle, distance);
            }
        }
    }

    
    private float getAngleConvert(float radian) {
        return 90 + Math.round(radian / Math.PI * 180);
    }

    
    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            canvas.drawColor(Color.WHITE);
            drawArea(canvas);
            drawRocker(canvas);
        }
    }

    

    public void setCanMove(boolean isMove) {
        this.canMove = isMove;
    }

    public int getAreaRadius() {
        return mAreaRadius;
    }

    public void setAreaRadius(int areaRadius) {
        mAreaRadius = areaRadius;
    }

    public int getRockerRadius() {
        return mRockerRadius;
    }

    public void setRockerRadius(int rockerRadius) {
        mRockerRadius = rockerRadius;
    }

    public Bitmap getAreaBitmap() {
        return mAreaBitmap;
    }

    public void setAreaBitmap(Bitmap areaBitmap) {
        mAreaBitmap = areaBitmap;
    }

    public Bitmap getRockerBitmap() {
        return mRockerBitmap;
    }

    public void setRockerBitmap(Bitmap rockerBitmap) {
        mRockerBitmap = rockerBitmap;
    }

    public int getRefreshCycle() {
        return mRefreshCycle;
    }

    public void setRefreshCycle(int refreshCycle) {
        mRefreshCycle = refreshCycle;
    }

    public int getCallbackCycle() {
        return mCallbackCycle;
    }

    public void setCallbackCycle(int callbackCycle) {
        mCallbackCycle = callbackCycle;
    }

    public int getAreaColor() {
        return mAreaColor;
    }

    public void setAreaColor(int areaColor) {
        mAreaColor = areaColor;
        mAreaBitmap = null;
    }

    public int getRockerColor() {
        return mRockerColor;
    }

    public void setRockerColor(int rockerColor) {
        mRockerColor = rockerColor;
        mRockerBitmap = null;
    }

    public void setListener(@NonNull RockerListener listener) {
        mListener = listener;
    }

    

    
    public interface RockerListener {

        
        void callback(int eventType, float currentAngle, float currentDistance);
    }

}