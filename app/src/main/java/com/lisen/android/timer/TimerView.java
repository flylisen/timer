package com.lisen.android.timer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Administrator on 2016/7/19.
 */
public class TimerView extends View {

    /**
     * 小球
     */
    private Bitmap mBall;
    /**
     * 提示文本
     */
    private String mHint = "";
    /**
     * 倒计时时间，单位为S，默认为一个小时
     */
    private int mTotalTime = 60 * 60;
    /**
     * 小球绕圆心旋转角度
     */
    private float mAngle = 0.f;
    /**
     * 考虑小球半径
     */
    private int mDx;
    public TimerView(Context context) {
        this(context, null);
    }

    public TimerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBall = BitmapFactory.decodeResource(getResources(), R.drawable.dot_focus);
        mDx = mBall.getWidth() / 2;
        //设置画笔 抗锯齿
        mPaint.setAntiAlias(true);

    }

    private String mTimeText = "01:00:00";

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureWidth(widthMeasureSpec);
        int height = measureWidth(heightMeasureSpec);
        setMeasuredDimension(width, height);

    }

    private int measureWidth(int widthMeasureSpec) {
        int result;
        int measureMode = MeasureSpec.getMode(widthMeasureSpec);
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (measureMode == MeasureSpec.EXACTLY) {
            result = measureWidth;
        } else {
            result = 200;
            if (measureMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, measureWidth);
            }
        }

        return result;
    }

    Paint mPaint = new Paint();



    @Override
    protected void onDraw(final Canvas canvas) {

        canvas.save();
        canvas.translate(getMeasuredWidth() / 2, getMeasuredHeight() / 2);
        //画时间文本
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(10);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(35);
        float textWidth = mPaint.measureText(mTimeText);
        float textHeight = (mPaint.descent() + mPaint.ascent()) / 2;
        canvas.drawText(mTimeText, -textWidth / 2, textHeight / 2, mPaint);

        //画圆
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(5);
        mPaint.setAlpha(50);
        canvas.drawCircle(0, 0, 200, mPaint);

        //画小球
        float location[] = getNewLocation(mAngle, 200);

        canvas.drawBitmap(mBall, location[0] - mDx, location[1] - mDx, null);


        //画提示语
        canvas.translate(0, 300);
        float hintTextWidth = mPaint.measureText(mHint);
        float hintTextHeight = (mPaint.descent() + mPaint.ascent()) / 2;
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(30);
        canvas.drawText(mHint, -hintTextWidth / 2, hintTextHeight / 2, mPaint);
        canvas.restore();

    }

    /**
     * 更新视图
     * @param angle     角度
     * @param count     时间计量
     */
    public void updateView(float angle, int count) {
        if (count == 0) {
            mTotalTime--;
            this.mTimeText = formatTime(mTotalTime);
        }

        mAngle = angle;

        invalidate();

    }

    /**
     * 得到还需倒计的时间
     * @return
     */
    public int getLeftTime() {
        return mTotalTime;
    }

    /**
     * 确定小球的圆心位置
     * @param angle
     * @param radius
     * @return
     */
    private float[] getNewLocation(float angle, int radius) {
        float newX = 0;
        float newY = 0;
        angle = angle % 360;

        if (angle == 0) {
            newX = 0;
            newY = -radius;
        } else if (angle == 90.0f) {
            newX = radius;
            newY = 0;
        } else if (angle == 180.0f) {
            newX = 0;
            newY = radius;
        } else if (angle == 270.0f) {
            newX = -radius;
            newY = 0;
        } else if (angle > 0 && angle < 90) {
            newX = (float) (radius * Math.sin(angle * Math.PI / 180));
            newY = (float) (-radius * Math.cos(angle * Math.PI / 180));
        } else if (angle > 90.0f && angle < 180.0f) {
            float newAngle = angle - 90;
            newX = (float) (radius * Math.cos(newAngle * Math.PI / 180));
            newY = (float) (radius * Math.sin(newAngle * Math.PI / 180));
        } else if (angle > 180.0f && angle < 270.0f) {
            float newAngle = angle - 180;
            newX = (float) (-radius * Math.sin(newAngle * Math.PI / 180));
            newY = (float) (radius * Math.cos(newAngle * Math.PI / 180));

        } else if (angle > 270.0f && angle < 360.0f) {
            float newAngle = angle - 270;
            newX = (float) (-radius * Math.cos(newAngle * Math.PI / 180));
            newY = (float) (-radius * Math.sin(newAngle * Math.PI / 180));
        }

        return new float[] {newX, newY};
    }

    /**
     * 将时间有秒转化为时：分：秒 格式
     * @param duration
     * @return
     */
    private String formatTime(int duration) {
        String result;
        if (duration == -1) {
            result = getResources().getString(R.string.time_is_up);
            mHint = "";
            if (mOnTaskFinishListener != null) {

                mOnTaskFinishListener.onTaskFinish();
            }
            return result;
        }
        int hours = duration / (60 * 60);
        int leftSeconds = duration % (60 * 60);
        int minutes = leftSeconds / 60;
        int seconds = leftSeconds % 60;
        StringBuffer buffer = new StringBuffer();
        buffer.append(addZeroPrefix(hours));
        buffer.append(":");
        buffer.append(addZeroPrefix(minutes));
        buffer.append(":");
        buffer.append(addZeroPrefix(seconds));
        return buffer.toString();
    }

    private String addZeroPrefix(int number) {
        if (number < 10) {
            return "0" + number;
        } else {
            return "" + number;
        }
    }

    /**
     * 重置view
     */
    public void reSetView(int duration) {
        mTotalTime = duration;
        mTimeText = formatTime(mTotalTime);
        mAngle = 0;
        mHint = "";
        invalidate();
    }

    /**
     * 根据按钮状态设置提示文本内容
     * @param hint
     */
    public void setHintText(String hint) {
        mHint = hint;
        invalidate();
    }

    /**
     * 供倒计时完成回调接口
     */
    public interface OnTaskFinishListener {
        void onTaskFinish();
    }

    private OnTaskFinishListener mOnTaskFinishListener;

    public void setOnTaskFinishListener(OnTaskFinishListener l) {
        mOnTaskFinishListener = l;
    }
}
