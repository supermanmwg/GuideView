package com.guidepage.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Created by weiguangmeng on 16/3/20.
 */
public class HorizontalScrollView extends ViewGroup {

    private static final String TAG = "HorizontalScrollView";
    int mChildIndex = 0;
    int mChildWidth;
    int mChildSize;

    int mLastX;
    int mLastY;
    int mLastInterceptX;
    int mLastInterceptY;
    int mOffset;
    boolean mIsInit = false;

    VelocityTracker mVelocityX;
    Scroller mScroller;

    public HorizontalScrollView(Context context) {
        super(context);
        init();
    }

    public HorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        mIsInit = true;
    }


    private void init() {
        mVelocityX = VelocityTracker.obtain();
        mScroller = new Scroller(getContext());
        mOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercepted = false;
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    intercepted = true;
                }
                intercepted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltx = x - mLastX;
                int delty = y - mLastY;
                if (Math.abs(deltx) > Math.abs(delty)) {
                    intercepted = true;
                } else {
                    intercepted = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                intercepted = false;
                break;
        }

        mLastX = x;
        mLastY = y;
        mLastInterceptX = x;
        mLastInterceptY = y;

        return intercepted;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mVelocityX.addMovement(event);
        int action = event.getAction();
        int x = (int) event.getX();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = x - mLastInterceptX;
                scrollBy(-deltaX, 0);
                break;
            case MotionEvent.ACTION_UP:
                int scrollX = getScrollX();
                mVelocityX.computeCurrentVelocity(1000);
                float velocityX = mVelocityX.getXVelocity();
                int mLastChildIndex = mChildIndex;
                if (Math.abs(velocityX) >= 50) {
                    mChildIndex = velocityX > 0 ? mChildIndex - 1 : mChildIndex + 1;
                } else {
                    mChildIndex = (scrollX + mChildWidth / 2) / mChildWidth;
                }

                mChildIndex = Math.max(0, Math.min(mChildIndex, mChildSize - 1));
                int offset = 0;
                if(mLastChildIndex != 0 && mLastChildIndex != mChildSize - 1) {
                    offset =  (mLastChildIndex > mChildIndex ? mOffset :  0 - mOffset);

                }

                if(mChildIndex == 0 && mLastChildIndex == 0) {
                    offset = mOffset;
                }

                if(mChildIndex == mChildSize -1 && mLastChildIndex == mChildSize -1 ) {
                    offset = 0 - mOffset;
                }
                int dx = mChildIndex * mChildWidth - scrollX + offset;
                setSmoothScroll(dx ,0, 500);

                mVelocityX.clear();  //reset velocity
                break;
        }

        mLastInterceptX = x;
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int measureWidth = 0;
        int measureHeight = 0;

        mChildSize = getChildCount();

        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int widthSpaceMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpaceSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpaceMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpaceSize = MeasureSpec.getSize(heightMeasureSpec);

        if(mChildSize == 0)  {
            setMeasuredDimension(0, 0);
        } else if(widthSpaceMode == MeasureSpec.AT_MOST && heightSpaceMode == MeasureSpec.AT_MOST) {
            final View childView = getChildAt(0);
            measureWidth = childView.getMeasuredWidth() * mChildSize;
            measureHeight = childView.getMeasuredHeight();
            setMeasuredDimension(measureWidth, measureHeight);
        } else if (widthSpaceMode == MeasureSpec.AT_MOST) {
            final View childView = getChildAt(0);
            measureWidth = childView.getMeasuredWidth() * mChildSize;
            setMeasuredDimension(measureWidth, heightSpaceSize);
        } else if (heightSpaceMode == MeasureSpec.AT_MOST) {
            final View childView = getChildAt(0);
            measureHeight = childView.getMeasuredHeight();
            setMeasuredDimension(widthSpaceSize, measureHeight);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = 0;
        int childCount = getChildCount();
        mChildSize = childCount;
        mChildWidth = getChildAt(0).getMeasuredWidth();

       /* if(!mIsInit) {
            childLeft += mChildWidth;
            Log.d(TAG, "is not init");
        }*/

        for(int i = 0; i < childCount; i++) {
            final View childView = getChildAt(i);
            if(childView.getVisibility() != View.GONE) {
                int childWidth = childView.getMeasuredWidth();   //必须用measureWidth,不能用getWidth
                mChildWidth = childWidth;
                childView.layout(childLeft, 0, childLeft + childWidth, childView.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
        setSmoothScroll(mChildWidth, 0, 0);
    }

    public void setSmoothScroll(int dx, int dy, int time) {
        mScroller.startScroll(getScrollX(), 0, dx, 0, time);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if(mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mVelocityX.recycle();
        super.onDetachedFromWindow();
    }
}
