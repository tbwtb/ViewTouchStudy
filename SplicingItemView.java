package com.everimaging.fotorlongpicturesplicing.view;

import android.content.Context;
import android.graphics.Matrix;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.everimaging.fotorlongpicturesplicing.R;
import com.everimaging.fotorlongpicturesplicing.util.SplicingImage;
import com.everimaging.fotorsdk.widget.FotorImageView;
import com.everimaging.fotorsdk.widget.entity.Rotate3dAnimation;

/**
 * create by colin
 * 2021/7/21
 */
public class SplicingItemView extends FrameLayout {

    //    private SplicingImage image;
    private final FotorImageView mImageView;
    private final View mStrokeView;
    private boolean isProgress = false, mIsFlipH = false, mIsFlipV;
    private final Matrix mFlipMatrix;

    public SplicingItemView(@NonNull Context context) {
        super(context);
        setBackgroundResource(R.color.fotor_window_background_dark);
        mImageView = new FotorImageView(context);
        mStrokeView = new View(context);
        mStrokeView.setBackgroundResource(R.drawable.bg_focus_lps_item);
        addView(mImageView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(mStrokeView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mStrokeView.setVisibility(GONE);
        mFlipMatrix = new Matrix();
    }

    public void setImage(SplicingImage image) {
//        this.image = image;
        mFlipMatrix.reset();
        Matrix finalMatrix = mImageView.getImageViewMatrix();
        finalMatrix.reset();
        mImageView.setImageMatrix(finalMatrix);
        Glide.with(this).load(image.uri)
                .into(mImageView);
    }

    public void setFocusModel(boolean focus) {
        mStrokeView.setVisibility(focus ? VISIBLE : GONE);
    }

    public void flipH() {
        if (isProgress)
            return;
        doFlip(false);
    }

    public void flipV() {
        if (isProgress)
            return;
        doFlip(true);
    }

    private void doFlip(boolean flipV) {
        mFlipMatrix.reset();
        if (flipV) {
            mIsFlipV = !mIsFlipV;
        } else {
            mIsFlipH = !mIsFlipH;
        }
        float pivotX = mImageView.getWidth() / 2.f;
        float pivotY = mImageView.getHeight() / 2.f;

        if (mIsFlipV)
            mFlipMatrix.postScale(1.0f, -1.0f, pivotX, pivotY);
        else
            mFlipMatrix.postScale(1.0f, 1.0f, pivotX, pivotY);

        if (mIsFlipH)
            mFlipMatrix.postScale(-1.0f, 1.0f, pivotX, pivotY);
        else {
            mFlipMatrix.postScale(1.0f, 1.0f, pivotX, pivotY);
        }
        Matrix finalMatrix = mImageView.getImageViewMatrix();
        finalMatrix.preConcat(mFlipMatrix);
        mImageView.setImageMatrix(finalMatrix);
        Rotate3dAnimation animRo = new Rotate3dAnimation(getContext(),
                180, 0, pivotX,
                pivotY, 0.f, false, flipV);
        animRo.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isProgress = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isProgress = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animRo.setDuration(350);
        mImageView.startAnimation(animRo);
    }

    public void dragStart() {
        mImageView.setVisibility(INVISIBLE);
    }

    public void dragEnd() {
        mImageView.setVisibility(VISIBLE);
    }
}
