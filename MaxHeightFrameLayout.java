package com.everimaging.fotorlongpicturesplicing.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.everimaging.fotorlongpicturesplicing.util.SplicingImage;

/**
 * create by colin
 * 2021/7/21
 */
public class MaxHeightFrameLayout extends LinearLayout {

    private SplicingItemView mFocusView;

    public MaxHeightFrameLayout(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int childCount = getChildCount();
        if (childCount < 1)
            return;

        int height = 0;
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            measureChildWithMargins(childAt, widthMeasureSpec,
                    0, heightMeasureSpec, 0);
            height += childAt.getMeasuredHeight();
        }
        //todo 根据方向排列

//        View lastChild = getChildAt(childCount - 1);
//        FrameLayout.LayoutParams layoutParams = (LayoutParams) lastChild.getLayoutParams();
//        int h = layoutParams.topMargin + lastChild.getMeasuredHeight();
        setMeasuredDimension(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    public void detachViewFromParent(int index) {
        super.detachViewFromParent(index);
    }

    @Override
    public void detachViewFromParent(View child) {
        super.detachViewFromParent(child);
    }

    @Override
    public void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
        super.attachViewToParent(child, index, params);
    }

    public void setFocusView(int index) {
        SplicingItemView childAt = (SplicingItemView) getChildAt(index);
        childAt.setFocusModel(true);
        mFocusView = childAt;
    }

    public void exitFocus() {
        if (mFocusView != null)
            mFocusView.setFocusModel(false);
        mFocusView = null;
    }

    public void flipHFocusView() {
        if (mFocusView == null) return;
        mFocusView.flipH();
    }

    public void flipVFocusView() {
        if (mFocusView == null) return;
        mFocusView.flipV();
    }

    public void replaceFocusImage(SplicingImage image) {
        if (mFocusView == null) return;
        mFocusView.setImage(image);
    }

    public void deleteFocusView() {
        if (mFocusView == null) return;
        removeView(mFocusView);
        mFocusView = null;
    }
}
