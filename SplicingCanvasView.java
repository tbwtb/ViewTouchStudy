package com.everimaging.fotorlongpicturesplicing.view;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import com.everimaging.fotorlongpicturesplicing.util.SplicingAnimateOBJ;
import com.everimaging.fotorlongpicturesplicing.util.SplicingCallback;
import com.everimaging.fotorlongpicturesplicing.util.SplicingImage;
import com.everimaging.fotorlongpicturesplicing.util.SplicingTypeEvaluator;
import com.everimaging.fotorlongpicturesplicing.util.SplicingUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * create by colin
 * 2021/7/21
 */
public class SplicingCanvasView extends FrameLayout
        implements ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener {

    public static final int VERTICAL = LinearLayout.VERTICAL;
    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;

    private GestureDetectorCompat gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private boolean isDragMode = false;
    private boolean isEditMode = false;
    private boolean isScalingMode = false;
    private int mOrientation;

    private MaxHeightFrameLayout mContentView;

    private SplicingCallback mCallback;
    private Matrix mTransformMatrix;

    private final TimeInterpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private final List<RectF> mImageRectList = new ArrayList<>();
    private final List<SplicingImage> mImageList = new ArrayList<>();
    private final List<SplicingItemView> mItemViews = new ArrayList<>();

    private int mCurrentEditIndex = -1;
    private DragShadowBuilder dragShadowBuilder;

    public SplicingCanvasView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SplicingCanvasView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mTransformMatrix = new Matrix();
        gestureDetector = new GestureDetectorCompat(context, this);
        gestureDetector.setIsLongpressEnabled(true);
        scaleGestureDetector = new ScaleGestureDetector(context, this);
        mContentView = new MaxHeightFrameLayout(context);
        addView(mContentView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.setMatrix(mTransformMatrix);
        super.dispatchDraw(canvas);
        canvas.restore();
        if (isDragMode && dragShadowBuilder != null) {
            dragShadowBuilder.onDrawShadow(canvas);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() > 1) {
            isScalingMode = true;
        }
        return !isDragMode;
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //????????????????????????
        if (isEditMode) {
            return true;
        }

        //????????????????????????,???????????????
        if (isDragMode) {
//            SplicingItemView dragShadowBuilderView = (SplicingItemView) dragShadowBuilder.getView();
//            switch (event.getAction()) {
//                case MotionEvent.ACTION_MOVE:
//                    //todo ??????????????????????????????view?????????????????????view????????????
//                    int toIndex = getIndex(event.getX(), event.getY());
//                    int fromIndex = mItemViews.indexOf(dragShadowBuilderView);
//                    LogUtils.i(fromIndex, toIndex);
//                    if (toIndex == fromIndex)
//                        return true;
////                    View toView = mContentView.getChildAt(toIndex);
////                    AppUtils.swapViewGroupChildren(mContentView, toView, dragShadowBuilderView);
//                    RectF tmp = new RectF(mImageRectList.get(toIndex));
//                    mImageRectList.get(toIndex).set(mImageRectList.get(fromIndex));
//                    mImageRectList.get(fromIndex).set(tmp);
//                    Collections.swap(mImageList, toIndex, fromIndex);
//                    Collections.swap(mItemViews, toIndex, fromIndex);
//                    break;
//                case MotionEvent.ACTION_UP:
//                    isDragMode = false;
//                    //todo ????????????view?????????
//                    dragShadowBuilderView.dragEnd();
//                    break;
//            }
            return false;
        }

        //?????????????????????????????????
        boolean handle = scaleGestureDetector.onTouchEvent(event);
        boolean inProgress = scaleGestureDetector.isInProgress();
        if (!inProgress) {
            //????????????
            handle = gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP && !isEditMode) {
                animateToProperPosition();
            }
        }
        return handle;
    }



    /*
     * ?????????????????????
     */

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        isScalingMode = true;
        //todo ??????????????????
        mTransformMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(),
                detector.getFocusX(), detector.getFocusY());
        invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        isScalingMode = true;
        mTransformMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(),
                detector.getFocusX(), detector.getFocusY());
        invalidate();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        isScalingMode = false;
    }


    /*
     * ????????????????????????????????????
     */
    @Override
    public boolean onDown(MotionEvent e) {
        return !isScalingMode;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // ??????????????????: ?????????????????????????????????????????????
        int index = getIndex(e.getX(), e.getY());
        boolean contains = index > -1;
        isEditMode = contains;
        if (mCallback != null && contains) {
            mCurrentEditIndex = index;
            mCallback.inEditModel();
            //?????????????????????????????????1f
            float currentScale = SplicingUtils.getScale(mTransformMatrix);
            float[] indexDxDy = getIndexDxDy(index);
            mContentView.setFocusView(index);
            if (currentScale != 1f) {
                mTransformMatrix.reset();
                mTransformMatrix.postTranslate(indexDxDy[0], -indexDxDy[1]);
                invalidate();
            } else {
                float[] translations = SplicingUtils.getTranslations(mTransformMatrix);
                animateToEdit(currentScale,
                        translations[0], translations[1], indexDxDy[0], -indexDxDy[1]);
            }
        }
        return contains;
    }


    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float scale = SplicingUtils.getScale(mTransformMatrix);
        if (scale != 1) {
            //??????contentView?????????????????????????????????????????????????????????????????????
            float contentWidth = mContentView.getWidth() * scale;
            float contentHeight = mContentView.getHeight() * scale;
            float dx = contentWidth <= getWidth() ? 0 : -distanceX;
            float dy = contentHeight <= getHeight() ? 0 : -distanceY;
            mTransformMatrix.postTranslate(dx, dy);
        } else {
            //todo ????????????
            if (mOrientation == VERTICAL) {
                //??????????????????
                mTransformMatrix.postTranslate(0, -distanceY);
            } else {
                //??????????????????
                mTransformMatrix.postTranslate(-distanceX, 0);
            }
        }
        invalidate();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        int index = getIndex(e.getX(), e.getY());
        if (index > -1) {
            isDragMode = true;
            float[] indexDxDy = getIndexDxDy(index);
            mTransformMatrix.reset();
            mTransformMatrix.postTranslate(indexDxDy[0], -indexDxDy[1]);
            invalidate();

//            float scale = SplicingUtils.getScale(mTransformMatrix);
//            if (scale > 1) {
//
//            }
//            post(() -> {
//                SplicingItemView childAt = (SplicingItemView) mContentView.getChildAt(index);
//                dragShadowBuilder = new DragShadowBuilder(childAt);
//                childAt.dragStart();
//            });
        }
    }

    private int getIndex(float x, float y) {
        int size = mImageRectList.size();
        RectF f = new RectF();
        int index = -1;
        for (int i = 0; i < size; i++) {
            RectF rectF = mImageRectList.get(i);
            mTransformMatrix.mapRect(f, rectF);
            if (f.contains(x, y)) {
                index = i;
                break;
            }
        }
        return index;
    }

    private float[] getIndexDxDy(int index) {
        RectF rectF = mImageRectList.get(index);
        //todo ????????????,???????????????dx???dy
        float dy = rectF.top + rectF.height() / 2f - getHeight() / 2f;
        float dx = 0;
        return new float[]{dx, dy};
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    /*
     * ?????????????????????
     */


    /**
     * ??????????????????????????????
     */
    private void animateToProperPosition() {
        if (mTransformMatrix.isIdentity())
            return;
        //????????????view???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        float scale = SplicingUtils.getScale(mTransformMatrix);
        float contentWidth = mContentView.getWidth() * scale;
        float contentHeight = mContentView.getHeight() * scale;
        float dx, dy;
        float[] translations = SplicingUtils.getTranslations(mTransformMatrix);
        if (contentWidth < getWidth()) {
            dx = translations[0] - (getWidth() / 2f - contentWidth / 2f);
        } else {
            //????????????,???????????????????????????
            dx = getEdgeTranslation(translations[0], contentWidth, getWidth());
        }
        if (contentHeight < getHeight()) {
            dy = translations[1] - (getHeight() / 2f - contentHeight / 2f);
        } else {
            //????????????
            dy = getEdgeTranslation(translations[1], contentHeight, getHeight());
        }
        post(new AnimateTranslateRunnable(-dx, -dy));
    }

    /**
     * ????????????????????????
     *
     * @param translation ???????????????
     * @param contentSize ????????????
     * @param canvasSize  ????????????
     * @return ????????????????????????
     */
    private float getEdgeTranslation(float translation, float contentSize, float canvasSize) {
        float d = 0;
        if (translation > 0) {
            d = translation;
        } else if (Math.abs(translation) + canvasSize >= contentSize) {
            d = contentSize - canvasSize + translation;
        }
        return d;
    }


    /**
     * ???????????????????????????
     *
     * @param startScale ???????????????
     * @param startDx    ??????dx
     * @param startDy    ??????dy
     * @param endDx      ??????dx
     * @param endDy      ??????dy
     */
    private void animateToEdit(float startScale, float startDx, float startDy, float endDx, float endDy) {
        ValueAnimator animator = ValueAnimator.ofObject(
                new SplicingTypeEvaluator(),
                new SplicingAnimateOBJ(startScale, startDx, startDy),
                new SplicingAnimateOBJ(1f, endDx, endDy));
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            SplicingAnimateOBJ obj = (SplicingAnimateOBJ) animation.getAnimatedValue();
            mTransformMatrix.setScale(obj.scale, obj.scale);
            mTransformMatrix.setTranslate(obj.dx, obj.dy);
            invalidate();
        });
        animator.start();
    }

    private void innerSetImageList(List<SplicingImage> imageList) {
        if (imageList == null)
            return;
        mImageRectList.clear();
        mContentView.removeAllViews();
        mImageList.clear();
        mItemViews.clear();
        mImageList.addAll(imageList);
        //todo ????????????????????????
        int top = 0;
        for (int i = 0; i < imageList.size(); i++) {
            SplicingItemView itemView = new SplicingItemView(getContext());
            SplicingImage splicingImage = imageList.get(i);
            itemView.setImage(splicingImage);
            int measuredWidth = getMeasuredWidth();
            int height = (int) (measuredWidth / (splicingImage.width * 1f / splicingImage.height));
            LinearLayout.LayoutParams itemParams
                    = new LinearLayout.LayoutParams(measuredWidth, height);
            itemView.setLayoutParams(itemParams);
            RectF f = new RectF(0, top, measuredWidth, top + height);
            top += height;
            mImageRectList.add(f);
            mContentView.addView(itemView);
            mItemViews.add(itemView);
        }
    }

    /**
     * ??????????????? runnable
     */
    private class AnimateTranslateRunnable implements Runnable {

        private final float targetDx;
        private final float targetDy;
        private float totalDx, totalDy;

        public AnimateTranslateRunnable(float dx, float dy) {
            this.targetDx = dx;
            this.targetDy = dy;
        }

        @Override
        public void run() {
            float vx = targetDx / 200f * 16;
            float vy = targetDy / 200f * 16;
            if (Math.abs(totalDx + vx) >= Math.abs(targetDx))
                vx = 0;
            if (Math.abs(totalDy + vy) >= Math.abs(targetDy))
                vy = 0;
            mTransformMatrix.postTranslate(vx, vy);
            invalidate();
            totalDx += vx;
            totalDy += vy;
            if (Math.abs(totalDx) < Math.abs(targetDx)
                    || Math.abs(targetDx) < Math.abs(targetDy)) {
                postDelayed(this, 16);
            }
        }
    }

    private class AnimatedZoomRunnable implements Runnable {

        private final float mFocalX, mFocalY;
        private final long mStartTime;
        private final float mZoomStart, mZoomEnd;
        private Runnable endAction;

        public AnimatedZoomRunnable(final float currentZoom, final float targetZoom,
                                    final float focalX, final float focalY) {
            mFocalX = focalX;
            mFocalY = focalY;
            mStartTime = System.currentTimeMillis();
            mZoomStart = currentZoom;
            mZoomEnd = targetZoom;
        }

        public AnimatedZoomRunnable(final float currentZoom, final float targetZoom,
                                    final float focalX, final float focalY, Runnable endAction) {
            this(currentZoom, targetZoom, focalX, focalY);
            this.endAction = endAction;
        }

        @Override
        public void run() {
            float t = interpolate();
            float scale = mZoomStart + t * (mZoomEnd - mZoomStart);
            float deltaScale = scale / SplicingUtils.getScale(mTransformMatrix);
            mTransformMatrix.postScale(deltaScale, deltaScale, mFocalX, mFocalY);
            invalidate();
            if (t < 1f) {
                postDelayed(this, 1000 / 60);
            } else {
                if (endAction != null)
                    endAction.run();
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / 200;
            t = Math.min(1f, t);
            t = mInterpolator.getInterpolation(t);
            return t;
        }
    }



    /*
     * ?????????????????????
     */

    public void setOrientation(int orientation) {
        mOrientation = orientation;
        mContentView.setOrientation(orientation);
    }


    public void exitEditMode() {
        isEditMode = false;
        mContentView.exitFocus();
        animateToProperPosition();
    }

    public void flipH() {
        if (!isEditMode) return;
        mContentView.flipHFocusView();
    }

    public void flipV() {
        if (!isEditMode) return;
        mContentView.flipVFocusView();
    }

    public void delete() {
        if (!isEditMode) return;
        if (mImageList.size() < 3) {
            //?????????????????????
            return;
        }
        if (mCurrentEditIndex > -1) {
            mImageRectList.remove(mCurrentEditIndex);
            mImageList.remove(mCurrentEditIndex);
            mContentView.deleteFocusView();
            mContentView.post(() -> innerSetImageList(new ArrayList<>(mImageList)));
            mContentView.post(this::exitEditMode);
            if (mCallback != null)
                mCallback.autoExitEditModel();
        }
    }

    public void replace(SplicingImage image) {
        if (!isEditMode) return;
        mContentView.replaceFocusImage(image);
    }

    public void setImageList(List<SplicingImage> imageList) {
        post(() -> innerSetImageList(imageList));
    }

    public void setCallback(SplicingCallback callback) {
        this.mCallback = callback;
    }
}
