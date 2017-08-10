package com.github.teocci.featureguide.lib;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import com.github.teocci.featureguide.lib.utils.LogHelper;

import java.util.ArrayList;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */
public class FrameLayoutWithHole extends FrameLayout
{
    private static final String TAG = LogHelper.makeLogTag(FrameLayoutWithHole.class);

    private TextPaint textPaint;
    private Activity activity;
    private FeatureGuide.MotionType motionType;
    private Paint eraser;

    private Bitmap eraserBitmap;
    private Canvas eraserCanvas;
    private Paint painter;
    private Paint transparentPainter;
    private View viewHole; // This is the targeted view to be highlighted, where the hole should be placed
    private int holeRadius;
    private int[] holePos;
    private float overlayDensity;
    private Overlay overlay;
    private RectF rectFrame;

    private ArrayList<AnimatorSet> animatorSetArrayList;

    public void setViewHole(View viewHole)
    {
        this.viewHole = viewHole;
        enforceMotionType();
    }

    public void addAnimatorSet(AnimatorSet animatorSet)
    {
        if (animatorSetArrayList == null) {
            animatorSetArrayList = new ArrayList<>();
        }
        animatorSetArrayList.add(animatorSet);
    }

    private void enforceMotionType()
    {
        LogHelper.d(TAG, "enforceMotionType 1");
        if (viewHole != null) {
            LogHelper.d(TAG, "enforceMotionType 2");
            if (motionType != null && motionType == FeatureGuide.MotionType.CLICK_ONLY) {
                LogHelper.d(TAG, "enforceMotionType 3");
                LogHelper.d(TAG, "only Clicking");
                viewHole.setOnTouchListener(new OnTouchListener()
                {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent)
                    {
                        viewHole.getParent().requestDisallowInterceptTouchEvent(true);
                        return false;
                    }
                });
            } else if (motionType != null && motionType == FeatureGuide.MotionType.SWIPE_ONLY) {
                LogHelper.d(TAG, "enforceMotionType 4");
                LogHelper.d(TAG, "only Swiping");
                viewHole.setClickable(false);
            }
        }
    }

    public FrameLayoutWithHole(Activity context, View view)
    {
        this(context, view, FeatureGuide.MotionType.ALLOW_ALL);
    }

    public FrameLayoutWithHole(Activity context, View view, FeatureGuide.MotionType motionType)
    {
        this(context, view, motionType, new Overlay());
    }

    public FrameLayoutWithHole(Activity context, View view, FeatureGuide.MotionType motionType, Overlay overlay)
    {
        super(context);
        activity = context;
        viewHole = view;
        init(null, 0);
        enforceMotionType();
        this.overlay = overlay;

        int[] pos = new int[2];
        viewHole.getLocationOnScreen(pos);
        holePos = pos;

        overlayDensity = context.getResources().getDisplayMetrics().density;
        int padding = (int) (20 * overlayDensity);

        if (viewHole.getHeight() > viewHole.getWidth()) {
            holeRadius = viewHole.getHeight() / 2 + padding;
        } else {
            holeRadius = viewHole.getWidth() / 2 + padding;
        }
        this.motionType = motionType;

        // Init a RectF to be used in OnDraw for a ROUNDED_RECTANGLE Style Overlay
        if (this.overlay != null && this.overlay.overlayStyle == Overlay.Style.ROUNDED_RECTANGLE) {
            int recfFPaddingPx = (int) (this.overlay.paddingDp * overlayDensity);
            rectFrame = new RectF(holePos[0] - recfFPaddingPx + this.overlay.holeOffsetLeft,
                    holePos[1] - recfFPaddingPx + this.overlay.holeOffsetTop,
                    holePos[0] + viewHole.getWidth() + recfFPaddingPx + this.overlay.holeOffsetLeft,
                    holePos[1] + viewHole.getHeight() + recfFPaddingPx + this.overlay.holeOffsetTop);
        }
    }

    private void init(AttributeSet attrs, int defStyle)
    {
        // Load attributes
//        final TypedArray a = getContext().obtainStyledAttributes(
//                attrs, FrameLayoutWithHole, defStyle, 0);
//
//
//        a.recycle();
        setWillNotDraw(false);
        // Set up a default TextPaint object
        textPaint = new TextPaint();
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.LEFT);

        Point size = new Point();
        size.x = activity.getResources().getDisplayMetrics().widthPixels;
        size.y = activity.getResources().getDisplayMetrics().heightPixels;

        eraserBitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
        eraserCanvas = new Canvas(eraserBitmap);

        painter = new Paint();
        painter.setColor(0xcc000000);
        transparentPainter = new Paint();
        transparentPainter.setColor(getResources().getColor(android.R.color.transparent));
        transparentPainter.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        eraser = new Paint();
        eraser.setColor(0xFFFFFFFF);
        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        eraser.setFlags(Paint.ANTI_ALIAS_FLAG);

        LogHelper.d(TAG, "getHeight: " + size.y);
        LogHelper.d(TAG, "getWidth: " + size.x);

    }

    private boolean mCleanUpLock = false;

    protected void cleanUp()
    {
        if (getParent() != null) {
            if (overlay != null && overlay.exitAnimation != null) {
                performOverlayExitAnimation();
            } else {
                ((ViewGroup) this.getParent()).removeView(this);
            }
        }
    }

    private void performOverlayExitAnimation()
    {
        if (!mCleanUpLock) {
            final FrameLayout _pointerToFrameLayout = this;
            mCleanUpLock = true;
            LogHelper.d(TAG, "Overlay exit animation listener is overwritten...");
            overlay.exitAnimation.setAnimationListener(new Animation.AnimationListener()
            {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation)
                {
                    ((ViewGroup) _pointerToFrameLayout.getParent()).removeView(_pointerToFrameLayout);
                }
            });
            this.startAnimation(overlay.exitAnimation);
        }
    }

    /* comment this whole method to cause a memory leak */
    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        /* cleanup reference to prevent memory leak */
        eraserCanvas.setBitmap(null);
        eraserBitmap = null;

        if (animatorSetArrayList != null && !animatorSetArrayList.isEmpty()) {
            for (int i = 0; i < animatorSetArrayList.size(); i++) {
                animatorSetArrayList.get(i).end();
                animatorSetArrayList.get(i).removeAllListeners();
            }
        }
    }

    /**
     * Show an event in the LogCat view, for debugging
     */
    private static void dumpEvent(MotionEvent event)
    {
        String[] names = {"DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
                "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?"};
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);
        if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(
                    action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }
        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }
        sb.append("]");
        LogHelper.d(TAG, sb.toString());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        //first check if the location button should handle the touch event
//        dumpEvent(ev);
//        int action = MotionEventCompat.getActionMasked(ev);
        if (viewHole != null) {

//            LogHelper.d(TAG, "[dispatchTouchEvent] viewHole.getHeight(): "+viewHole.getHeight());
//            LogHelper.d(TAG, "[dispatchTouchEvent] viewHole.getWidth(): "+viewHole.getWidth());
//
//            LogHelper.d(TAG, "[dispatchTouchEvent] Touch X(): "+ev.getRawX());
//            LogHelper.d(TAG, "[dispatchTouchEvent] Touch Y(): "+ev.getRawY());

//            LogHelper.d(TAG, "[dispatchTouchEvent] X of image: "+pos[0]);
//            LogHelper.d(TAG, "[dispatchTouchEvent] Y of image: "+pos[1]);

//            LogHelper.d(TAG, "[dispatchTouchEvent] X lower bound: "+ pos[0]);
//            LogHelper.d(TAG, "[dispatchTouchEvent] X higher bound: "+(pos[0] +viewHole.getWidth()));
//
//            LogHelper.d(TAG, "[dispatchTouchEvent] Y lower bound: "+ pos[1]);
//            LogHelper.d(TAG, "[dispatchTouchEvent] Y higher bound: "+(pos[1] +viewHole.getHeight()));

            if (isWithinButton(ev) && overlay != null && overlay.disableClickThroughHole) {
                LogHelper.d(TAG, "block user clicking through hole");
                // block it
                return true;
            } else if (isWithinButton(ev)) {
                // let it pass through
                return false;
            }
        }
        // do nothing, just propagating up to super
        return super.dispatchTouchEvent(ev);
    }

    private boolean isWithinButton(MotionEvent ev)
    {
        int[] pos = new int[2];
        viewHole.getLocationOnScreen(pos);
        return ev.getRawY() >= pos[1] &&
                ev.getRawY() <= (pos[1] + viewHole.getHeight()) &&
                ev.getRawX() >= pos[0] &&
                ev.getRawX() <= (pos[0] + viewHole.getWidth());
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        eraserBitmap.eraseColor(Color.TRANSPARENT);

        if (overlay != null) {
            eraserCanvas.drawColor(overlay.backgroundColor);
            int padding = (int) (overlay.paddingDp * overlayDensity);
            LogHelper.i(TAG, String.format("**********PADDING: %s**********", padding));

            if (overlay.overlayStyle == Overlay.Style.RECTANGLE) {
                eraserCanvas.drawRect(
                        holePos[0] - padding + overlay.holeOffsetLeft,
                        holePos[1] - padding + overlay.holeOffsetTop,
                        holePos[0] + viewHole.getWidth() + padding + overlay.holeOffsetLeft,
                        holePos[1] + viewHole.getHeight() + padding + overlay.holeOffsetTop, eraser);
            } else if (overlay.overlayStyle == Overlay.Style.NO_HOLE) {
                eraserCanvas.drawCircle(
                        holePos[0] + viewHole.getWidth() / 2 + overlay.holeOffsetLeft,
                        holePos[1] + viewHole.getHeight() / 2 + overlay.holeOffsetTop,
                        0, eraser);
            } else if (overlay.overlayStyle == Overlay.Style.ROUNDED_RECTANGLE) {
                int roundedCornerRadiusPx;
                if (overlay.roundedCornerRadiusDp != 0) {
                    roundedCornerRadiusPx = (int) (overlay.roundedCornerRadiusDp * overlayDensity);
                } else {
                    roundedCornerRadiusPx = (int) (10 * overlayDensity);
                }
                eraserCanvas.drawRoundRect(rectFrame, roundedCornerRadiusPx, roundedCornerRadiusPx, eraser);
            } else {
                int holeRadius = overlay.holeRadius != Overlay.NOT_SET ? overlay.holeRadius : this.holeRadius;
                eraserCanvas.drawCircle(
                        holePos[0] + viewHole.getWidth() / 2 + overlay.holeOffsetLeft,
                        holePos[1] + viewHole.getHeight() / 2 + overlay.holeOffsetTop,
                        holeRadius, eraser);
            }
        }
        canvas.drawBitmap(eraserBitmap, 0, 0, null);

    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        if (overlay != null && overlay.enterAnimation != null) {
            this.startAnimation(overlay.enterAnimation);
        }
    }

    /**
     * Convenient method to obtain screen width in pixel
     *
     * @param activity
     * @return screen width in pixel
     */
    public int getScreenWidth(Activity activity)
    {
        return activity.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * Convenient method to obtain screen height in pixel
     *
     * @param activity
     * @return screen width in pixel
     */
    public int getScreenHeight(Activity activity)
    {
        return activity.getResources().getDisplayMetrics().heightPixels;
    }
}
