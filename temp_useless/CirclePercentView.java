package com.zclf.tellhow.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.zclf.tellhow.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CirclePercentView extends View {

    //圆的半径
    private float mRadius;

    //色带的宽度
    private float mStripeWidth;
    //总体大小
    private int mHeight;
    private int mWidth;

    //动画位置百分比进度
    private int mCurPercent;

    //圆心坐标
    private float x;
    private float y;

    //要画的弧度
    private int mEndAngle;

    //小圆的颜色
    private int mSmallColor;
    //大圆颜色
    private int mBigColor;

    //中心百分比文字大小
    private float mCenterTextSize;
    private int textHeight;

    Paint bigCirclePaint = new Paint();
    Paint sectorPaint = new Paint();
    Paint smallCirclePaint = new Paint();
    Paint textPaint = new Paint();
    Paint yellowDotPaint = new Paint();

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public CirclePercentView(Context context) {
        this(context, null);
    }

    public CirclePercentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CirclePercentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CirclePercentView, defStyleAttr, 0);
        mStripeWidth = a.getDimension(R.styleable.CirclePercentView_stripeWidth, PxUtils.dpToPx(30, context));
        mCurPercent = a.getInteger(R.styleable.CirclePercentView_percent, 0);
        mSmallColor = a.getColor(R.styleable.CirclePercentView_smallColor, 0xffafb4db);
        mBigColor = a.getColor(R.styleable.CirclePercentView_bigColor, 0xff6950a1);
        mCenterTextSize = a.getDimensionPixelSize(R.styleable.CirclePercentView_centerTextSize, PxUtils.spToPx(20, context));
        mRadius = a.getDimensionPixelSize(R.styleable.CirclePercentView_radius, PxUtils.dpToPx(100, context));
        a.recycle();
        initPaints();
    }

    private void initPaints() {
        bigCirclePaint.setAntiAlias(true);
//        bigCirclePaint.setColor(mBigColor);
        bigCirclePaint.setColor(0xFF73ADD2);

        sectorPaint.setColor(mSmallColor);
        sectorPaint.setAntiAlias(true);

        smallCirclePaint.setAntiAlias(true);
        smallCirclePaint.setColor(mBigColor);

        textPaint.setTextSize(mCenterTextSize);
//        textHeight = (int) Math.ceil(textPaint.getFontMetrics().descent - textPaint.getFontMetrics().top) + 2;
        textHeight = (int) (textPaint.descent() + textPaint.ascent());
        textPaint.setColor(Color.WHITE);

        yellowDotPaint.setColor(0xffffbf00);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //获取测量模式
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        //获取测量大小
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            mRadius = widthSize / 2;
            x = widthSize / 2;
            y = heightSize / 2;
            mWidth = widthSize;
            mHeight = heightSize;
        }

        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            mWidth = (int) (mRadius * 2);
            mHeight = (int) (mRadius * 2);
            x = mRadius;
            y = mRadius;

        }

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        mEndAngle = (int) (mCurPercent * 3.6);
        //绘制大圆
        // canvas.drawCircle(x, y, mRadius, bigCirclePaint);

        //绘制底部一层浅色的线，形成阴影效果
        canvas.drawCircle(x, y+1, mRadius - mStripeWidth, bigCirclePaint);

        //饼状图
       RectF  rect = new RectF(mStripeWidth / 2, mStripeWidth / 2, mWidth - mStripeWidth / 2, mHeight - mStripeWidth / 2);
        canvas.drawArc(rect, 270, mEndAngle, true, sectorPaint);

        //绘制小圆,颜色透明(遮盖形成环形效果，否则是绘制扇形)
        canvas.drawCircle(x, y, mRadius - mStripeWidth, smallCirclePaint);

        //绘制最先前的小黄球
//        圆点坐标：(x0,y0)
//        半径：r
//        角度：a0
//
//        则圆上任一点为：（x1,y1）
//        x1 = x0 + r * cos(ao * 3.14 /180 )
//        y1 = y0 + r * sin(ao * 3.14 /180 )
        canvas.drawCircle((float) (x + (mRadius - mStripeWidth) * Math.cos((mEndAngle - 90) * 3.14 / 180)),
                (float) (y + (mRadius - mStripeWidth) * Math.sin((mEndAngle - 90) * 3.14 / 180)),
                mStripeWidth,
                yellowDotPaint);

        //绘制文本
        String text = mCurPercent + "%";
        float textLength = textPaint.measureText(text);
        canvas.drawText(text, x - textLength / 2, y - textHeight / 2, textPaint);

    }

    //外部设置百分比数
    public void setPercent(int percent) {
        if (percent > 100) {
            throw new IllegalArgumentException("percent must less than 100!");
        }

        setCurPercent(percent);


    }

    //内部设置百分比 用于动画效果
    private void setCurPercent(final int percent) {

        executor.submit(new Runnable() {
            @Override
            public void run() {
                int sleepTime = 10; //slow down anim speed
                if (percent > mCurPercent) {
                    for (int i = mCurPercent; i <= percent; i++) {
                        SystemClock.sleep(sleepTime);
                        mCurPercent = i;
                        CirclePercentView.this.postInvalidate();
                    }
                } else {
                    for (int i = mCurPercent; i > percent; i--) {
                        SystemClock.sleep(sleepTime);
                        mCurPercent = i;
                        CirclePercentView.this.postInvalidate();
                    }
                }
            }
        });
    }
}