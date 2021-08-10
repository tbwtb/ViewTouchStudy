package com.colin.longpicture;

import android.graphics.Matrix;
import android.view.MotionEvent;

/**
 * create by colin
 * 2021/8/10
 */
public class CommonTouchHelper {

    private final Matrix textureMatrix;
    private final boolean needRotate;
    public static final int TYPE_TRANSLATION = 1;
    public static final int TYPE_SCALE = 2;
    public static final int TYPE_NONE = 0;
    private float mMaxZoom = 5f;
    private float mMinZoom = 0.3f;
    private int moveType;

    //记录上次手指的点
    private float actionX, actionY;
    private int actionPointerId = MotionEvent.INVALID_POINTER_ID; //单指移动的手指id

    private float spacing; //两指的距离
    private int scalePointerId = MotionEvent.INVALID_POINTER_ID; //第二根手指的id，负责移动和缩放

    private float degree; //两指的角度


    public CommonTouchHelper(Matrix textureMatrix, boolean needRotate) {
        this.textureMatrix = textureMatrix;
        this.needRotate = needRotate;
    }

    public boolean onTouchEvent(MotionEvent event, Runnable invalidate, Runnable touchUp) {
        boolean consume = false;
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setActionPointerId(pointerId);
                setMoveType(TYPE_TRANSLATION, event, true);
                consume = true;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (getScalePointerId() == MotionEvent.INVALID_POINTER_ID) {
                    setScalePointerId(pointerId);
                    setMoveType(TYPE_SCALE, event, true);
                    consume = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (getMoveType() == TYPE_TRANSLATION
                        && getActionPointerId() != MotionEvent.INVALID_POINTER_ID) {
                    //移动
                    calculationTranslation(event);
                    if (invalidate != null)
                        invalidate.run();
                    consume = true;
                } else if (getMoveType() == TYPE_SCALE
                        && getScalePointerId() != MotionEvent.INVALID_POINTER_ID) {
                    //缩放或者旋转
                    calculationScaleAndRotation(event,
                            event.getX(), event.getY());
                    if (invalidate != null)
                        invalidate.run();
                    consume = true;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (pointerId == getActionPointerId()) {
                    setActionPointerId(MotionEvent.INVALID_POINTER_ID);
                } else if (pointerId == getScalePointerId()) {
                    setScalePointerId(MotionEvent.INVALID_POINTER_ID);
                }

                if (getActionPointerId() != MotionEvent.INVALID_POINTER_ID) {
                    if (getScalePointerId() != MotionEvent.INVALID_POINTER_ID) {
                        setMoveType(TYPE_SCALE, event, false);
                    } else {
                        setMoveType(TYPE_TRANSLATION, event, true);
                    }
                } else {
                    setMoveType(TYPE_NONE, event, false);
                }
                consume = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setActionPointerId(MotionEvent.INVALID_POINTER_ID);
                setScalePointerId(MotionEvent.INVALID_POINTER_ID);
                setMoveType(TYPE_NONE, event, false);
                if (touchUp != null)
                    touchUp.run();
                consume = true;
                break;
        }
        return consume;
    }

    /**
     * 设置手势类别
     *
     * @param moveType 手势类别
     * @param event    触摸事件
     * @param needSet  需要设置对应值？
     */
    public void setMoveType(int moveType, MotionEvent event, boolean needSet) {
        this.moveType = moveType;
        if (moveType == TYPE_TRANSLATION && needSet) {
            setActionX(event.getX());
            setActionY(event.getY());
        } else if (moveType == TYPE_SCALE && needSet) {
            setSpacing(event);
            setDegree(event);
        }
    }

    /**
     * 计算平移
     *
     * @param event 触摸事件
     */
    public void calculationTranslation(MotionEvent event) {
        if (event.getPointerId(event.getActionIndex()) == actionPointerId) {
            //安全区的判断
            float dx = event.getX() - actionX;
            float dy = event.getY() - actionY;
            textureMatrix.postTranslate(dx, dy);
            setActionX(event.getX());
            setActionY(event.getY());
        }
    }

    /**
     * 计算旋转和缩放
     * <p>
     * 关于旋转中心的问题，旋转中心使用双指距离的中间点体验最好，但由于后续需要保存需要，
     * 不好记录，故使用画布中心点。
     *
     * @param event   触摸事件
     * @param centerX 旋转，缩放中心x坐标
     * @param centerY 旋转，缩放中心y坐标
     */
    public void calculationScaleAndRotation(MotionEvent event, float centerX, float centerY) {
        try {
            //计算缩放
            float newSpacing = getSpacing(event);
            float newScale = newSpacing / spacing;
            float oldScale = getScale();
            float scale = oldScale * newScale;
            if (scale >= mMinZoom && scale <= mMaxZoom) {
                textureMatrix.postScale(newScale, newScale, centerX, centerY);
                spacing = newSpacing;
            }

            //计算角度
            if (needRotate) {
                float newDegree = getDegree(event);
                float rotate = newDegree - degree;
                textureMatrix.postRotate(rotate, centerX, centerY);
                degree = newDegree;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setActionX(float actionX) {
        this.actionX = actionX;
    }

    public void setActionY(float actionY) {
        this.actionY = actionY;
    }

    public int getMoveType() {
        return moveType;
    }

    public int getActionPointerId() {
        return actionPointerId;
    }

    public void setActionPointerId(int actionPointerId) {
        this.actionPointerId = actionPointerId;
    }

    public int getScalePointerId() {
        return scalePointerId;
    }

    public void setScalePointerId(int scalePointerId) {
        this.scalePointerId = scalePointerId;
    }

    public void setSpacing(MotionEvent event) {
        spacing = getSpacing(event);
    }

    public void setDegree(MotionEvent event) {
        degree = getDegree(event);
    }

    // 触碰两点间距离
    private float getSpacing(MotionEvent event) {
        //通过勾股定理得到两点间的距离
        float x = event.getX(findActionIndex(event))
                - event.getX(findSCaleIndex(event));
        float y = event.getY(findActionIndex(event))
                - event.getY(findSCaleIndex(event));
        return (float) Math.sqrt(x * x + y * y);
    }

    // 取旋转角度
    private float getDegree(MotionEvent event) {
        //得到两个手指间的旋转角度
        double delta_x = event.getX(findActionIndex(event)) - event.getX(findSCaleIndex(event));
        double delta_y = event.getY(findActionIndex(event)) - event.getY(findSCaleIndex(event));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private int findActionIndex(MotionEvent event) {
        return event.findPointerIndex(actionPointerId);
    }

    private int findSCaleIndex(MotionEvent event) {
        return event.findPointerIndex(scalePointerId);
    }

    private float getScale() {
        float[] values = new float[9];
        textureMatrix.getValues(values);
        float scalex = values[Matrix.MSCALE_X];
        float skewy = values[Matrix.MSKEW_Y];
        return (float) Math.sqrt(scalex * scalex + skewy * skewy);
    }

    public void setMaxZoom(float mMaxZoom) {
        this.mMaxZoom = mMaxZoom;
    }

    public void setMinZoom(float mMinZoom) {
        this.mMinZoom = mMinZoom;
    }
}
