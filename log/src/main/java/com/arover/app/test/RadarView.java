package com.arover.app.test;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.FrameLayout;


/**
 * radar view
 */

public class RadarView extends FrameLayout {

    private static final String TAG = "RadarView";
    private Context mContext;

    private Paint mPaintLine;
    private Paint mPaintSector;
    public boolean isstart = false;
    private ScanThread mThread;
    //旋转效果起始角度
    private int start = 0;

    private Shader mShader;

    private Matrix matrix;

    public final static int CLOCK_WISE = -1;
    public final static int ANTI_CLOCK_WISE = 1;
    private Paint mPaintCircle3;
    private Paint mPaintCircle2;
    private Paint mPaintCircle1;
    private Paint mPaintCircle0;
    private Paint mPaintCircleWhite;
    private int p;
    private int size;
    private int coreRadius;
    private int padding;
    private float circleWidth;
    private int canvasSize;
    private float whiteCircleWith;

    @IntDef({CLOCK_WISE, ANTI_CLOCK_WISE})
    public @interface RADAR_DIRECTION {

    }

    //默认为顺时针呢
    private final static int DEFAULT_DIERCTION = ANTI_CLOCK_WISE;

    //设定雷达扫描方向
    private int direction = DEFAULT_DIERCTION;

    private boolean threadRunning = true;

    public RadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initPaint();
    }

    public RadarView(Context context) {
        super(context);
        mContext = context;
        initPaint();

    }

    private void initPaint() {
        whiteCircleWith = dp2px(2);
        coreRadius = dp2px(7);

        setBackgroundColor(Color.TRANSPARENT);

        mPaintLine = new Paint();
        mPaintLine.setStrokeWidth(dp2px(2));
        mPaintLine.setAntiAlias(true);
        mPaintLine.setStyle(Paint.Style.FILL);
        mPaintLine.setColor(0xFF3B83B8);

        mPaintCircle3 = new Paint();
        mPaintCircle3.setAntiAlias(true);
        mPaintCircle3.setStyle(Paint.Style.STROKE);
        mPaintCircle3.setColor(0xfffafcfd);

        mPaintCircle2 = new Paint();
        mPaintCircle2.setAntiAlias(true);
        mPaintCircle2.setStyle(Paint.Style.STROKE);
        mPaintCircle2.setColor(0xffe9f0f7);

        mPaintCircle1 = new Paint();
        mPaintCircle1.setAntiAlias(true);
        mPaintCircle1.setStyle(Paint.Style.FILL);
        mPaintCircle1.setColor(0xffd5e4ef);

        mPaintCircle0 = new Paint();
        mPaintCircle0.setAntiAlias(true);
        mPaintCircle0.setStyle(Paint.Style.FILL);
        mPaintCircle0.setColor(0xff3f84b6);


        mPaintCircleWhite = new Paint();
        mPaintCircleWhite.setStrokeWidth(whiteCircleWith);
        mPaintCircleWhite.setAntiAlias(true);
        mPaintCircleWhite.setStyle(Paint.Style.STROKE);
        mPaintCircleWhite.setColor(0xffffffff);

        //暗绿色的画笔
        mPaintSector = new Paint();
        mPaintSector.setColor(0xFF3B83B8);
        mPaintSector.setAntiAlias(true);

    }

    private int dp2px(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = getMeasuredWidth();
        setMeasuredDimension(size, size);
        initSizes(size);
    }

    private void initSizes(int size) {
        this.size = size;
        p = size / 2;
        padding = dp2px(16);
        int core = dp2px(8);
        canvasSize = size - padding * 2;
        circleWidth = canvasSize / 6.0f;
        Log.d(TAG, "size = " + size + ",circleWidth=" + circleWidth + ",canvasSize=" + canvasSize);
        mPaintCircle3.setStrokeWidth(circleWidth);
        mPaintCircle2.setStrokeWidth(circleWidth);

    }


    public void start() {
        mThread = new ScanThread(this);
        mThread.setName("radar");
        mThread.start();
        threadRunning = true;
        isstart = true;
    }

    public void stop() {
        if (isstart) {
            threadRunning = false;
            isstart = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mShader == null) {
            //AE3B83B8 0xFF3B83B8
            mShader = new SweepGradient(p, p, Color.TRANSPARENT, 0xAE3B83B8);
            mPaintSector.setShader(mShader);
        }
        canvas.drawCircle(p, p, circleWidth * 2.5f, mPaintCircle3);
        canvas.drawCircle(p, p, circleWidth*1.5f, mPaintCircle2);
        canvas.drawCircle(p, p, circleWidth, mPaintCircle1);

        canvas.drawCircle(p, p, coreRadius, mPaintCircle0);
        canvas.drawCircle(p, p, coreRadius, mPaintCircleWhite);

        //根据matrix中设定角度，不断绘制shader,呈现出一种扇形扫描效果
        canvas.concat(matrix);
        canvas.drawLine(p, p, p+canvasSize/2.0f, p, mPaintLine);
        canvas.drawCircle(p, p, canvasSize/2.0f, mPaintSector);
        super.onDraw(canvas);
    }

    public void setDirection(@RADAR_DIRECTION int direction) {
        if (direction != CLOCK_WISE && direction != ANTI_CLOCK_WISE) {
            throw new IllegalArgumentException("Use @RADAR_DIRECTION constants only!");
        }
        this.direction = direction;
    }

    protected class ScanThread extends Thread {

        private RadarView view;

        public ScanThread(RadarView view) {

            this.view = view;
        }

        @Override
        public void run() {

            while (threadRunning) {
                if (isstart) {
                    view.post(new Runnable() {
                        public void run() {
                            start = start + 1;
                            matrix = new Matrix();
                            //设定旋转角度,制定进行转转操作的圆心
//                            matrix.postRotate(start, viewSize / 2, viewSize / 2);
//                            matrix.setRotate(start,viewSize/2,viewSize/2);
                            matrix.preRotate(direction * start, size / 2.0f, size / 2.0f);
                            view.invalidate();

                        }
                    });
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

}
